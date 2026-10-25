package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.Constants;
import com.community.groupbuy.common.ResultCode;
import com.community.groupbuy.dto.ActivityQueryDTO;
import com.community.groupbuy.dto.CreateActivityDTO;
import com.community.groupbuy.dto.PageResult;
import com.community.groupbuy.entity.GroupActivity;
import com.community.groupbuy.mapper.ActivityMapper;
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

    /**
     * 创建开团活动（团长 / 管理员）。
     * 新建活动为 PENDING，需调用 updateStatus 流转到 ONGOING 后才可被参团下单。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createActivity(CreateActivityDTO dto, Long operatorId, String operatorRole) {
        assertHeadOrAdmin(operatorRole);
        validateCreateParam(dto);
        GroupActivity activity = buildActivity(dto);
        activityMapper.insert(activity);
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
        log.info("活动状态流转成功: id={}, {} -> {}, operator={}", activityId, current.name(), target.name(), operatorRole);
    }

    public GroupActivity getActivity(Long activityId) {
        return requireActivity(activityId);
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

    /**
     * 售罄即成团：库存售罄时把 ONGOING 活动判定为 SUCCESS。
     * 供 M4 支付成功与 M8 定时任务调用；并发下 CAS 仅一方生效，未抢到的返回 false 不报错。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markSuccessIfSoldOut(Long activityId) {
        GroupActivity activity = activityMapper.selectById(activityId);
        if (activity == null || !Constants.ACTIVITY_ONGOING.equals(activity.getStatus()) || activity.getStock() > 0) {
            return false;
        }
        int rows = activityMapper.updateStatusCAS(activityId, Constants.ACTIVITY_ONGOING, Constants.ACTIVITY_SUCCESS);
        if (rows == 1) {
            log.info("活动已成团（库存售罄）: activityId={}", activityId);
        }
        return rows == 1;
    }

    private GroupActivity requireActivity(Long activityId) {
        GroupActivity activity = activityMapper.selectById(activityId);
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

    private GroupActivity buildActivity(CreateActivityDTO dto) {
        GroupActivity activity = new GroupActivity();
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
