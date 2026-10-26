package com.community.groupbuy.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品（M1 商品管理）
 *
 * 对应表 product，字段与 db/schema.sql 一一对应，不做任何表结构变更。
 */
@Data
public class Product {
    private Long id;
    /** 商品名称 */
    private String name;
    /** 分类：蔬菜/水果/肉禽/水产/蛋奶 */
    private String category;
    /** 规格，如 500g/份 */
    private String spec;
    /** 单位 */
    private String unit;
    /** 原价 */
    private BigDecimal price;
    /** 团购价 */
    private BigDecimal groupPrice;
    /** 平台总库存 */
    private Integer stock;
    /** 图片地址（URL，不做文件上传） */
    private String imageUrl;
    /** 描述 */
    private String description;
    /** 1 上架 / 0 下架 */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
