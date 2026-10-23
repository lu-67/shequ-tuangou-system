package com.community.groupbuy.dto;

import lombok.Data;

/**
 * 订单查询条件（用于手写动态 SQL，供后台统计导出）
 */
@Data
public class OrderQueryDTO {
    private String status;
    private Long activityId;
    private Long userId;
    private String orderNo;
}
