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
 * M1 商品管理 接口集成测试（MockMvc + 真实 Spring 容器 + MySQL）
 *
 * 与 M2/M3 的集成测试保持一致：
 * - spring.sql.init.mode=never：测试不重置数据库，保护演示数据；
 * - @Transactional：每个测试的数据库变更自动回滚；
 * - 登录上下文用 user(JwtUser) 构造，不依赖 M0 的 JWT 过滤器。
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static JwtUser adminUser() {
        return new JwtUser(1L, "admin", Constants.ROLE_ADMIN, "N/A");
    }

    private static JwtUser residentUser() {
        return new JwtUser(5L, "user05", Constants.ROLE_RESIDENT, "N/A");
    }

    private static String createBody() {
        return "{\"name\":\"测试商品-有机小白菜\",\"category\":\"蔬菜\",\"spec\":\"500g/份\",\"unit\":\"份\","
                + "\"price\":6.80,\"groupPrice\":4.90,\"stock\":200,\"imageUrl\":\"\",\"description\":\"\",\"status\":1}";
    }

    /** 上架商品列表：居民端首页要的是数组 */
    @Test
    void listOnShelf_returnsArray() throws Exception {
        mockMvc.perform(get("/api/products/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data").isArray());
    }

    /** 分页查询：返回 { total, list } 对象，不是数组 */
    @Test
    void page_returnsPageObject() throws Exception {
        mockMvc.perform(get("/api/products").param("pageNum", "1").param("pageSize", "5")
                        .with(user(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.total").exists())
                .andExpect(jsonPath("$.data.list").isArray());
    }

    /** 管理员新增商品：返回新商品 ID */
    @Test
    void create_asAdmin_success() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(user(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data").isNumber());
    }

    /** 越权：居民新增商品，返回业务码 403（统一响应体，HTTP 200） */
    @Test
    void create_asResident_forbidden() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(user(residentUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(403)));
    }

    /** 参数校验：名称为空返回 400 */
    @Test
    void create_blankName_returns400() throws Exception {
        String body = createBody().replace("\"name\":\"测试商品-有机小白菜\"", "\"name\":\"\"");
        mockMvc.perform(post("/api/products")
                        .with(user(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)));
    }

    /** 商品不存在：详情返回业务码 1101 */
    @Test
    void get_notFound_returns1101() throws Exception {
        mockMvc.perform(get("/api/products/999999")
                        .with(user(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1101)));
    }

    /** 上下架：状态值非法返回业务码 1103 */
    @Test
    void updateStatus_invalidValue_returns1103() throws Exception {
        mockMvc.perform(put("/api/products/1/status")
                        .with(user(adminUser()))
                        .param("status", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1103)));
    }
}
