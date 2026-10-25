package com.community.groupbuy.controller;

import com.community.groupbuy.common.Constants;
import com.community.groupbuy.config.JwtUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M2 开团活动 接口集成测试（MockMvc + 真实 Spring 容器 + MySQL）
 *
 * 说明：
 * - spring.sql.init.mode=never：测试不重置数据库，保护演示数据；
 * - @Transactional：每个测试的数据库变更自动回滚；
 * - 登录上下文用 SecurityMockMvcRequestPostProcessors.user(JwtUser) 构造（与 M3 测试一致）。
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@Transactional
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static JwtUser headUser() {
        return new JwtUser(2L, "head01", Constants.ROLE_HEAD, "N/A");
    }

    private static JwtUser residentUser() {
        return new JwtUser(5L, "user05", Constants.ROLE_RESIDENT, "N/A");
    }

    private static String createBody() {
        return "{\"productId\":1,\"title\":\"测试拼团活动\",\"originPrice\":39.90,\"groupPrice\":29.90,"
                + "\"totalStock\":100,\"perLimit\":5,"
                + "\"startTime\":\"2026-11-01 00:00:00\",\"endTime\":\"2026-11-30 23:59:59\"}";
    }

    /** 团长开团：返回活动 ID */
    @Test
    void createActivity_success() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .with(user(headUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data").isNumber());
    }

    /** 参数校验：总库存为 0，返回 400 */
    @Test
    void createActivity_invalidParam_returns400() throws Exception {
        String body = createBody().replace("\"totalStock\":100", "\"totalStock\":0");
        mockMvc.perform(post("/api/activities")
                        .with(user(headUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)));
    }

    /** 越权：居民开团，返回 403 */
    @Test
    void createActivity_residentForbidden_returns403() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .with(user(residentUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(403)));
    }

    /** 状态流转：演示数据中活动 1 已是 ONGOING，再流转到 ONGOING 属非法流转，返回 2004 */
    @Test
    void updateStatus_illegalTransition_returns2004() throws Exception {
        mockMvc.perform(put("/api/activities/1/status")
                        .param("status", Constants.ACTIVITY_ONGOING)
                        .with(user(headUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(2004)));
    }

    /** 活动不存在：返回 2001 */
    @Test
    void getActivity_notFound_returns2001() throws Exception {
        mockMvc.perform(get("/api/activities/999999")
                        .with(user(headUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(2001)));
    }

    /** 分页查询：返回分页结构 */
    @Test
    void pageActivities_returnsPageResult() throws Exception {
        mockMvc.perform(get("/api/activities")
                        .param("status", Constants.ACTIVITY_ONGOING)
                        .param("pageNum", "1")
                        .param("pageSize", "5")
                        .with(user(headUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.list").isArray());
    }

    /** 进行中列表：返回数组 */
    @Test
    void listOngoing_returnsArray() throws Exception {
        mockMvc.perform(get("/api/activities/ongoing")
                        .with(user(residentUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data").isArray());
    }
}
