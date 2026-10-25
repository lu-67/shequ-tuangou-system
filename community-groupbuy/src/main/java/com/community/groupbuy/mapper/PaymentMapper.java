package com.community.groupbuy.mapper;

import com.community.groupbuy.dto.PaymentQueryDTO;
import com.community.groupbuy.entity.Payment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 支付记录 Mapper（M4 支付与成团）
 */
@Mapper
public interface PaymentMapper {

    @Select("SELECT * FROM payment WHERE id = #{id}")
    Payment selectById(@Param("id") Long id);

    @Select("SELECT * FROM payment WHERE pay_no = #{payNo}")
    Payment selectByPayNo(@Param("payNo") String payNo);

    /** 同一订单可能有多笔流水时取最新一笔（当前业务一单一笔） */
    @Select("SELECT * FROM payment WHERE order_id = #{orderId} ORDER BY id DESC LIMIT 1")
    Payment selectLatestByOrderId(@Param("orderId") Long orderId);

    @Insert("INSERT INTO payment(pay_no, order_id, order_no, amount, channel, status) "
            + "VALUES(#{payNo}, #{orderId}, #{orderNo}, #{amount}, #{channel}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Payment payment);

    /**
     * 回调入账（CAS）：仅当支付单仍是 PENDING 时才置为 SUCCESS 并写入回调时间。
     * 返回值 0 表示该笔回调已被处理过（重放），调用方按幂等直接返回成功。
     */
    @Update("UPDATE payment SET status = #{to}, callback_time = NOW() WHERE pay_no = #{payNo} AND status = #{from}")
    int markCallbackSuccess(@Param("payNo") String payNo, @Param("from") String from, @Param("to") String to);

    /**
     * 状态流转（CAS）：带上原状态作为条件，返回值 0 表示状态已被其他操作变更。
     */
    @Update("UPDATE payment SET status = #{to} WHERE pay_no = #{payNo} AND status = #{from}")
    int updateStatusCAS(@Param("payNo") String payNo, @Param("from") String from, @Param("to") String to);

    /** 动态条件分页查询，见 PaymentMapper.xml */
    List<Payment> selectByCondition(PaymentQueryDTO query);

    /** 动态条件统计总数，见 PaymentMapper.xml */
    long countByCondition(PaymentQueryDTO query);
}
