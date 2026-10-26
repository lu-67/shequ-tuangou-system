package com.community.groupbuy.mapper;

import com.community.groupbuy.dto.OrderQueryDTO;
import com.community.groupbuy.entity.GroupOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
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

    /** M4：按订单号查询（回调对账用） */
    @Select("SELECT * FROM group_order WHERE order_no = #{orderNo}")
    GroupOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * M4：订单状态流转（CAS），带上原状态作为条件，返回值 0 表示状态已被其他操作变更。
     * 支付入账时自动写 pay_time，保证并发回调只有一个能改成功。
     */
    @Update("UPDATE group_order SET status = #{to}, "
            + "pay_time = CASE WHEN #{to} = 'PAID' THEN NOW() ELSE pay_time END "
            + "WHERE id = #{id} AND status = #{from}")
    int updateStatusCAS(@Param("id") Long id, @Param("from") String from, @Param("to") String to);

    /**
     * M4：活动「已参团份数」= 已支付订单的购买数量汇总。
     * 不新增字段（约定：不改表结构），支付成功一笔该值自动 +1 份。
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM group_order WHERE activity_id = #{activityId} AND status = 'PAID'")
    long sumPaidQuantity(@Param("activityId") Long activityId);

    /** M8：超时仍未支付的订单（下单时间早于截止时间），供定时任务取消并回补库存 */
    @Select("SELECT * FROM group_order WHERE status = 'UNPAID' AND create_time < #{deadline} "
            + "ORDER BY id ASC LIMIT 500")
    List<GroupOrder> selectTimeoutUnpaid(@Param("deadline") LocalDateTime deadline);

    /**
     * M5（C 同学追加）：按核销码查订单，核销接口入口。
     * verification_code 有唯一索引，最多命中一条；查不到说明核销码无效。
     */
    @Select("SELECT * FROM group_order WHERE verification_code = #{code}")
    GroupOrder selectByVerificationCode(@Param("code") String code);
}
