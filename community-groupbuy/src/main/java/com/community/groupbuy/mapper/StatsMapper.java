package com.community.groupbuy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {

    // 统计大盘数据：总订单数、总销售额
    @Select("SELECT " +
            "(SELECT COUNT(*) FROM group_order) AS totalOrders, " +
            "(SELECT IFNULL(SUM(amount), 0) FROM group_order WHERE status = 'PAID') AS totalSales")
    Map<String, Object> getOverviewStats();

    // 直接查订单表（不要连表！连表一旦匹配不上就会变成空）
    // 使用 AS 别名，保证返回的 Key 是驼峰命名
    @Select("SELECT " +
            "order_no AS orderNo, " +
            "user_id AS username, " +
            "amount AS amount, " +
            "status AS status, " +
            "create_time AS createTime " +
            "FROM group_order ORDER BY id DESC LIMIT 500")
    List<Map<String, Object>> getOrderListForExport();
}