package com.community.groupbuy.controller;

import com.community.groupbuy.common.Constants;
import com.community.groupbuy.config.JwtUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M6 后台统计导出 接口集成测试（MockMvc + 真实 Spring 容器 + MySQL）
 *
 * 覆盖：概览统计（ADMIN/HEAD 可访问，返回 200+data）、订单导出（ADMIN 返回 xlsx）、
 *       鉴权失败（无 token -> 403）、越权（居民访问 -> 403）。
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@Transactional
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static JwtUser adminUser() {
        return new JwtUser(1L, "admin", Constants.ROLE_ADMIN, "N/A");
    }

    private static JwtUser headUser() {
        return new JwtUser(2L, "head1", Constants.ROLE_HEAD, "N/A");
    }

    private static JwtUser residentUser() {
        return new JwtUser(5L, "user05", Constants.ROLE_RESIDENT, "N/A");
    }

    @Test
    void overview_asAdmin_success() throws Exception {
        mockMvc.perform(get("/api/stats/overview").with(user(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void overview_asHead_success() throws Exception {
        mockMvc.perform(get("/api/stats/overview").with(user(headUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    /** 鉴权失败：未携带 token 访问受保护接口，项目统一返回 403（未配 AuthenticationEntryPoint） */
    @Test
    void overview_noToken_forbidden() throws Exception {
        mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isForbidden());
    }

    /** 越权：居民身份访问仅 ADMIN/HEAD 可访问的统计接口，应 403 */
    @Test
    void overview_asResident_forbidden() throws Exception {
        mockMvc.perform(get("/api/stats/overview").with(user(residentUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportOrders_asAdmin_returnsXlsx() throws Exception {
        mockMvc.perform(get("/api/stats/orders/export").with(user(adminUser())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        containsString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().string("Content-disposition", containsString(".xlsx")));
    }

    /** 越权：居民身份访问导出接口，应 403 */
    @Test
    void exportOrders_asResident_forbidden() throws Exception {
        mockMvc.perform(get("/api/stats/orders/export").with(user(residentUser())))
                .andExpect(status().isForbidden());
    }
}
