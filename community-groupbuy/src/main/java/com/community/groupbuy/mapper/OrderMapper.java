package com.community.groupbuy.mapper;

import com.community.groupbuy.dto.OrderQueryDTO;
import com.community.groupbuy.entity.GroupOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 参团订单 Mapper（当前仅保留 M3 参团下单 / 我的订单 依赖的方法）
 *
 * 说明：selectByOrderNo / updateStatusByExpected / payOrder 属于
 * M4 支付成团（B 同学）与 M6 统计导出（C/D 同学）负责，由对应同学按需补充。
 */
@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO group_order(order_no, activity_id, user_id, product_id, quantity, amount, status, verification_code, create_time) "
            + "VALUES(#{orderNo}, #{activityId}, #{userId}, #{productId}, #{quantity}, #{amount}, #{status}, #{verificationCode}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GroupOrder order);

    @Select("SELECT * FROM group_order WHERE id = #{id}")
    GroupOrder selectById(@Param("id") Long id);

    @Update("UPDATE group_order SET verification_code = #{code} WHERE id = #{id}")
    int updateVerificationCode(@Param("id") Long id, @Param("code") String code);

    /** 手写动态 SQL：按条件查询（状态/活动/用户/订单号），见 OrderMapper.xml */
    List<GroupOrder> selectByCondition(OrderQueryDTO query);
}
