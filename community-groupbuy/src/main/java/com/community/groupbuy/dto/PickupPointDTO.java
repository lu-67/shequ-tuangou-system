package com.community.groupbuy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 自提点新增 / 修改入参（M5）
 */
@Data
public class PickupPointDTO {

    @NotBlank(message = "自提点名称不能为空")
    @Size(max = 100, message = "自提点名称长度不能超过100")
    private String name;

    @NotBlank(message = "自提点地址不能为空")
    @Size(max = 200, message = "地址长度不能超过200")
    private String address;

    @Size(max = 50, message = "负责人长度不能超过50")
    private String manager;

    @Size(max = 20, message = "联系电话长度不能超过20")
    private String phone;
}
