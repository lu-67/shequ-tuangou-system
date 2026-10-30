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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M0 用户与权限 接口集成测试（MockMvc + 真实 Spring 容器 + MySQL）
 *
 * 约定与 M1 保持一致：spring.sql.init.mode=never 不重置库、@Transactional 自动回滚、
 * 登录上下文用 user(JwtUser) 构造，不依赖 JWT 过滤器。
 *
 * 覆盖：登录成功 / 密码错误(1003) / 用户不存在(1003) / 参数校验空用户名(400) /
 *       注册成功 / 用户名重复(1001) / 越权（居民访问管理端统计接口 -> 403）。
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static JwtUser adminUser() {
        return new JwtUser(1L, "admin", Constants.ROLE_ADMIN, "N/A");
    }

    private static JwtUser residentUser() {
        return new JwtUser(5L, "user05", Constants.ROLE_RESIDENT, "N/A");
    }

    @Test
    void login_success_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    /** 异常分支：密码错误返回业务码 1003（不再是 500） */
    @Test
    void login_wrongPassword_returns1003() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrongpwd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    /** 异常分支：用户不存在同样返回 1003，且不泄露账号是否存在 */
    @Test
    void login_userNotFound_returns1003() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"not_exist_user\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    /** 非法参数：用户名为空，@Valid 触发 400 */
    @Test
    void login_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void register_success() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test_resident_alpha\",\"password\":\"pwd123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"));
    }

    /** 异常分支：用户名重复返回业务码 1001 */
    @Test
    void register_duplicateUsername_returns1001() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    /** 越权：居民身份访问仅 ADMIN/HEAD 可访问的统计接口，应被方法级鉴权拒绝 */
    @Test
    void statsOverview_asResident_forbidden() throws Exception {
        mockMvc.perform(get("/api/stats/overview").with(user(residentUser())))
                .andExpect(status().isForbidden());
    }
}
