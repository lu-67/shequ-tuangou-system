package com.community.groupbuy.service;

import com.community.groupbuy.common.Constants;
import com.community.groupbuy.entity.GroupActivity;
import com.community.groupbuy.entity.GroupOrder;
import com.community.groupbuy.mapper.ActivityMapper;
import com.community.groupbuy.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * M8 定时任务：到期活动成团/失败判定 + 超时未支付订单自动取消。
 *
 * 设计要点：
 * 1. 全部更新走 CAS（WHERE status = 原状态），任务重复执行不会产生副作用，天然幂等；
 * 2. 成团口径统一为「已参团份数」= 已支付订单数量汇总（见 ActivityService.markSuccessIfReached）：
 *    达到 total_stock 判定 SUCCESS；到期时已参团份数 &gt; 0 判定 SUCCESS，= 0 判定 FAILED；
 * 3. 超时未支付订单：UNPAID → CANCELLED，抢到锁才回补活动库存，避免重复回补；
 * 4. 每个任务都打印处理数量，便于演示与排查（SLF4J）。
 *
 * 与 M4 的关系：支付成功时会实时判一次成团，本任务每分钟再扫一次，作为事件丢失 / 直接改库的兜底。
 * 两条路径共用同一个 CAS 判定，重复执行无副作用。
 *
 * 演示提示：任务每分钟执行一次，首次延迟 5 分钟，避免应用刚启动就跑批处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", matchIfMissing = true)
public class SchedulerService {

    private final ActivityMapper activityMapper;
    private final OrderMapper orderMapper;
    private final ActivityService activityService;

    /** 超时未支付订单的取消阈值（分钟），来自配置 app.pay.timeout-minutes */
    @Value("${app.pay.timeout-minutes:15}")
    private int payTimeoutMinutes;

    /**
     * 到期活动判定：已有人参团（已参团份数 &gt; 0）→ SUCCESS，一份未成 → FAILED。
     *
     * @return 本次成功判定的活动数量
     */
    @Scheduled(initialDelay = 300000, fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public int settleExpiredActivities() {
        List<GroupActivity> expired = activityMapper.selectExpired(LocalDateTime.now());
        if (expired.isEmpty()) {
            return 0;
        }
        int settled = 0;
        for (GroupActivity activity : expired) {
            long joined = orderMapper.sumPaidQuantity(activity.getId());
            String target = joined > 0 ? Constants.ACTIVITY_SUCCESS : Constants.ACTIVITY_FAILED;
            int rows = activityMapper.updateStatusCAS(activity.getId(), Constants.ACTIVITY_ONGOING, target);
            if (rows == 1) {
                settled++;
                log.info("活动到期判定: activityId={}, 已参团={} -> {}", activity.getId(), joined, target);
            }
        }
        log.info("到期活动判定完成: 到期={}, 本次判定={}", expired.size(), settled);
        return settled;
    }

    /**
     * 达到 total_stock 的活动判定为 SUCCESS（成团）。
     * 先用一条 SQL 批量筛出候选，再逐个走 ActivityService 的统一 CAS 判定，避免规则写两份。
     *
     * @return 本次判定为成团的活动数量
     */
    @Scheduled(initialDelay = 300000, fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public int settleReachedActivities() {
        List<GroupActivity> reached = activityMapper.selectReachedPaidTotal();
        if (reached.isEmpty()) {
            return 0;
        }
        int settled = 0;
        for (GroupActivity activity : reached) {
            if (activityService.markSuccessIfReached(activity.getId())) {
                settled++;
            }
        }
        log.info("达标活动判定完成: 候选={}, 本次成团={}", reached.size(), settled);
        return settled;
    }

    /**
     * 超时未支付订单：自动取消并回补活动库存。
     * 先 CAS 抢订单状态，只有抢到（影响行数 = 1）才回补库存，保证重复执行不会虚增库存。
     *
     * @return 本次取消的订单数量
     */
    @Scheduled(initialDelay = 300000, fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public int cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(payTimeoutMinutes);
        List<GroupOrder> orders = orderMapper.selectTimeoutUnpaid(deadline);
        if (orders.isEmpty()) {
            return 0;
        }
        int cancelled = 0;
        for (GroupOrder order : orders) {
            int rows = orderMapper.updateStatusCAS(order.getId(), Constants.ORDER_UNPAID, Constants.ORDER_CANCELLED);
            if (rows == 0) {
                continue;
            }
            activityMapper.incrementStock(order.getActivityId(), order.getQuantity());
            cancelled++;
            log.info("超时未支付订单已取消: orderId={}, 库存回补={}", order.getId(), order.getQuantity());
        }
        log.info("超时订单清理完成: 超时={}, 本次取消={}, 截止时间={}", orders.size(), cancelled, deadline);
        return cancelled;
    }
}
