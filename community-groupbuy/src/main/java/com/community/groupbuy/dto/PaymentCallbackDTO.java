package com.community.groupbuy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付回调入参（M4，模拟第三方支付平台异步通知）
 * 验签字段顺序：payNo | orderNo | amount
 */
@Data
public class PaymentCallbackDTO {

    @NotBlank(message = "支付流水号不能为空")
    private String payNo;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;

    /** 支付渠道 */
    private String channel;

    @NotBlank(message = "签名不能为空")
    private String sign;
}
