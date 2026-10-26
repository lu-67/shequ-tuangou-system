package com.community.groupbuy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 自提核销入参（M5）：输入核销码 + 自提点
 */
@Data
public class VerifyDTO {

    @NotBlank(message = "核销码不能为空")
    @Size(max = 20, message = "核销码长度不能超过20")
    private String verificationCode;

    @NotNull(message = "自提点不能为空")
    private Long pickupPointId;
}
