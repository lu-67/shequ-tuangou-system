package com.community.groupbuy.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页结果封装（手写分页，项目未引入 PageHelper）
 */
@Data
public class PageResult<T> {

    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> list;

    public static <T> PageResult<T> of(long total, int pageNum, int pageSize, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setList(list == null ? new ArrayList<>() : list);
        return result;
    }

    /** 总页数 */
    public long getPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (total + pageSize - 1) / pageSize;
    }
}
