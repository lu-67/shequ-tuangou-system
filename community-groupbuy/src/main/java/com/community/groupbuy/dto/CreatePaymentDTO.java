package com.community.groupbuy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发起支付入参（M4）
 */
@Data
public class CreatePaymentDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 支付渠道：WECHAT / ALIPAY，默认 WECHAT */
    private String channel;
}
