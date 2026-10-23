package com.community.groupbuy.mapper;

import com.community.groupbuy.entity.GroupActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 团购活动 Mapper（当前仅保留 M3 参团下单依赖的方法）
 *
 * 说明：insert / updateStatus / selectOngoing / 动态条件查询 属于 M2 开团活动（B 同学）负责，
 * 由 B 同学按其需求在此接口中补充。
 */
@Mapper
public interface ActivityMapper {

    @Select("SELECT * FROM group_activity WHERE id = #{id}")
    GroupActivity selectById(@Param("id") Long id);

    /**
     * 库存扣减（DB CAS，保证不超卖）：
     * 仅当剩余库存 >= 购买量时才扣减，受影响行数=1 表示成功。
     */
    @Update("UPDATE group_activity SET stock = stock - #{qty} WHERE id = #{activityId} AND stock >= #{qty}")
    int deductStockCAS(@Param("activityId") Long activityId, @Param("qty") Integer qty);

    /** 库存回补（退款/取消时使用，M4 支付成团（B 同学）会用到） */
    @Update("UPDATE group_activity SET stock = stock + #{qty} WHERE id = #{activityId}")
    int incrementStock(@Param("activityId") Long activityId, @Param("qty") Integer qty);
}
