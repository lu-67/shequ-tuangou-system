package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.Constants;
import com.community.groupbuy.common.PaySignUtils;
import com.community.groupbuy.common.ResultCode;
import com.community.groupbuy.dto.PaymentCallbackDTO;
import com.community.groupbuy.entity.GroupOrder;
import com.community.groupbuy.entity.Payment;
import com.community.groupbuy.mapper.ActivityMapper;
import com.community.groupbuy.mapper.OrderMapper;
import com.community.groupbuy.mapper.PaymentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M4 支付与成团 Service 单元测试（Mockito，不启动容器、不依赖数据库）
 *
 * 覆盖：创建支付成功、重复支付幂等、订单不存在、越权、订单非待支付、
 *       回调正常入账、验签失败、金额不一致、回调重放（幂等）、并发回调未抢到锁、
 *       已退款支付再次回调、退款成功、重复退款幂等、未支付退款、退款 CAS 失败不回补库存。
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String SECRET = "test-pay-secret";

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "paySecret", SECRET);
    }

    // ---------- 发起支付 ----------

    @Test
    void createPayment_success() {
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(paymentMapper.selectByPayNo("PAY100")).thenReturn(null);

        Payment payment = paymentService.createPayment(100L, "WECHAT", 5L, Constants.ROLE_RESIDENT);

        assertThat(payment.getPayNo()).isEqualTo("PAY100");
        assertThat(payment.getStatus()).isEqualTo(Constants.PAY_PENDING);
        assertThat(payment.getAmount()).isEqualByComparingTo("39.90");
        verify(paymentMapper).insert(any(Payment.class));
    }

    @Test
    void createPayment_repeatReturnsSamePayment() {
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        Payment exist = pendingPayment("PAY100", 100L);
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(paymentMapper.selectByPayNo("PAY100")).thenReturn(exist);

        Payment payment = paymentService.createPayment(100L, null, 5L, Constants.ROLE_RESIDENT);

        assertThat(payment).isSameAs(exist);
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void createPayment_orderNotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> paymentService.createPayment(999L, null, 5L, Constants.ROLE_RESIDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ORDER_NOT_FOUND.getCode());
    }

    @Test
    void createPayment_otherUserOrderForbidden() {
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        when(orderMapper.selectById(100L)).thenReturn(order);

        assertThatThrownBy(() -> paymentService.createPayment(100L, null, 6L, Constants.ROLE_RESIDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void createPayment_orderAlreadyPaid() {
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        order.setStatus(Constants.ORDER_PAID);
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(paymentMapper.selectByPayNo("PAY100")).thenReturn(null);

        assertThatThrownBy(() -> paymentService.createPayment(100L, null, 5L, Constants.ROLE_RESIDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode());
    }

    // ---------- 支付回调 ----------

    @Test
    void handleCallback_success() {
        Payment pending = pendingPayment("PAY100", 100L);
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        when(paymentMapper.selectByPayNo("PAY100")).thenReturn(pending);
        when(paymentMapper.markCallbackSuccess("PAY100", Constants.PAY_PENDING, Constants.PAY_SUCCESS)).thenReturn(1);
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(orderMapper.updateStatusCAS(100L, Constants.ORDER_UNPAID, Constants.ORDER_PAID)).thenReturn(1);

        boolean ok = paymentService.handleCallback(callback("PAY100", "1", "39.90", true));

        assertThat(ok).isTrue();
        verify(orderMapper).updateStatusCAS(100L, Constants.ORDER_UNPAID, Constants.ORDER_PAID);
        verify(activityService).markSuccessIfSoldOut(200L);
    }

    @Test
    void handle_callback_signError() {
        assertThatThrownBy(() -> paymentService.handleCallback(callback("PAY100", "1", "39.90", false)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.PAY_SIGN_ERROR.getCode());
    }

    @Test
    void handle_callback_amountMismatch() {
        Payment pending = pendingPayment("PAY100", 100L);
        when(paymentMapper.selectByPayNo("PAY100")).thenReturn(pending);

        assertThatThrownBy(() -> paymentService.handleCallback(callback("PAY100", "1", "1.00", true)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.PAY_AMOUNT_MISMATCH.getCode());
    }

    @Test
    void handle_callback_replayAlreadySuccess() {
        Payment paid = pendingPayment("PAY100", 100L);
        paid.setStatus(Constants.PAY_SUCCESS);
        when(paymentMapper.selectByPayNo("PAY100")).thenReturn(paid);

        boolean ok = paymentService.handleCallback(callback("PAY100", "1", "39.90", true));

        assertThat(ok).isTrue();
        verify(paymentMapper, never()).markCallbackSuccess(anyString(), anyString(), anyString());
        verify(orderMapper, never()).updateStatusCAS(anyLong(), anyString(), anyString());
    }

    @Test
    void handle_callback_concurrentLostCasNotDoubleBooking() {
        Payment pending = pendingPayment("PAY100", 100L);
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        when(paymentMapper.selectByPayNo("PAY100")).thenReturn(pending);
        // 支付单 CAS 未抢到锁（另一个线程先改成了 SUCCESS）
        when(paymentMapper.markCallbackSuccess("PAY100", Constants.PAY_PENDING, Constants.PAY_SUCCESS)).thenReturn(0);

        boolean ok = paymentService.handleCallback(callback("PAY100", "1", "39.90", true));

        assertThat(ok).isTrue();
        verify(orderMapper, never()).updateStatusCAS(anyLong(), anyString(), anyString());
    }

    @Test
    void handle_callback_orderAlreadyPaidIdempotent() {
        Payment pending = pendingPayment("PAY100", 100L);
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        order.setStatus(Constants.ORDER_PAID);
        when(paymentMapper.selectByPayNo("PAY100")).thenReturn(pending);
        when(paymentMapper.markCallbackSuccess("PAY100", Constants.PAY_PENDING, Constants.PAY_SUCCESS)).thenReturn(1);
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(orderMapper.updateStatusCAS(100L, Constants.ORDER_UNPAID, Constants.ORDER_PAID)).thenReturn(0);

        boolean ok = paymentService.handleCallback(callback("PAY100", "1", "39.90", true));

        assertThat(ok).isTrue();
        verify(activityService, never()).markSuccessIfSoldOut(anyLong());
    }

    @Test
    void handle_callback_cancelledOrderRejected() {
        Payment pending = pendingPayment("PAY100", 100L);
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        order.setStatus(Constants.ORDER_CANCELLED);
        when(paymentMapper.selectByPayNo("PAY100")).thenReturn(pending);
        when(paymentMapper.markCallbackSuccess("PAY100", Constants.PAY_PENDING, Constants.PAY_SUCCESS)).thenReturn(1);
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(orderMapper.updateStatusCAS(100L, Constants.ORDER_UNPAID, Constants.ORDER_PAID)).thenReturn(0);

        assertThatThrownBy(() -> paymentService.handleCallback(callback("PAY100", "1", "39.90", true)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode());
    }

    @Test
    void handle_callback_refundedPaymentRejected() {
        Payment refunded = pendingPayment("PAY100", 100L);
        refunded.setStatus(Constants.PAY_REFUNDED);
        when(paymentMapper.selectByPayNo("PAY100")).thenReturn(refunded);

        assertThatThrownBy(() -> paymentService.handleCallback(callback("PAY100", "1", "39.90", true)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode());
    }

    // ---------- 退款 ----------

    @Test
    void refund_success() {
        Payment paid = pendingPayment("PAY100", 100L);
        paid.setId(7L);
        paid.setStatus(Constants.PAY_SUCCESS);
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        order.setStatus(Constants.ORDER_PAID);
        when(paymentMapper.selectById(7L)).thenReturn(paid);
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(orderMapper.updateStatusCAS(100L, Constants.ORDER_PAID, Constants.ORDER_CANCELLED)).thenReturn(1);

        paymentService.refund(7L, 5L, Constants.ROLE_RESIDENT);

        verify(activityMapper).incrementStock(200L, 2);
        verify(paymentMapper).updateStatusCAS("PAY100", Constants.PAY_SUCCESS, Constants.PAY_REFUNDED);
    }

    @Test
    void refund_repeatIsIdempotent() {
        Payment refunded = pendingPayment("PAY100", 100L);
        refunded.setId(7L);
        refunded.setStatus(Constants.PAY_REFUNDED);
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        order.setStatus(Constants.ORDER_CANCELLED);
        when(paymentMapper.selectById(7L)).thenReturn(refunded);
        when(orderMapper.selectById(100L)).thenReturn(order);

        paymentService.refund(7L, 5L, Constants.ROLE_RESIDENT);

        // 重复退款：不再改订单状态，更不会重复回补库存
        verify(orderMapper, never()).updateStatusCAS(anyLong(), anyString(), anyString());
        verify(activityMapper, never()).incrementStock(anyLong(), anyInt());
    }

    @Test
    void refund_unpaidOrderRejected() {
        Payment pending = pendingPayment("PAY100", 100L);
        pending.setId(7L);
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        when(paymentMapper.selectById(7L)).thenReturn(pending);
        when(orderMapper.selectById(100L)).thenReturn(order);

        assertThatThrownBy(() -> paymentService.refund(7L, 5L, Constants.ROLE_RESIDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode());
    }

    @Test
    void refund_casFailNoStockRollback() {
        Payment paid = pendingPayment("PAY100", 100L);
        paid.setId(7L);
        paid.setStatus(Constants.PAY_SUCCESS);
        GroupOrder order = unpaidOrder(100L, 5L, "1");
        order.setStatus(Constants.ORDER_PAID);
        when(paymentMapper.selectById(7L)).thenReturn(paid);
        when(orderMapper.selectById(100L)).thenReturn(order);
        // 并发下订单已被其他操作改掉，CAS 失败
        when(orderMapper.updateStatusCAS(100L, Constants.ORDER_PAID, Constants.ORDER_CANCELLED)).thenReturn(0);

        assertThatThrownBy(() -> paymentService.refund(7L, 5L, Constants.ROLE_RESIDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ORDER_STATUS_ERROR.getCode());

        // 关键：CAS 失败绝不允许回补库存，否则重复退款会把库存刷上去
        verify(activityMapper, never()).incrementStock(anyLong(), anyInt());
    }

    @Test
    void refund_paymentNotFound() {
        when(paymentMapper.selectById(7L)).thenReturn(null);

        assertThatThrownBy(() -> paymentService.refund(7L, 5L, Constants.ROLE_RESIDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    // ---------- 辅助方法 ----------

    private GroupOrder unpaidOrder(Long id, Long userId, String orderNo) {
        GroupOrder order = new GroupOrder();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setActivityId(200L);
        order.setQuantity(2);
        order.setAmount(new BigDecimal("39.90"));
        order.setStatus(Constants.ORDER_UNPAID);
        return order;
    }

    private Payment pendingPayment(String payNo, Long orderId) {
        Payment payment = new Payment();
        payment.setPayNo(payNo);
        payment.setOrderId(orderId);
        payment.setOrderNo("1");
        payment.setAmount(new BigDecimal("39.90"));
        payment.setChannel("WECHAT");
        payment.setStatus(Constants.PAY_PENDING);
        return payment;
    }

    /** sign=true 时按服务端同样规则生成签名；sign=false 时给一个错误签名 */
    private PaymentCallbackDTO callback(String payNo, String orderNo, String amount, boolean valid) {
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setPayNo(payNo);
        dto.setOrderNo(orderNo);
        dto.setAmount(new BigDecimal(amount));
        dto.setChannel("WECHAT");
        dto.setSign(valid ? PaySignUtils.sign(SECRET, payNo, orderNo, new BigDecimal(amount).toPlainString()) : "bad-sign");
        return dto;
    }
}
