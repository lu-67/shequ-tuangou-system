package com.community.groupbuy.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 参团订单
 */
@Data
public class GroupOrder {
    private Long id;
    private String orderNo;
    private Long activityId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal amount;
    private String status;
    private String verificationCode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
}
