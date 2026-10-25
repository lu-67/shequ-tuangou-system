package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.Constants;
import com.community.groupbuy.common.PaySignUtils;
import com.community.groupbuy.common.ResultCode;
import com.community.groupbuy.dto.PaymentCallbackDTO;
import com.community.groupbuy.dto.PaymentQueryDTO;
import com.community.groupbuy.entity.GroupOrder;
import com.community.groupbuy.entity.Payment;
import com.community.groupbuy.mapper.ActivityMapper;
import com.community.groupbuy.mapper.OrderMapper;
import com.community.groupbuy.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * M4 支付与成团
 *
 * 三个关键点（报告「并发 / 幂等 / 事务」章节素材）：
 * 1. 幂等发起支付：支付流水号按订单维度唯一，重复请求返回同一笔支付单；
 * 2. 回调防重入账：支付单与订单均使用 CAS（WHERE status = 原状态）更新，
 *    影响行数为 0 即判定为重放，直接返回成功、不重复入账；
 * 3. 退款先 CAS 改订单状态，只有抢到锁（影响行数 = 1）才回补库存，
 *    避免重复退款把库存无限加回去；整套操作在同一个事务里。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final ActivityMapper activityMapper;
    private final ActivityService activityService;

    /** 回调验签密钥：演示环境写死，生产用环境变量 APP_PAY_SECRET 注入 */
    @Value("${app.pay.secret:community-groupbuy-pay-dev-secret-2026}")
    private String paySecret;

    /**
     * 发起支付（幂等）：同一订单重复提交返回同一笔支付单。
     */
    @Transactional(rollbackFor = Exception.class)
    public Payment createPayment(Long orderId, String channel, Long operatorId, String operatorRole) {
        GroupOrder order = requireOrder(orderId);
        assertOwnership(order, operatorId, operatorRole);

        String payNo = buildPayNo(order.getId());
        Payment exist = paymentMapper.selectByPayNo(payNo);
        if (exist != null) {
            log.info("重复支付请求命中幂等: payNo={}, status={}", payNo, exist.getStatus());
            return exist;
        }
        if (!Constants.ORDER_UNPAID.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不可支付: " + order.getStatus());
        }

        Payment payment = new Payment();
        payment.setPayNo(payNo);
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setAmount(order.getAmount());
        payment.setChannel(channel == null || channel.isEmpty() ? "WECHAT" : channel);
        payment.setStatus(Constants.PAY_PENDING);
        paymentMapper.insert(payment);

        log.info("支付单已创建: payNo={}, orderId={}, amount={}", payNo, order.getId(), order.getAmount());
        return payment;
    }

    /**
     * 支付回调（验签 + 幂等）：已成功过的回调重复到达时直接返回成功，不重复入账。
     *
     * @return true 表示入账成功或命中幂等；异常表示验签失败 / 金额不一致 / 订单状态异常
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handleCallback(PaymentCallbackDTO dto) {
        // 1. 验签：payNo | orderNo | amount + 服务端密钥
        if (!PaySignUtils.verify(paySecret, dto.getSign(), dto.getPayNo(), dto.getOrderNo(),
                dto.getAmount().toPlainString())) {
            log.warn("支付回调验签失败: payNo={}, orderNo={}", dto.getPayNo(), dto.getOrderNo());
            throw new BusinessException(ResultCode.PAY_SIGN_ERROR);
        }

        Payment payment = paymentMapper.selectByPayNo(dto.getPayNo());
        if (payment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付流水不存在: " + dto.getPayNo());
        }
        if (!payment.getOrderNo().equals(dto.getOrderNo())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单号与支付流水不匹配");
        }
        if (payment.getAmount().compareTo(dto.getAmount()) != 0) {
            throw new BusinessException(ResultCode.PAY_AMOUNT_MISMATCH);
        }

        // 2. 幂等：支付单已成功 / 已退款 → 不再入账
        if (Constants.PAY_SUCCESS.equals(payment.getStatus())) {
            log.info("回调重放，已入账过，直接返回成功: payNo={}", payment.getPayNo());
            return true;
        }
        if (Constants.PAY_REFUNDED.equals(payment.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "该笔支付已退款，不可重复入账");
        }

        // 3. 支付单 CAS：只有 PENDING → SUCCESS 抢到锁的一方继续走入账
        int payRows = paymentMapper.markCallbackSuccess(payment.getPayNo(), Constants.PAY_PENDING, Constants.PAY_SUCCESS);
        if (payRows == 0) {
            log.info("并发回调未抢到锁，按幂等返回: payNo={}", payment.getPayNo());
            return true;
        }

        // 4. 订单 CAS：UNPAID → PAID，影响行数 0 说明订单已被支付过（并发重放）
        GroupOrder order = requireOrder(payment.getOrderId());
        int orderRows = orderMapper.updateStatusCAS(order.getId(), Constants.ORDER_UNPAID, Constants.ORDER_PAID);
        if (orderRows == 0) {
            if (Constants.ORDER_PAID.equals(order.getStatus())) {
                log.info("订单已支付，回调按幂等返回: orderId={}", order.getId());
                return true;
            }
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态异常，支付结果不予入账: " + order.getStatus());
        }

        // 5. 成团判定：库存售罄即成团（M2 提供，幂等可重入）
        activityService.markSuccessIfSoldOut(order.getActivityId());

        log.info("支付回调入账成功: payNo={}, orderId={}, amount={}", payment.getPayNo(), order.getId(), order.getAmount());
        return true;
    }

    /**
     * 退款：订单 PAID → CANCELLED（CAS）+ 回补活动库存，全程同一事务。
     * 只有订单状态抢占成功才回补库存，避免重复退款造成库存虚增。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long paymentId, Long operatorId, String operatorRole) {
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付记录不存在");
        }
        GroupOrder order = requireOrder(payment.getOrderId());
        assertOwnership(order, operatorId, operatorRole);

        if (Constants.PAY_REFUNDED.equals(payment.getStatus())) {
            log.info("重复退款请求命中幂等: payNo={}", payment.getPayNo());
            return;
        }
        if (!Constants.PAY_SUCCESS.equals(payment.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "仅支付成功的订单可退款，当前支付状态: " + payment.getStatus());
        }
        if (!Constants.ORDER_PAID.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "仅已支付订单可退款，当前订单状态: " + order.getStatus());
        }

        // 先抢订单状态，抢到才回补库存
        int rows = orderMapper.updateStatusCAS(order.getId(), Constants.ORDER_PAID, Constants.ORDER_CANCELLED);
        if (rows == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态已被变更，退款失败");
        }
        activityMapper.incrementStock(order.getActivityId(), order.getQuantity());
        paymentMapper.updateStatusCAS(payment.getPayNo(), Constants.PAY_SUCCESS, Constants.PAY_REFUNDED);

        log.info("退款完成: payNo={}, orderId={}, 库存回补={}", payment.getPayNo(), order.getId(), order.getQuantity());
    }

    public Payment getPayment(Long id) {
        Payment payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付记录不存在");
        }
        return payment;
    }

    /** 支付记录分页查询（动态条件） */
    public List<Payment> list(PaymentQueryDTO query) {
        return paymentMapper.selectByCondition(query);
    }

    public long count(PaymentQueryDTO query) {
        return paymentMapper.countByCondition(query);
    }

    /**
     * 生成演示用签名（仅供 Knife4j 自测回调接口，生产环境应移除）。
     * 字段顺序必须与 PaySignUtils 一致：payNo | orderNo | amount
     */
    public String demoSign(String payNo, String orderNo, String amount) {
        return PaySignUtils.sign(paySecret, payNo, orderNo, amount);
    }

    /** 支付流水号按订单维度固定，保证重复支付命中同一笔流水 */
    private String buildPayNo(Long orderId) {
        return "PAY" + orderId;
    }

    private GroupOrder requireOrder(Long orderId) {
        GroupOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 管理员可操作任意订单，普通用户只能操作自己的订单 */
    private void assertOwnership(GroupOrder order, Long operatorId, String operatorRole) {
        if (Constants.ROLE_ADMIN.equals(operatorRole)) {
            return;
        }
        if (operatorId == null || !operatorId.equals(order.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人订单");
        }
    }
}
