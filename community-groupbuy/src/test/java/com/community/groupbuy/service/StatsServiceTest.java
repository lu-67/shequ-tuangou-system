package com.community.groupbuy.service;

import com.community.groupbuy.mapper.StatsMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * M6 后台统计导出 Service 单元测试（Mockito，不启动 Spring 容器、不依赖数据库）
 *
 * 覆盖：概览统计正常返回 / 概览为空不抛异常；订单导出有数据 / 空列表 / null 列表 / IO 异常分支。
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private StatsMapper statsMapper;

    @InjectMocks
    private StatsServiceImpl statsService;

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Test
    void getOverviewStats_returnsMapperResult() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("orderCount", 320);
        overview.put("totalAmount", 12345.6);
        when(statsMapper.getOverviewStats(null)).thenReturn(overview);

        Map<String, Object> result = statsService.getOverviewStats();
        assertThat(result).isSameAs(overview);
    }

    /** 异常分支：mapper 返回 null 时不应抛出 NPE，调用方透传即可 */
    @Test
    void getOverviewStats_mapperReturnsNull_doesNotThrow() {
        when(statsMapper.getOverviewStats(null)).thenReturn(null);

        Map<String, Object> result = statsService.getOverviewStats();
        assertThat(result).isNull();
    }

    @Test
    void exportOrders_withData_writesXlsx() throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("orderNo", "NO20261001001");
        row.put("userId", 5L);
        row.put("amount", 29.9);
        row.put("status", "PAID");
        row.put("createTime", "2026-10-01 10:00:00");
        rows.add(row);
        when(statsMapper.getOrderListForExport(null)).thenReturn(rows);

        MockHttpServletResponse response = new MockHttpServletResponse();
        statsService.exportOrders(response);

        assertThat(response.getContentType()).contains(XLSX);
        assertThat(response.getContentAsByteArray().length).isGreaterThan(0);
    }

    @Test
    void exportOrders_emptyList_writesHeaderOnly() throws Exception {
        when(statsMapper.getOrderListForExport(null)).thenReturn(new ArrayList<>());

        MockHttpServletResponse response = new MockHttpServletResponse();
        statsService.exportOrders(response);

        assertThat(response.getContentType()).contains(XLSX);
        assertThat(response.getContentAsByteArray().length).isGreaterThan(0);
    }

    /** 异常分支：mapper 返回 null 时，service 兜底为空列表，不抛 NPE */
    @Test
    void exportOrders_nullList_treatedAsEmpty() throws Exception {
        when(statsMapper.getOrderListForExport(null)).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        statsService.exportOrders(response);

        assertThat(response.getContentType()).contains(XLSX);
        assertThat(response.getContentAsByteArray().length).isGreaterThan(0);
    }

    /** 异常分支：响应流写出失败时应抛出友好的业务 RuntimeException，而非裸 IOException */
    @Test
    void exportOrders_ioException_rethrowsRuntimeException() throws Exception {
        when(statsMapper.getOrderListForExport(null)).thenReturn(new ArrayList<>());
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> statsService.exportOrders(response))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("导出Excel失败");
    }
}
