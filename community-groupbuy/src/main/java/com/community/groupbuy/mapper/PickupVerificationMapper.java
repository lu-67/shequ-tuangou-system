package com.community.groupbuy.mapper;

import com.community.groupbuy.entity.PickupVerification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 核销记录 Mapper（M5，C 同学）
 *
 * 幂等三道防线：
 * 1) Service 先判断订单 status 是否已 PICKED；
 * 2) group_order 状态 CAS（UPDATE ... WHERE status='PAID'）——并发下只有一个线程能改成功；
 * 3) 本表的 uk_order_id / uk_code 唯一索引兜底，重复插入直接抛 DuplicateKeyException。
 */
@Mapper
public interface PickupVerificationMapper {

    /** 新增核销记录，主键回填；verify_time 取数据库当前时间 */
    @Insert("INSERT INTO pickup_verification(order_id, verification_code, pickup_point_id, status, verify_time) "
            + "VALUES(#{orderId}, #{verificationCode}, #{pickupPointId}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PickupVerification record);

    /** 按订单查核销记录（幂等校验 / 展示用） */
    @Select("SELECT * FROM pickup_verification WHERE order_id = #{orderId}")
    PickupVerification selectByOrderId(@Param("orderId") Long orderId);
}
