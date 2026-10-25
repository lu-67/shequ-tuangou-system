package com.community.groupbuy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 开团活动入参（M2）
 */
@Data
public class CreateActivityDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotBlank(message = "活动标题不能为空")
    private String title;

    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.01", message = "原价必须大于0")
    private BigDecimal originPrice;

    @NotNull(message = "团购价不能为空")
    @DecimalMin(value = "0.01", message = "团购价必须大于0")
    private BigDecimal groupPrice;

    /** 总库存（验收用 1000）；创建后剩余库存 = 总库存 */
    @NotNull(message = "活动总库存不能为空")
    @Min(value = 1, message = "活动总库存至少为1")
    private Integer totalStock;

    /** 每人限购，不传默认 5 */
    @Min(value = 1, message = "每人限购至少为1")
    private Integer perLimit;

    @NotNull(message = "活动开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "活动结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
