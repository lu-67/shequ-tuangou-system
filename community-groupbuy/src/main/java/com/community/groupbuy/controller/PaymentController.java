package com.community.groupbuy.controller;

import com.community.groupbuy.common.Result;
import com.community.groupbuy.common.SecurityUtils;
import com.community.groupbuy.dto.CreatePaymentDTO;
import com.community.groupbuy.dto.PaymentCallbackDTO;
import com.community.groupbuy.dto.PaymentQueryDTO;
import com.community.groupbuy.entity.Payment;
import com.community.groupbuy.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * M4 支付与成团（支付幂等 / 回调防重入账 / 退款回补库存）
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** 发起支付（幂等：同一订单重复提交返回同一笔支付单） */
    @PostMapping
    public Result<Payment> create(@Valid @RequestBody CreatePaymentDTO dto) {
        Long userId = SecurityUtils.getUserId();
        String role = SecurityUtils.getRole();
        return Result.success("支付单已创建", paymentService.createPayment(dto.getOrderId(), dto.getChannel(), userId, role));
    }

    /** 支付回调（验签 + 幂等，重放不重复入账） */
    @PostMapping("/callback")
    public Result<Boolean> callback(@Valid @RequestBody PaymentCallbackDTO dto) {
        return Result.success("回调处理成功", paymentService.handleCallback(dto));
    }

    /** 退款：订单置为 CANCELLED 并回补活动库存 */
    @PostMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        String role = SecurityUtils.getRole();
        paymentService.refund(id, userId, role);
        return Result.success("退款成功");
    }

    /** 支付单详情 */
    @GetMapping("/{id}")
    public Result<Payment> get(@PathVariable Long id) {
        return Result.success(paymentService.getPayment(id));
    }

    /** 支付记录列表（动态条件 + 分页） */
    @GetMapping
    public Result<List<Payment>> list(PaymentQueryDTO query) {
        return Result.success(paymentService.list(query));
    }

    /**
     * 演示用：生成回调签名，方便在 Knife4j 里自测 /api/payments/callback
     * 字段顺序 payNo | orderNo | amount，生产环境应移除该接口
     */
    @GetMapping("/sign-demo")
    public Result<String> signDemo(String payNo, String orderNo, String amount) {
        return Result.success("演示签名", paymentService.demoSign(payNo, orderNo, amount));
    }
}
