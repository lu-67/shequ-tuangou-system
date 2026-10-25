package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.Constants;
import com.community.groupbuy.dto.ActivityQueryDTO;
import com.community.groupbuy.dto.CreateActivityDTO;
import com.community.groupbuy.dto.PageResult;
import com.community.groupbuy.entity.GroupActivity;
import com.community.groupbuy.mapper.ActivityMapper;
import com.community.groupbuy.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M2 开团活动 Service 单元测试（Mockito，不启动 Spring 容器，不依赖数据库）
 *
 * 覆盖：创建成功/居民越权/参数非法（库存/价格/时间）、状态合法流转、
 *       非法流转、活动不存在、并发 CAS 冲突、分页查询、进行中列表、
 *       成团判定（达标/未达标/库存抢光但未支付/非ONGOING/CAS冲突/活动不存在）。
 */
@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private ActivityService activityService;

    private CreateActivityDTO createDTO() {
        CreateActivityDTO dto = new CreateActivityDTO();
        dto.setProductId(1L);
        dto.setTitle("洛川苹果限时拼团");
        dto.setOriginPrice(new BigDecimal("39.90"));
        dto.setGroupPrice(new BigDecimal("29.90"));
        dto.setTotalStock(1000);
        dto.setPerLimit(5);
        dto.setStartTime(LocalDateTime.of(2026, 11, 1, 0, 0));
        dto.setEndTime(LocalDateTime.of(2026, 11, 30, 23, 59, 59));
        return dto;
    }

    private GroupActivity activityOf(Long id, String status, int stock) {
        GroupActivity a = new GroupActivity();
        a.setId(id);
        a.setProductId(1L);
        a.setTitle("测试活动");
        a.setStatus(status);
        a.setTotalStock(1000);
        a.setStock(stock);
        return a;
    }

    /** 创建成功：状态 PENDING、剩余库存 = 总库存、回填主键 */
    @Test
    void createActivity_success_returnsIdAndPending() {
        doAnswer(invocation -> {
            GroupActivity arg = invocation.getArgument(0);
            arg.setId(101L);
            return 1;
        }).when(activityMapper).insert(any(GroupActivity.class));

        Long id = activityService.createActivity(createDTO(), 2L, Constants.ROLE_HEAD);

        assertThat(id).isEqualTo(101L);
        ArgumentCaptor<GroupActivity> captor = ArgumentCaptor.forClass(GroupActivity.class);
        verify(activityMapper).insert(captor.capture());
        GroupActivity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getStock()).isEqualTo(1000);
    }

    /** 异常分支：居民无权开团（403） */
    @Test
    void createActivity_residentForbidden() {
        assertThatThrownBy(() -> activityService.createActivity(createDTO(), 5L, Constants.ROLE_RESIDENT))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(403);
        verify(activityMapper, never()).insert(any());
    }

    /** 异常分支：总库存非法（0 件） */
    @Test
    void createActivity_invalidTotalStock() {
        CreateActivityDTO dto = createDTO();
        dto.setTotalStock(0);

        assertThatThrownBy(() -> activityService.createActivity(dto, 2L, Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(400);
    }

    /** 异常分支：团购价高于原价 */
    @Test
    void createActivity_groupPriceGreaterThanOrigin() {
        CreateActivityDTO dto = createDTO();
        dto.setGroupPrice(new BigDecimal("49.90"));

        assertThatThrownBy(() -> activityService.createActivity(dto, 2L, Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(400);
    }

    /** 异常分支：结束时间早于开始时间 */
    @Test
    void createActivity_endTimeBeforeStartTime() {
        CreateActivityDTO dto = createDTO();
        dto.setEndTime(dto.getStartTime().minusDays(1));

        assertThatThrownBy(() -> activityService.createActivity(dto, 2L, Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(400);
    }

    /** 状态流转：PENDING -> ONGOING（CAS 带上原状态） */
    @Test
    void updateStatus_pendingToOngoing_success() {
        when(activityMapper.selectById(1L)).thenReturn(activityOf(1L, Constants.ACTIVITY_PENDING, 1000));
        when(activityMapper.updateStatusCAS(1L, "PENDING", "ONGOING")).thenReturn(1);

        activityService.updateStatus(1L, "ONGOING", Constants.ROLE_HEAD);

        verify(activityMapper).updateStatusCAS(1L, "PENDING", "ONGOING");
    }

    /** 异常分支：非法流转（PENDING 直接跳 SUCCESS） */
    @Test
    void updateStatus_illegalTransition_throws() {
        when(activityMapper.selectById(1L)).thenReturn(activityOf(1L, Constants.ACTIVITY_PENDING, 1000));

        assertThatThrownBy(() -> activityService.updateStatus(1L, "SUCCESS", Constants.ROLE_HEAD))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(2004);
        verify(activityMapper, never()).updateStatusCAS(anyLong(), anyString(), anyString());
    }

    /** 异常分支：终态不可再流转（SUCCESS -> ONGOING） */
    @Test
    void updateStatus_terminalStateNotTransferable() {
        when(activityMapper.selectById(2L)).thenReturn(activityOf(2L, Constants.ACTIVITY_SUCCESS, 0));

        assertThatThrownBy(() -> activityService.updateStatus(2L, "ONGOING", Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(2004);
    }

    /** 异常分支：活动不存在（2001） */
    @Test
    void updateStatus_activityNotFound() {
        when(activityMapper.selectById(999999L)).thenReturn(null);

        assertThatThrownBy(() -> activityService.updateStatus(999999L, "ONGOING", Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(2001);
    }

    /** 异常分支：并发下状态已被他人变更（CAS 影响行数 0） */
    @Test
    void updateStatus_casConflict_throws() {
        when(activityMapper.selectById(3L)).thenReturn(activityOf(3L, Constants.ACTIVITY_ONGOING, 500));
        when(activityMapper.updateStatusCAS(3L, "ONGOING", "SUCCESS")).thenReturn(0);

        assertThatThrownBy(() -> activityService.updateStatus(3L, "SUCCESS", Constants.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(2004);
    }

    /** 分页查询：返回总数与当前页数据 */
    @Test
    void page_returnsTotalAndList() {
        ActivityQueryDTO query = new ActivityQueryDTO();
        query.setStatus(Constants.ACTIVITY_ONGOING);
        query.setPageNum(1);
        query.setPageSize(10);
        when(activityMapper.countByCondition(query)).thenReturn(2L);
        when(activityMapper.selectByCondition(query)).thenReturn(List.of(activityOf(1L, "ONGOING", 900)));

        PageResult<GroupActivity> page = activityService.page(query);

        assertThat(page.getTotal()).isEqualTo(2L);
        assertThat(page.getList()).hasSize(1);
        assertThat(page.getPageNum()).isEqualTo(1);
    }

    /** 分页查询：无数据时不再查列表，返回空集合 */
    @Test
    void page_emptyResult_skipsListQuery() {
        ActivityQueryDTO query = new ActivityQueryDTO();
        when(activityMapper.countByCondition(query)).thenReturn(0L);

        PageResult<GroupActivity> page = activityService.page(query);

        assertThat(page.getList()).isEmpty();
        verify(activityMapper, never()).selectByCondition(any());
    }

    /** 进行中列表 */
    @Test
    void listOngoing_returnsOngoingActivities() {
        when(activityMapper.selectOngoing()).thenReturn(List.of(activityOf(1L, "ONGOING", 900)));

        List<GroupActivity> list = activityService.listOngoing();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStatus()).isEqualTo("ONGOING");
    }

    /** 成团判定：已参团份数达到 total_stock → SUCCESS */
    @Test
    void markSuccessIfReached_reached_becomesSuccess() {
        when(activityMapper.selectById(4L)).thenReturn(activityOf(4L, Constants.ACTIVITY_ONGOING, 0));
        when(orderMapper.sumPaidQuantity(4L)).thenReturn(1000L);
        when(activityMapper.updateStatusCAS(4L, "ONGOING", "SUCCESS")).thenReturn(1);

        assertThat(activityService.markSuccessIfReached(4L)).isTrue();
        verify(activityMapper).updateStatusCAS(4L, Constants.ACTIVITY_ONGOING, Constants.ACTIVITY_SUCCESS);
    }

    /** 未达标：已参团份数仍小于 total_stock，不流转 */
    @Test
    void markSuccessIfReached_notReached_noTransfer() {
        when(activityMapper.selectById(5L)).thenReturn(activityOf(5L, Constants.ACTIVITY_ONGOING, 10));
        when(orderMapper.sumPaidQuantity(5L)).thenReturn(999L);

        assertThat(activityService.markSuccessIfReached(5L)).isFalse();
        verify(activityMapper, never()).updateStatusCAS(eq(5L), anyString(), anyString());
    }

    /**
     * 口径校验：哪怕库存已被抢光（stock = 0），只要已支付份数不够就不成团。
     * 这正是「已参团份数」与「total_stock - stock」两个口径的区别。
     */
    @Test
    void markSuccessIfReached_stockZeroButUnpaid_noTransfer() {
        when(activityMapper.selectById(6L)).thenReturn(activityOf(6L, Constants.ACTIVITY_ONGOING, 0));
        when(orderMapper.sumPaidQuantity(6L)).thenReturn(880L);

        assertThat(activityService.markSuccessIfReached(6L)).isFalse();
    }

    /** 非 ONGOING（待上架或已终态）活动不参与成团判定 */
    @Test
    void markSuccessIfReached_notOngoing_noTransfer() {
        when(activityMapper.selectById(7L)).thenReturn(activityOf(7L, Constants.ACTIVITY_PENDING, 1000));

        assertThat(activityService.markSuccessIfReached(7L)).isFalse();
        verify(orderMapper, never()).sumPaidQuantity(anyLong());
    }

    /** 并发下 CAS 未抢到（状态已被其他流程改过）：返回 false，不报错 */
    @Test
    void markSuccessIfReached_casConflict_returnsFalse() {
        when(activityMapper.selectById(8L)).thenReturn(activityOf(8L, Constants.ACTIVITY_ONGOING, 0));
        when(orderMapper.sumPaidQuantity(8L)).thenReturn(1000L);
        when(activityMapper.updateStatusCAS(8L, "ONGOING", "SUCCESS")).thenReturn(0);

        assertThat(activityService.markSuccessIfReached(8L)).isFalse();
    }

    /** 活动不存在：返回 false（定时任务批量扫描时不应中断整批） */
    @Test
    void markSuccessIfReached_activityNotFound_returnsFalse() {
        when(activityMapper.selectById(99L)).thenReturn(null);

        assertThat(activityService.markSuccessIfReached(99L)).isFalse();
        verify(activityMapper, never()).updateStatusCAS(anyLong(), anyString(), anyString());
    }
}
