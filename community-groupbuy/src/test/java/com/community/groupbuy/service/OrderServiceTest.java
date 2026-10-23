package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.dto.CreateOrderDTO;
import com.community.groupbuy.entity.GroupActivity;
import com.community.groupbuy.entity.GroupOrder;
import com.community.groupbuy.mapper.ActivityMapper;
import com.community.groupbuy.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M3 参团下单 Service 单元测试（Mockito，不启动 Spring 容器，不依赖数据库）
 *
 * 覆盖：正常下单（Redis 未预热走 DB CAS）、活动不存在、活动非进行中、
 *       超过限购、CAS 失败库存不足、限流拦截、订单不存在、我的订单倒序。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private OrderService orderService;

    /** 构造一个进行中活动（库存 1000，每人限购 5 件，拼团价 12.90） */
    private GroupActivity ongoingActivity(Long id, int stock, int perLimit) {
        GroupActivity a = new GroupActivity();
        a.setId(id);
        a.setProductId(2L);
        a.setTitle("测试拼团活动");
        a.setOriginPrice(new BigDecimal("19.90"));
        a.setGroupPrice(new BigDecimal("12.90"));
        a.setTotalStock(1000);
        a.setStock(stock);
        a.setPerLimit(perLimit);
        a.setStatus("ONGOING");
        return a;
    }

    private CreateOrderDTO dto(Long activityId, int quantity) {
        CreateOrderDTO d = new CreateOrderDTO();
        d.setActivityId(activityId);
        d.setQuantity(quantity);
        return d;
    }

    @Test
    void createOrder_success_deductStockAndInsertOrder() {
        when(activityMapper.selectById(1L)).thenReturn(ongoingActivity(1L, 1000, 5));
        when(rateLimitService.tryAcquire(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(activityMapper.deductStockCAS(1L, 2)).thenReturn(1);
        // 模拟数据库自增主键回填
        doAnswer(inv -> {
            GroupOrder o = inv.getArgument(0);
            o.setId(321L);
            return 1;
        }).when(orderMapper).insert(any(GroupOrder.class));

        Long orderId = orderService.createOrder(5L, dto(1L, 2));

        assertThat(orderId).isEqualTo(321L);

        // 校验落库订单的字段：金额 = 拼团价 × 数量，状态为待支付
        ArgumentCaptor<GroupOrder> captor = ArgumentCaptor.forClass(GroupOrder.class);
        verify(orderMapper).insert(captor.capture());
        GroupOrder saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(5L);
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(saved.getAmount()).isEqualByComparingTo("25.80");
        assertThat(saved.getStatus()).isEqualTo("UNPAID");

        // 下单后生成唯一核销码
        verify(orderMapper).updateVerificationCode(eq(321L), eq("CG00000321"));
    }

    @Test
    void createOrder_activityNotFound_throwBusinessException() {
        when(rateLimitService.tryAcquire(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(activityMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> orderService.createOrder(5L, dto(99L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("团购活动不存在");
    }

    @Test
    void createOrder_activityNotOngoing_throwBusinessException() {
        GroupActivity pending = ongoingActivity(1L, 1000, 5);
        pending.setStatus("PENDING");
        when(rateLimitService.tryAcquire(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(activityMapper.selectById(1L)).thenReturn(pending);

        assertThatThrownBy(() -> orderService.createOrder(5L, dto(1L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("活动未在进行中");
    }

    @Test
    void createOrder_exceedPerLimit_throwBusinessException() {
        when(rateLimitService.tryAcquire(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(activityMapper.selectById(1L)).thenReturn(ongoingActivity(1L, 1000, 5));

        assertThatThrownBy(() -> orderService.createOrder(5L, dto(1L, 10)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超过每人限购数量");
    }

    @Test
    void createOrder_casFail_stockNotEnough_throwAndNoOrderInserted() {
        when(rateLimitService.tryAcquire(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(true);
        when(activityMapper.selectById(1L)).thenReturn(ongoingActivity(1L, 1000, 5));
        // CAS 失败：库存不足（受影响行数 0）
        when(activityMapper.deductStockCAS(1L, 2)).thenReturn(0);

        assertThatThrownBy(() -> orderService.createOrder(5L, dto(1L, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");

        // 库存不足时不得落订单
        verify(orderMapper, never()).insert(any(GroupOrder.class));
    }

    @Test
    void createOrder_rateLimited_throwBeforeAnyQuery() {
        when(rateLimitService.tryAcquire(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> orderService.createOrder(5L, dto(1L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请求过于频繁");

        // 限流拦截发生在最前面，不应查询活动
        verify(activityMapper, never()).selectById(anyLong());
    }

    @Test
    void getOrder_notFound_throwBusinessException() {
        when(orderMapper.selectById(999999L)).thenReturn(null);

        assertThatThrownBy(() -> orderService.getOrder(999999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    void listMyOrders_sortedByIdDesc() {
        GroupOrder o1 = new GroupOrder();
        o1.setId(1L);
        GroupOrder o3 = new GroupOrder();
        o3.setId(3L);
        when(orderMapper.selectByCondition(any())).thenReturn(List.of(o1, o3));

        List<GroupOrder> result = orderService.listMyOrders(5L);

        assertThat(result).extracting(GroupOrder::getId).containsExactly(3L, 1L);
    }
}
