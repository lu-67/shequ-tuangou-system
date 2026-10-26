package com.community.groupbuy.mapper;

import com.community.groupbuy.entity.PickupVerification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 核销记录 Mapper（M5，C 同学）
 *
 * 核销语义：data.sql 为已支付订单预置了 PENDING 记录，核销 = 把 PENDING 更新为 DONE（方案 A）。
 * 幂等三道防线：
 * 1) Service 先判断订单 status 是否已 PICKED；
 * 2) group_order 状态 CAS（UPDATE ... WHERE status='PAID'）——并发下只有一个线程能改成功；
 * 3) pickup_verification 的 uk_order_id / uk_code 唯一索引兜底，重复插入直接抛 DuplicateKeyException。
 */
@Mapper
public interface PickupVerificationMapper {

    /**
     * M5（方案 A）：核销 = 更新预置 PENDING 记录。
     * 影响行数 = 1 表示核销成功；= 0 表示记录不存在（走 INSERT 兜底）或已被核销（该记录已非 PENDING）。
     */
    @Update("UPDATE pickup_verification SET status = 'DONE', pickup_point_id = #{pickupPointId}, verify_time = NOW() "
            + "WHERE order_id = #{orderId} AND status = 'PENDING'")
    int updateToDone(@Param("orderId") Long orderId, @Param("pickupPointId") Long pickupPointId);

    /**
     * M5：新增核销记录（仅用于"订单已支付但没有预置 PENDING 记录"的兜底场景，如 M4 真实支付流）。
     * uk_order_id / uk_code 唯一索引兜底，重复插入抛 DuplicateKeyException。
     */
    @Insert("INSERT INTO pickup_verification(order_id, verification_code, pickup_point_id, status, verify_time) "
            + "VALUES(#{orderId}, #{verificationCode}, #{pickupPointId}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PickupVerification record);

    /** 按订单查核销记录（UPDATE 影响 0 行时判断是"已核销"还是"无记录"） */
    @Select("SELECT * FROM pickup_verification WHERE order_id = #{orderId}")
    PickupVerification selectByOrderId(@Param("orderId") Long orderId);
}
