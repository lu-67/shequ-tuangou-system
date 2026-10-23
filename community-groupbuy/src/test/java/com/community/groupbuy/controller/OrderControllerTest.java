package com.community.groupbuy.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M3 参团下单 接口集成测试（MockMvc + 真实 Spring 容器 + MySQL）
 *
 * 前置条件：MySQL(3307) 已启动，且库中已有演示数据
 * （先执行一次 mvn spring-boot:run 自动初始化，或手动执行 db/schema.sql + db/data.sql）。
 *
 * 说明：
 * - spring.sql.init.mode=never：测试不重置数据库，保护演示数据；
 * - @Transactional：每个测试的数据库变更自动回滚；
 * - 登录上下文用 SecurityMockMvcRequestPostProcessors.user(JwtUser) 构造
 *   （JwtUser 实现了 UserDetails，可直接作为认证主体，与线上 JWT 过滤器放入的主体一致）；
 * - M0（D 同学）实现后，可改用登录接口拿 token + Authorization 头，并补「鉴权失败/越权」用例。
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@Transactional
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** 构造居民 user05 的登录上下文（与 JWT 过滤器放入 SecurityContext 的主体一致） */
    private static JwtUser residentUser() {
        return new JwtUser(5L, "user05", "RESIDENT", "N/A");
    }

    /** 正常下单：返回订单 ID */
    @Test
    void createOrder_success() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(user(residentUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityId\":1,\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data").isNumber());
    }

    /** 非法参数：数量为 0，触发 @Valid 校验，返回 400 */
    @Test
    void createOrder_invalidQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(user(residentUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityId\":1,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)));
    }

    /** 业务异常：超过每人限购数量，返回统一响应体中的业务错误码 */
    @Test
    void createOrder_exceedPerLimit_returnsBusinessError() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(user(residentUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityId\":1,\"quantity\":9999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("超过每人限购数量")));
    }

    /** 订单不存在：返回业务错误码 3001 */
    @Test
    void getOrder_notFound_returnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/orders/999999")
                        .with(user(residentUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(3001)));
    }
}
