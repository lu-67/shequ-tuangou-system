package com.community.groupbuy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {
    // 动态 SQL 查询大盘数据
    Map<String, Object> getOverviewStats(@Param("status") String status);

    // 动态 SQL 查询订单用于导出
    List<Map<String, Object>> getOrderListForExport(@Param("status") String status);
}