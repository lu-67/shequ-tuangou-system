package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.Constants;
import com.community.groupbuy.common.ResultCode;
import com.community.groupbuy.dto.PickupPointDTO;
import com.community.groupbuy.entity.PickupPoint;
import com.community.groupbuy.mapper.PickupPointMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M5 自提点 Service 单元测试（Mockito）
 *
 * 覆盖：列表、新增成功/越权、修改成功/不存在、删除被核销记录引用/删除成功。
 */
@ExtendWith(MockitoExtension.class)
class PickupPointServiceTest {

    @Mock
    private PickupPointMapper pickupPointMapper;

    @InjectMocks
    private PickupPointService pickupPointService;

    private PickupPointDTO dto() {
        PickupPointDTO dto = new PickupPointDTO();
        dto.setName("阳光花园自提点");
        dto.setAddress("阳光花园东门 1 号铺");
        dto.setManager("王阿姨");
        dto.setPhone("13800000001");
        return dto;
    }

    private PickupPoint pointOf(Long id) {
        PickupPoint p = new PickupPoint();
        p.setId(id);
        p.setName("阳光花园自提点");
        p.setAddress("阳光花园东门 1 号铺");
        return p;
    }

    /** 列表：居民端首页不传条件即全量 */
    @Test
    void list_withoutName_returnsAll() {
        when(pickupPointMapper.selectByCondition(null)).thenReturn(List.of(pointOf(1L), pointOf(2L)));
        assertThat(pickupPointService.list(null)).hasSize(2);
    }

    /** 新增成功：回填主键 */
    @Test
    void create_success_returnsId() {
        doAnswer(invocation -> {
            PickupPoint arg = invocation.getArgument(0);
            arg.setId(21L);
            return 1;
        }).when(pickupPointMapper).insert(any(PickupPoint.class));

        Long id = pickupPointService.create(dto(), Constants.ROLE_ADMIN);

        assertThat(id).isEqualTo(21L);
    }

    /** 越权：居民新增自提点被拒绝 */
    @Test
    void create_residentForbidden() {
        assertThatThrownBy(() -> pickupPointService.create(dto(), Constants.ROLE_RESIDENT))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.FORBIDDEN.getCode());
        verify(pickupPointMapper, never()).insert(any(PickupPoint.class));
    }

    /** 修改成功 */
    @Test
    void update_success() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf(1L));
        when(pickupPointMapper.update(any(PickupPoint.class))).thenReturn(1);

        pickupPointService.update(1L, dto(), Constants.ROLE_HEAD);

        verify(pickupPointMapper).update(any(PickupPoint.class));
    }

    /** 异常分支：自提点不存在 */
    @Test
    void update_notFound_throws4101() {
        when(pickupPointMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> pickupPointService.update(99L, dto(), Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PICKUP_POINT_NOT_FOUND.getCode());
    }

    /** 异常分支：已有核销记录，禁止删除 */
    @Test
    void delete_referenced_throws4102() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf(1L));
        when(pickupPointMapper.countVerificationRef(1L)).thenReturn(2L);

        assertThatThrownBy(() -> pickupPointService.delete(1L, Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PICKUP_POINT_REFERENCED.getCode());
        verify(pickupPointMapper, never()).deleteById(anyLong());
    }

    /** 删除成功 */
    @Test
    void delete_success() {
        when(pickupPointMapper.selectById(1L)).thenReturn(pointOf(1L));
        when(pickupPointMapper.countVerificationRef(1L)).thenReturn(0L);
        when(pickupPointMapper.deleteById(1L)).thenReturn(1);

        pickupPointService.delete(1L, Constants.ROLE_ADMIN);

        verify(pickupPointMapper).deleteById(1L);
    }
}
