package com.community.groupbuy.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品查询条件（M1，手写动态 SQL + 分页）
 */
@Data
public class ProductQueryDTO {

    /** 商品名称模糊匹配 */
    private String name;

    /** 分类精确匹配 */
    private String category;

    /** 1 上架 / 0 下架，null 表示不限 */
    private Integer status;

    /** 团购价下限 */
    private BigDecimal minPrice;

    /** 团购价上限 */
    private BigDecimal maxPrice;

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 分页偏移量，供 Mapper XML 的 LIMIT 使用 */
    public int getOffset() {
        return (getSafePageNum() - 1) * getSafePageSize();
    }

    /** 每页条数，最大 100，防止大翻页拖垮数据库 */
    public int getSize() {
        return getSafePageSize();
    }

    private int getSafePageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int getSafePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
