package com.community.groupbuy.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 核销记录（M5）
 *
 * 对应表 pickup_verification，uk_order_id / uk_code 两个唯一索引是幂等的第二道防线。
 */
@Data
public class PickupVerification {
    private Long id;
    /** 关联订单 */
    private Long orderId;
    /** 核销码 */
    private String verificationCode;
    /** 核销自提点 */
    private Long pickupPointId;
    /** PENDING / DONE / INVALID */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verifyTime;
}
