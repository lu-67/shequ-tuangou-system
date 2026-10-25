package com.community.groupbuy.service;

import com.community.groupbuy.common.Constants;
import com.community.groupbuy.entity.GroupActivity;
import com.community.groupbuy.entity.GroupOrder;
import com.community.groupbuy.mapper.ActivityMapper;
import com.community.groupbuy.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M8 定时任务 Service 单元测试（Mockito，不启动容器、不依赖数据库）
 *
 * 覆盖：到期已成团（有已支付订单）→ SUCCESS、到期零成交 → FAILED、无到期活动、CAS 冲突跳过、
 *       达到 total_stock 成团、无候选活动、已被 M4 实时判过则跳过、超时订单取消并回补库存、
 *       CAS 失败不回补库存、无超时订单。
 * 幂等验证：CAS 影响行数为 0 时既不改状态也不回补库存，任务重复执行无副作用。
 */
@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private SchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(schedulerService, "payTimeoutMinutes", 15);
    }

    /** 到期且有已支付订单（有人真的参团了）→ SUCCESS */
    @Test
    void settleExpired_hasPaidOrder_becomesSuccess() {
        when(activityMapper.selectExpired(any(LocalDateTime.class))).thenReturn(List.of(activity(1L, 0, 100)));
        when(orderMapper.sumPaidQuantity(1L)).thenReturn(30L);
        when(activityMapper.updateStatusCAS(1L, Constants.ACTIVITY_ONGOING, Constants.ACTIVITY_SUCCESS)).thenReturn(1);

        int settled = schedulerService.settleExpiredActivities();

        assertThat(settled).isEqualTo(1);
        verify(activityMapper).updateStatusCAS(1L, Constants.ACTIVITY_ONGOING, Constants.ACTIVITY_SUCCESS);
    }

    /** 到期但一份都没付过钱 → FAILED（哪怕库存被锁过，也不算成团） */
    @Test
    void settleExpired_noPaidOrder_becomesFailed() {
        when(activityMapper.selectExpired(any(LocalDateTime.class))).thenReturn(List.of(activity(2L, 30, 100)));
        when(orderMapper.sumPaidQuantity(2L)).thenReturn(0L);
        when(activityMapper.updateStatusCAS(2L, Constants.ACTIVITY_ONGOING, Constants.ACTIVITY_FAILED)).thenReturn(1);

        int settled = schedulerService.settleExpiredActivities();

        assertThat(settled).isEqualTo(1);
        verify(activityMapper).updateStatusCAS(2L, Constants.ACTIVITY_ONGOING, Constants.ACTIVITY_FAILED);
    }

    @Test
    void settleExpired_noExpiredActivity() {
        when(activityMapper.selectExpired(any(LocalDateTime.class))).thenReturn(List.of());

        assertThat(schedulerService.settleExpiredActivities()).isZero();
        verify(activityMapper, never()).updateStatusCAS(anyLong(), anyString(), anyString());
    }

    /** CAS 冲突（已被其他流程改过状态）：跳过，不算入本次判定，下一轮自然重试 */
    @Test
    void settleExpired_casConflictSkipped() {
        when(activityMapper.selectExpired(any(LocalDateTime.class))).thenReturn(List.of(activity(3L, 0, 100)));
        when(orderMapper.sumPaidQuantity(3L)).thenReturn(100L);
        when(activityMapper.updateStatusCAS(3L, Constants.ACTIVITY_ONGOING, Constants.ACTIVITY_SUCCESS)).thenReturn(0);

        assertThat(schedulerService.settleExpiredActivities()).isZero();
    }

    /** 达到 total_stock：交由 ActivityService 的统一成团判定处理 */
    @Test
    void settleReached_becomesSuccess() {
        when(activityMapper.selectReachedPaidTotal()).thenReturn(List.of(activity(4L, 0, 200)));
        when(activityService.markSuccessIfReached(4L)).thenReturn(true);

        assertThat(schedulerService.settleReachedActivities()).isEqualTo(1);
        verify(activityService).markSuccessIfReached(4L);
    }

    @Test
    void settleReached_noCandidate() {
        when(activityMapper.selectReachedPaidTotal()).thenReturn(List.of());

        assertThat(schedulerService.settleReachedActivities()).isZero();
        verify(activityService, never()).markSuccessIfReached(anyLong());
    }

    /** 已被 M4 实时判成团（CAS 未抢到）：本次不计入，重复执行无副作用 */
    @Test
    void settleReached_alreadySettled_skipped() {
        when(activityMapper.selectReachedPaidTotal()).thenReturn(List.of(activity(5L, 0, 200)));
        when(activityService.markSuccessIfReached(5L)).thenReturn(false);

        assertThat(schedulerService.settleReachedActivities()).isZero();
    }

    /** 超时未支付：取消订单 + 回补库存 */
    @Test
    void cancelTimeoutOrders_cancelAndRestoreStock() {
        when(orderMapper.selectTimeoutUnpaid(any(LocalDateTime.class))).thenReturn(List.of(unpaidOrder(10L)));
        when(orderMapper.updateStatusCAS(10L, Constants.ORDER_UNPAID, Constants.ORDER_CANCELLED)).thenReturn(1);

        int cancelled = schedulerService.cancelTimeoutOrders();

        assertThat(cancelled).isEqualTo(1);
        verify(orderMapper).updateStatusCAS(10L, Constants.ORDER_UNPAID, Constants.ORDER_CANCELLED);
        verify(activityMapper).incrementStock(200L, 2);
    }

    /** CAS 失败（订单已被支付/取消）：绝不回补库存，否则库存会被刷高 */
    @Test
    void cancelTimeoutOrders_casFailNoStockRestore() {
        when(orderMapper.selectTimeoutUnpaid(any(LocalDateTime.class))).thenReturn(List.of(unpaidOrder(11L)));
        when(orderMapper.updateStatusCAS(11L, Constants.ORDER_UNPAID, Constants.ORDER_CANCELLED)).thenReturn(0);

        assertThat(schedulerService.cancelTimeoutOrders()).isZero();
        verify(activityMapper, never()).incrementStock(anyLong(), eq(2));
    }

    @Test
    void cancelTimeoutOrders_noTimeoutOrder() {
        when(orderMapper.selectTimeoutUnpaid(any(LocalDateTime.class))).thenReturn(List.of());

        assertThat(schedulerService.cancelTimeoutOrders()).isZero();
        verify(activityMapper, never()).incrementStock(anyLong(), any());
    }

    private GroupActivity activity(Long id, Integer stock, Integer totalStock) {
        GroupActivity activity = new GroupActivity();
        activity.setId(id);
        activity.setStock(stock);
        activity.setTotalStock(totalStock);
        activity.setStatus(Constants.ACTIVITY_ONGOING);
        return activity;
    }

    private GroupOrder unpaidOrder(Long id) {
        GroupOrder order = new GroupOrder();
        order.setId(id);
        order.setActivityId(200L);
        order.setQuantity(2);
        order.setStatus(Constants.ORDER_UNPAID);
        return order;
    }
}
