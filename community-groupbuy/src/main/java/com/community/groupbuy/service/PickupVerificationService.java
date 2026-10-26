package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.Constants;
import com.community.groupbuy.common.ResultCode;
import com.community.groupbuy.dto.VerifyDTO;
import com.community.groupbuy.entity.GroupOrder;
import com.community.groupbuy.entity.PickupPoint;
import com.community.groupbuy.entity.PickupVerification;
import com.community.groupbuy.mapper.OrderMapper;
import com.community.groupbuy.mapper.PickupPointMapper;
import com.community.groupbuy.mapper.PickupVerificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * M5 自提核销（验收硬指标：同一核销码连续核销 5 次，仅第 1 次成功）
 *
 * 核销语义（方案 A，与数据模型匹配）：data.sql 为已支付订单预置了 PENDING 核销记录，
 * 核销 = UPDATE pickup_verification SET status='DONE' WHERE order_id=? AND status='PENDING'。
 *
 * 幂等设计（三道防线，缺一不可）：
 * 1) 先判断订单状态：已 PICKED → 直接拒绝（4002）；未 PAID → 拒绝（3002）；
 * 2) 数据库状态机 CAS：UPDATE group_order SET status='PICKED' WHERE id=? AND status='PAID'，
 *    影响行数 = 0 说明已被别的请求核销，抛 4002。并发下只有一个线程能改成功；
 * 3) 核销记录 UPDATE ... WHERE status='PENDING' 影响行数=0 时：
 *    - 已有记录（已被核销）→ 4002；
 *    - 无记录（如 M4 真实支付流未预置）→ INSERT 兜底，uk_order_id/uk_code 唯一索引再兜一层。
 *
 * 整个过程在同一事务内，不会出现"订单已核销但没记录"。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PickupVerificationService {

    private final OrderMapper orderMapper;

    private final PickupPointMapper pickupPointMapper;

    private final PickupVerificationMapper pickupVerificationMapper;

    /**
     * 核销：返回核销订单 ID
     *
     * @param operatorRole 当前登录人角色，仅 ADMIN / HEAD 可核销
     */
    @Transactional(rollbackFor = Exception.class)
    public Long verify(VerifyDTO dto, String operatorRole) {
        assertAdminOrHead(operatorRole);
        PickupPoint point = requirePoint(dto.getPickupPointId());
        GroupOrder order = requireOrderByCode(dto.getVerificationCode());

        assertCanVerify(order);

        // 核心：数据库状态机 CAS，并发下仅一个请求能把 PAID 改成 PICKED
        int orderRows = orderMapper.updateStatusCAS(order.getId(), Constants.ORDER_PAID, Constants.ORDER_PICKED);
        if (orderRows != 1) {
            throw new BusinessException(ResultCode.VERIFY_ALREADY_DONE);
        }

        // 方案 A：核销 = 更新预置 PENDING 记录为 DONE
        int recordRows = pickupVerificationMapper.updateToDone(order.getId(), point.getId());
        if (recordRows != 1) {
            resolveZeroRowConflict(order);
        }

        log.info("核销成功: orderId={}, orderNo={}, code={}, pickupPoint={}, operator={}",
                order.getId(), order.getOrderNo(), order.getVerificationCode(), point.getName(), operatorRole);
        return order.getId();
    }

    /**
     * UPDATE 影响 0 行的两种可能：
     * - 记录存在但已非 PENDING（已被核销）→ 4002；
     * - 无记录（如 M4 真实支付流未预置）→ INSERT 一条 DONE 记录兜底，唯一索引再兜一层。
     */
    private void resolveZeroRowConflict(GroupOrder order) {
        PickupVerification existing = pickupVerificationMapper.selectByOrderId(order.getId());
        if (existing != null) {
            throw new BusinessException(ResultCode.VERIFY_ALREADY_DONE);
        }
        PickupVerification record = new PickupVerification();
        record.setOrderId(order.getId());
        record.setVerificationCode(order.getVerificationCode());
        record.setStatus(Constants.VERIFY_DONE);
        try {
            pickupVerificationMapper.insert(record);
        } catch (DuplicateKeyException e) {
            log.warn("核销记录唯一索引冲突: orderId={}, code={}", order.getId(), order.getVerificationCode());
            throw new BusinessException(ResultCode.VERIFY_ALREADY_DONE);
        }
    }

    /** 订单状态校验：已核销 / 未支付都不允许核销 */
    private void assertCanVerify(GroupOrder order) {
        if (Constants.ORDER_PICKED.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.VERIFY_ALREADY_DONE);
        }
        if (!Constants.ORDER_PAID.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单未支付或已取消，不可核销");
        }
    }

    private GroupOrder requireOrderByCode(String code) {
        String trimmed = code == null ? "" : code.trim();
        GroupOrder order = orderMapper.selectByVerificationCode(trimmed);
        if (order == null) {
            throw new BusinessException(ResultCode.VERIFY_CODE_INVALID);
        }
        return order;
    }

    private PickupPoint requirePoint(Long pickupPointId) {
        PickupPoint point = pickupPointMapper.selectById(pickupPointId);
        if (point == null) {
            throw new BusinessException(ResultCode.PICKUP_POINT_NOT_FOUND);
        }
        return point;
    }

    private void assertAdminOrHead(String role) {
        if (!Constants.ROLE_ADMIN.equals(role) && !Constants.ROLE_HEAD.equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员或团长可执行核销");
        }
    }
}
