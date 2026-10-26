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
 * 覆盖：核销成功、核销码不存在、订单未支付、订单已核销、并发 CAS 失败（影响行数 0）、
 *       自提点不存在、居民越权、唯一索引兜底冲突。
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

    /** 核销成功：订单 PAID → PICKED，写入 DONE 记录 */
    @Test
    void verify_success_returnsRecordId() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf());
        when(orderMapper.selectByVerificationCode("CG10000001")).thenReturn(orderOf(Constants.ORDER_PAID));
        when(orderMapper.updateStatusCAS(1L, Constants.ORDER_PAID, Constants.ORDER_PICKED)).thenReturn(1);
        doAnswer(invocation -> {
            PickupVerification arg = invocation.getArgument(0);
            arg.setId(88L);
            return 1;
        }).when(pickupVerificationMapper).insert(any(PickupVerification.class));

        Long id = verificationService.verify(dto(), Constants.ROLE_HEAD);

        assertThat(id).isEqualTo(88L);
        ArgumentCaptor<PickupVerification> captor = ArgumentCaptor.forClass(PickupVerification.class);
        verify(pickupVerificationMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Constants.VERIFY_DONE);
        assertThat(captor.getValue().getOrderId()).isEqualTo(1L);
        assertThat(captor.getValue().getPickupPointId()).isEqualTo(1L);
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

    /** 并发场景：CAS 影响行数 0，说明已被其他请求核销 */
    @Test
    void verify_casFailed_throws4002() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf());
        when(orderMapper.selectByVerificationCode("CG10000001")).thenReturn(orderOf(Constants.ORDER_PAID));
        when(orderMapper.updateStatusCAS(1L, Constants.ORDER_PAID, Constants.ORDER_PICKED)).thenReturn(0);

        assertThatThrownBy(() -> verificationService.verify(dto(), Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.VERIFY_ALREADY_DONE.getCode());
        verify(pickupVerificationMapper, never()).insert(any(PickupVerification.class));
    }

    /** 唯一索引兜底：插入核销记录冲突同样按重复核销处理 */
    @Test
    void verify_duplicateKey_throws4002() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf());
        when(orderMapper.selectByVerificationCode("CG10000001")).thenReturn(orderOf(Constants.ORDER_PAID));
        when(orderMapper.updateStatusCAS(1L, Constants.ORDER_PAID, Constants.ORDER_PICKED)).thenReturn(1);
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
