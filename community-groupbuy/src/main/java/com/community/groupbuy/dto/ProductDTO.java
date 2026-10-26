package com.community.groupbuy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品新增 / 修改入参（M1）
 *
 * 字段级校验用 jakarta.validation，跨字段规则（团购价不得高于原价）在 ProductService 中校验。
 */
@Data
public class ProductDTO {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过100")
    private String name;

    @Size(max = 50, message = "分类长度不能超过50")
    private String category;

    @Size(max = 50, message = "规格长度不能超过50")
    private String spec;

    @Size(max = 10, message = "单位长度不能超过10")
    private String unit;

    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.00", message = "原价不能为负数")
    private BigDecimal price;

    @NotNull(message = "团购价不能为空")
    @DecimalMin(value = "0.00", message = "团购价不能为负数")
    private BigDecimal groupPrice;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Size(max = 255, message = "图片地址长度不能超过255")
    private String imageUrl;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    /** 1 上架 / 0 下架，不传默认上架 */
    private Integer status;
}
