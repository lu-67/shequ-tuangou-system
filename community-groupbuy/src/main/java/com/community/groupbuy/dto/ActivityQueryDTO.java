package com.community.groupbuy.dto;

import lombok.Data;

/**
 * 团购活动查询条件（M2，手写动态 SQL + 分页）
 */
@Data
public class ActivityQueryDTO {

    /** 活动状态: PENDING/ONGOING/SUCCESS/FAILED */
    private String status;

    private Long productId;

    /** 标题模糊匹配 */
    private String title;

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
