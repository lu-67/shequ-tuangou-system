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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M5 自提核销 Service 单元测试（Mockito）
 *
 * 核销语义（方案 A）：更新预置 PENDING 记录为 DONE。
 * 覆盖：核销成功、核销码不存在、订单未支付、订单已核销、并发 CAS 失败、
 *       PENDING 记录已被核销（UPDATE 0 行 + 记录存在）、
 *       无预置记录兜底 INSERT、唯一索引冲突、自提点不存在、居民越权。
 */
@ExtendWith(MockitoExtension.class)
class PickupVerificationServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PickupPointMapper pickupPointMapper;

    @Mock
    private PickupVerificationMapper pickupVerificationMapper;

    @InjectMocks
    private PickupVerificationService verificationService;

    private VerifyDTO dto() {
        VerifyDTO dto = new VerifyDTO();
        dto.setVerificationCode("CG10000001");
        dto.setPickupPointId(1L);
        return dto;
    }

    private GroupOrder orderOf(String status) {
        GroupOrder order = new GroupOrder();
        order.setId(1L);
        order.setOrderNo("NO2026000001");
        order.setVerificationCode("CG10000001");
        order.setStatus(status);
        return order;
    }

    private PickupPoint pointOf() {
        PickupPoint point = new PickupPoint();
        point.setId(1L);
        point.setName("阳光花园自提点");
        point.setAddress("阳光花园东门 1 号铺");
        return point;
    }

    /** 核销成功：订单 PAID → PICKED，预置 PENDING 记录被更新为 DONE */
    @Test
    void verify_success_returnsOrderId() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf());
        when(orderMapper.selectByVerificationCode("CG10000001")).thenReturn(orderOf(Constants.ORDER_PAID));
        when(orderMapper.updateStatusCAS(1L, Constants.ORDER_PAID, Constants.ORDER_PICKED)).thenReturn(1);
        when(pickupVerificationMapper.updateToDone(1L, 1L)).thenReturn(1);

        Long orderId = verificationService.verify(dto(), Constants.ROLE_HEAD);

        assertThat(orderId).isEqualTo(1L);
        verify(pickupVerificationMapper).updateToDone(1L, 1L);
        verify(pickupVerificationMapper, never()).insert(any(PickupVerification.class));
    }

    /** 异常分支：核销码不存在 */
    @Test
    void verify_codeNotFound_throws4001() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf());
        when(orderMapper.selectByVerificationCode("CG10000001")).thenReturn(null);

        assertThatThrownBy(() -> verificationService.verify(dto(), Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.VERIFY_CODE_INVALID.getCode());
        verify(orderMapper, never()).updateStatusCAS(anyLong(), anyString(), anyString());
    }

    /** 异常分支：订单未支付 */
    @Test
    void verify_orderNotPaid_throws3002() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf());
        when(orderMapper.selectByVerificationCode("CG10000001")).thenReturn(orderOf(Constants.ORDER_UNPAID));

        assertThatThrownBy(() -> verificationService.verify(dto(), Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.ORDER_STATUS_ERROR.getCode());
        verify(orderMapper, never()).updateStatusCAS(anyLong(), anyString(), anyString());
    }

    /** 异常分支：订单已核销（PICKED） */
    @Test
    void verify_alreadyPicked_throws4002() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf());
        when(orderMapper.selectByVerificationCode("CG10000001")).thenReturn(orderOf(Constants.ORDER_PICKED));

        assertThatThrownBy(() -> verificationService.verify(dto(), Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.VERIFY_ALREADY_DONE.getCode());
    }

    /** 并发场景：订单状态 CAS 影响行数 0，说明已被其他请求核销 */
    @Test
    void verify_casFailed_throws4002() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf());
        when(orderMapper.selectByVerificationCode("CG10000001")).thenReturn(orderOf(Constants.ORDER_PAID));
        when(orderMapper.updateStatusCAS(1L, Constants.ORDER_PAID, Constants.ORDER_PICKED)).thenReturn(0);

        assertThatThrownBy(() -> verificationService.verify(dto(), Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.VERIFY_ALREADY_DONE.getCode());
        verify(pickupVerificationMapper, never()).updateToDone(anyLong(), anyLong());
    }

    /** UPDATE 影响 0 行且记录存在（已非 PENDING，说明已被核销）→ 4002 */
    @Test
    void verify_pendingRecordAlreadyDone_throws4002() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf());
        when(orderMapper.selectByVerificationCode("CG10000001")).thenReturn(orderOf(Constants.ORDER_PAID));
        when(orderMapper.updateStatusCAS(1L, Constants.ORDER_PAID, Constants.ORDER_PICKED)).thenReturn(1);
        when(pickupVerificationMapper.updateToDone(1L, 1L)).thenReturn(0);

        PickupVerification done = new PickupVerification();
        done.setOrderId(1L);
        done.setStatus(Constants.VERIFY_DONE);
        when(pickupVerificationMapper.selectByOrderId(1L)).thenReturn(done);

        assertThatThrownBy(() -> verificationService.verify(dto(), Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.VERIFY_ALREADY_DONE.getCode());
        verify(pickupVerificationMapper, never()).insert(any(PickupVerification.class));
    }

    /** 无预置 PENDING 记录的订单（如 M4 真实支付流）→ INSERT 兜底，核销成功 */
    @Test
    void verify_noPresetRecord_insertsFallbackRecord() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf());
        when(orderMapper.selectByVerificationCode("CG10000001")).thenReturn(orderOf(Constants.ORDER_PAID));
        when(orderMapper.updateStatusCAS(1L, Constants.ORDER_PAID, Constants.ORDER_PICKED)).thenReturn(1);
        when(pickupVerificationMapper.updateToDone(1L, 1L)).thenReturn(0);
        when(pickupVerificationMapper.selectByOrderId(1L)).thenReturn(null);
        doAnswer(invocation -> {
            PickupVerification arg = invocation.getArgument(0);
            arg.setId(88L);
            return 1;
        }).when(pickupVerificationMapper).insert(any(PickupVerification.class));

        Long orderId = verificationService.verify(dto(), Constants.ROLE_ADMIN);

        assertThat(orderId).isEqualTo(1L);
        ArgumentCaptor<PickupVerification> captor = ArgumentCaptor.forClass(PickupVerification.class);
        verify(pickupVerificationMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Constants.VERIFY_DONE);
        assertThat(captor.getValue().getVerificationCode()).isEqualTo("CG10000001");
    }

    /** 唯一索引兜底：兜底 INSERT 冲突同样按重复核销处理 */
    @Test
    void verify_fallbackInsertDuplicateKey_throws4002() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf());
        when(orderMapper.selectByVerificationCode("CG10000001")).thenReturn(orderOf(Constants.ORDER_PAID));
        when(orderMapper.updateStatusCAS(1L, Constants.ORDER_PAID, Constants.ORDER_PICKED)).thenReturn(1);
        when(pickupVerificationMapper.updateToDone(1L, 1L)).thenReturn(0);
        when(pickupVerificationMapper.selectByOrderId(1L)).thenReturn(null);
        doThrow(new DuplicateKeyException("uk_order_id")).when(pickupVerificationMapper)
                .insert(any(PickupVerification.class));

        assertThatThrownBy(() -> verificationService.verify(dto(), Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.VERIFY_ALREADY_DONE.getCode());
    }

    /** 异常分支：自提点不存在 */
    @Test
    void verify_pointNotFound_throws4101() {
        when(pickupPointMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> verificationService.verify(dto(), Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PICKUP_POINT_NOT_FOUND.getCode());
        verify(orderMapper, never()).selectByVerificationCode(anyString());
    }

    /** 越权：居民不可核销 */
    @Test
    void verify_residentForbidden_throws403() {
        assertThatThrownBy(() -> verificationService.verify(dto(), Constants.ROLE_RESIDENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.FORBIDDEN.getCode());
        verify(pickupPointMapper, never()).selectById(anyLong());
    }
}
