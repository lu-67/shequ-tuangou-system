package com.community.groupbuy.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自提点（M5 自提点与核销）
 */
@Data
public class PickupPoint {
    private Long id;
    /** 自提点名称 */
    private String name;
    /** 地址 */
    private String address;
    /** 负责人 */
    private String manager;
    /** 联系电话 */
    private String phone;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
