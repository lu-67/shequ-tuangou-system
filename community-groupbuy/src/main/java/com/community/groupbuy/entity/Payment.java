package com.community.groupbuy.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录（M4 支付与成团）
 * 状态：PENDING(待支付) / SUCCESS(已支付) / REFUNDED(已退款)
 */
@Data
public class Payment {

    private Long id;
    private String payNo;
    private Long orderId;
    private String orderNo;
    private BigDecimal amount;
    private String channel;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime callbackTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
