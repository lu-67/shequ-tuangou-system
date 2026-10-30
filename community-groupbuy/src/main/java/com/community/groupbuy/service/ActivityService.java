package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.Constants;
import com.community.groupbuy.common.ResultCode;
import com.community.groupbuy.dto.ActivityQueryDTO;
import com.community.groupbuy.dto.CreateActivityDTO;
import com.community.groupbuy.dto.PageResult;
import com.community.groupbuy.entity.GroupActivity;
import com.community.groupbuy.mapper.ActivityMapper;
import com.community.groupbuy.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * M2 开团活动：创建 / 状态机流转 / 分页查询 / 进行中列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityMapper activityMapper;

    private final OrderMapper orderMapper;

    /** M7：库存预热 / 清除（C 同学） */
    private final StockCacheService stockCacheService;

    /** M7：活动详情 Cache-Aside（C 同学） */
    private final CacheService cacheService;

    /**
     * 创建开团活动（团长 / 管理员）。
     * 新建活动为 PENDING，需调用 updateStatus 流转到 ONGOING 后才可被参团下单。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createActivity(CreateActivityDTO dto, Long operatorId, String operatorRole) {
        assertHeadOrAdmin(operatorRole);
        validateCreateParam(dto);
        GroupActivity activity = buildActivity(dto, operatorId);
        activityMapper.insert(activity);
        // M7：初始状态若已是 ONGOING，立即预热库存（当前创建固定 PENDING，此处为保护性判断）
        if (Constants.ACTIVITY_ONGOING.equals(activity.getStatus())) {
            stockCacheService.preheat(activity.getId(), activity.getStock());
        }
        log.info("开团活动创建成功: id={}, title={}, operatorId={}", activity.getId(), activity.getTitle(), operatorId);
        return activity.getId();
    }

    /**
     * 活动状态流转：枚举 + 转移表断言 + DB CAS，非法流转抛 2004。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long activityId, String targetStatus, String operatorRole) {
        assertHeadOrAdmin(operatorRole);
        ActivityStatus target = ActivityStatus.of(targetStatus);
        GroupActivity activity = requireActivity(activityId);
        ActivityStatus current = ActivityStatus.of(activity.getStatus());
        current.assertCanTransferTo(target);

        int rows = activityMapper.updateStatusCAS(activityId, current.name(), target.name());
        if (rows != 1) {
            throw new BusinessException(ResultCode.ACTIVITY_STATUS_ERROR, "活动状态已被其他操作变更，请刷新后重试");
        }
        // M7：进入 ONGOING 预热库存；SUCCESS/FAILED 清除预热；活动详情缓存一律失效
        syncStockCacheOnTransfer(activity, target);
        cacheService.evict(Constants.REDIS_CACHE_ACTIVITY_PREFIX + activityId);
        log.info("活动状态流转成功: id={}, {} -> {}, operator={}", activityId, current.name(), target.name(), operatorRole);
    }

    /**
     * 活动详情（M7 缓存）：Cache-Aside + 分布式锁回源 + 随机 TTL。
     * 库存变化频繁，TTL 取 20 秒；状态流转会主动失效该 key。
     */
    public GroupActivity getActivity(Long activityId) {
        return cacheService.getOrLoad(Constants.REDIS_CACHE_ACTIVITY_PREFIX + activityId, GroupActivity.class,
                Constants.CACHE_ACTIVITY_TTL_SECONDS, () -> requireActivity(activityId));
    }

    /** 动态条件分页查询 */
    public PageResult<GroupActivity> page(ActivityQueryDTO query) {
        long total = activityMapper.countByCondition(query);
        List<GroupActivity> list = total == 0 ? List.of() : activityMapper.selectByCondition(query);
        return PageResult.of(total, query.getPageNum() == null ? 1 : query.getPageNum(),
                query.getPageSize() == null ? 10 : query.getPageSize(), list);
    }

    /** 进行中活动列表（居民端首页） */
    public List<GroupActivity> listOngoing() {
        return activityMapper.selectOngoing();
    }

    /** 团长/管理员：我创建的活动（全部状态），用于前台「我的开团」页面 */
    public List<GroupActivity> listMyActivities(Long userId, String role) {
        assertHeadOrAdmin(role);
        return activityMapper.selectByCreator(userId);
    }

    /**
     * 成团判定（全项目唯一口径）：「已参团份数」达到 total_stock 时，把 ONGOING 活动判定为 SUCCESS。
     *
     * 已参团份数 = 该活动已支付（PAID）订单的数量汇总，不新增字段（约定：不改表结构），
     * 因此支付成功一笔，已参团份数自动 +1 份 —— 即需求里的「支付成功后已参团人数 +1」。
     * 注意：不可用 total_stock - stock 代替，那个口径把未支付订单也算进去了。
     *
     * 供 M4 支付成功（实时判定）与 M8 定时任务（兜底判定）共用；
     * 状态流转走 CAS，并发下只有一方生效，未抢到的一方返回 false，不报错（幂等）。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markSuccessIfReached(Long activityId) {
        GroupActivity activity = activityMapper.selectById(activityId);
        if (activity == null || !Constants.ACTIVITY_ONGOING.equals(activity.getStatus())) {
            return false;
        }
        long joinedQuantity = orderMapper.sumPaidQuantity(activityId);
        if (joinedQuantity < activity.getTotalStock()) {
            log.info("活动暂未成团: activityId={}, 已参团={}/{}",
                    activityId, joinedQuantity, activity.getTotalStock());
            return false;
        }
        int rows = activityMapper.updateStatusCAS(activityId, Constants.ACTIVITY_ONGOING, Constants.ACTIVITY_SUCCESS);
        if (rows == 1) {
            // M7：已成团，清除库存预热与详情缓存（活动不再接受下单）
            stockCacheService.evict(activityId);
            cacheService.evict(Constants.REDIS_CACHE_ACTIVITY_PREFIX + activityId);
            log.info("活动已成团: activityId={}, 已参团={}/{}",
                    activityId, joinedQuantity, activity.getTotalStock());
        }
        return rows == 1;
    }

    /** M7：状态流转后同步库存缓存（ONGOING 预热，SUCCESS/FAILED 清除） */
    private void syncStockCacheOnTransfer(GroupActivity activity, ActivityStatus target) {
        if (target == ActivityStatus.ONGOING) {
            stockCacheService.preheat(activity.getId(), activity.getStock());
        } else if (target == ActivityStatus.SUCCESS || target == ActivityStatus.FAILED) {
            stockCacheService.evict(activity.getId());
        }
    }

    private GroupActivity requireActivity(Long activityId) {        GroupActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.ACTIVITY_NOT_FOUND);
        }
        return activity;
    }

    private void assertHeadOrAdmin(String role) {
        if (!Constants.ROLE_ADMIN.equals(role) && !Constants.ROLE_HEAD.equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅团长或管理员可管理团购活动");
        }
    }

    private void validateCreateParam(CreateActivityDTO dto) {
        if (dto.getTotalStock() == null || dto.getTotalStock() < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动总库存至少为1");
        }
        if (dto.getGroupPrice().compareTo(dto.getOriginPrice()) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "团购价不能高于原价");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动时间不能为空");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "活动结束时间必须晚于开始时间");
        }
        if (dto.getPerLimit() != null && dto.getPerLimit() < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "每人限购至少为1");
        }
    }

    private GroupActivity buildActivity(CreateActivityDTO dto, Long operatorId) {
        GroupActivity activity = new GroupActivity();
        activity.setCreatorId(operatorId);
        activity.setProductId(dto.getProductId());
        activity.setTitle(dto.getTitle());
        activity.setOriginPrice(dto.getOriginPrice());
        activity.setGroupPrice(dto.getGroupPrice());
        activity.setTotalStock(dto.getTotalStock());
        activity.setStock(dto.getTotalStock());
        activity.setPerLimit(dto.getPerLimit() == null ? 5 : dto.getPerLimit());
        activity.setStatus(ActivityStatus.PENDING.name());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        return activity;
    }
}
