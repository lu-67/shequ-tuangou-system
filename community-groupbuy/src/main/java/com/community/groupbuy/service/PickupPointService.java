package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.Constants;
import com.community.groupbuy.common.ResultCode;
import com.community.groupbuy.dto.PickupPointDTO;
import com.community.groupbuy.entity.PickupPoint;
import com.community.groupbuy.mapper.PickupPointMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * M5 自提点管理：列表（居民端首页）/ 详情 / 新增 / 修改 / 删除
 *
 * 写操作仅 ADMIN / HEAD；已有核销记录的自提点禁止删除。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PickupPointService {

    private final PickupPointMapper pickupPointMapper;

    /** 自提点列表（居民端首页、我的订单、管理后台共用），name 可选模糊 */
    public List<PickupPoint> list(String name) {
        return pickupPointMapper.selectByCondition(name);
    }

    public PickupPoint getById(Long id) {
        return requirePoint(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(PickupPointDTO dto, String operatorRole) {
        assertAdminOrHead(operatorRole);
        PickupPoint point = build(dto);
        pickupPointMapper.insert(point);
        log.info("自提点创建成功: id={}, name={}, operator={}", point.getId(), point.getName(), operatorRole);
        return point.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PickupPointDTO dto, String operatorRole) {
        assertAdminOrHead(operatorRole);
        requirePoint(id);
        PickupPoint point = build(dto);
        point.setId(id);
        int rows = pickupPointMapper.update(point);
        if (rows != 1) {
            throw new BusinessException(ResultCode.PICKUP_POINT_NOT_FOUND, "自提点已被删除，更新失败");
        }
        log.info("自提点更新成功: id={}, name={}, operator={}", id, point.getName(), operatorRole);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, String operatorRole) {
        assertAdminOrHead(operatorRole);
        requirePoint(id);
        long refs = pickupPointMapper.countVerificationRef(id);
        if (refs > 0) {
            throw new BusinessException(ResultCode.PICKUP_POINT_REFERENCED,
                    "该自提点已有 " + refs + " 条核销记录，不可删除");
        }
        pickupPointMapper.deleteById(id);
        log.info("自提点删除成功: id={}, operator={}", id, operatorRole);
    }

    private PickupPoint requirePoint(Long id) {
        PickupPoint point = pickupPointMapper.selectById(id);
        if (point == null) {
            throw new BusinessException(ResultCode.PICKUP_POINT_NOT_FOUND);
        }
        return point;
    }

    private void assertAdminOrHead(String role) {
        if (!Constants.ROLE_ADMIN.equals(role) && !Constants.ROLE_HEAD.equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员或团长可管理自提点");
        }
    }

    private PickupPoint build(PickupPointDTO dto) {
        PickupPoint point = new PickupPoint();
        point.setName(dto.getName().trim());
        point.setAddress(dto.getAddress().trim());
        point.setManager(dto.getManager() == null ? "" : dto.getManager());
        point.setPhone(dto.getPhone() == null ? "" : dto.getPhone());
        return point;
    }
}
