package com.community.groupbuy.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 团购活动（状态机: PENDING -> ONGOING -> SUCCESS / FAILED）
 */
@Data
public class GroupActivity {
    private Long id;
    private Long productId;
    private Long creatorId;
    private String title;
    private BigDecimal originPrice;
    private BigDecimal groupPrice;
    private Integer totalStock;
    private Integer stock;
    private Integer perLimit;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
