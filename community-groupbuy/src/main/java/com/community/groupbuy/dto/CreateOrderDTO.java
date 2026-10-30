package com.community.groupbuy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 参团下单入参
 */
@Data
public class CreateOrderDTO {
    @NotNull(message = "活动ID不能为空")
    private Long activityId;
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity;
    @NotNull(message = "请选择自提点")
    private Long pickupPointId;
}
