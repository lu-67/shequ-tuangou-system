package com.community.groupbuy.dto;

import lombok.Data;

/**
 * 支付记录查询条件（M4，手写动态 SQL + 分页）
 */
@Data
public class PaymentQueryDTO {

    private String payNo;

    private String orderNo;

    /** 支付状态: PENDING/SUCCESS/REFUNDED */
    private String status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 分页偏移量，供 Mapper XML 的 LIMIT 使用 */
    public int getOffset() {
        return (getSafePageNum() - 1) * getSafePageSize();
    }

    /** 每页条数，最大 100 */
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
