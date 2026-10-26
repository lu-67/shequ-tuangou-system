package com.community.groupbuy.controller;

import com.community.groupbuy.common.Constants;
import com.community.groupbuy.config.JwtUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M5 自提核销 接口集成测试（MockMvc + 真实 Spring 容器 + MySQL）
 *
 * 验收硬指标在这里落地：同一核销码连续核销 5 次，仅第 1 次成功。
 * 演示数据中：
 * - CG10000001 对应订单 1（PAID），CG10000007 对应订单 7（PICKED）；
 * - 自提点 id 1 存在。
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@Transactional
class PickupVerificationControllerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    private static JwtUser headUser() {
        return new JwtUser(2L, "head01", Constants.ROLE_HEAD, "N/A");
    }

    private static JwtUser residentUser() {
        return new JwtUser(5L, "user05", Constants.ROLE_RESIDENT, "N/A");
    }

    private static String body(String code, long pointId) {
        return "{\"verificationCode\":\"" + code + "\",\"pickupPointId\":" + pointId + "}";
    }

    private JsonNode verifyOnce(JwtUser loginUser, String code, long pointId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/pickup/verify")
                        .with(user(loginUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(code, pointId)))
                .andExpect(status().isOk())
                .andReturn();
        return MAPPER.readTree(result.getResponse().getContentAsString());
    }

    /** 验收硬指标：同一核销码连续核销 5 次，仅 1 次成功，其余均为 4002 */
    @Test
    void verify_sameCodeFiveTimes_onlyFirstSucceeds() throws Exception {
        int successCount = 0;
        int alreadyDoneCount = 0;
        for (int i = 1; i <= 5; i++) {
            JsonNode json = verifyOnce(headUser(), "CG10000001", 1L);
            int code = json.get("code").asInt();
            if (code == 200) {
                successCount++;
            } else if (code == 4002) {
                alreadyDoneCount++;
            }
        }
        assertThat(successCount).isEqualTo(1);
        assertThat(alreadyDoneCount).isEqualTo(4);
    }

    /** 核销码不存在 → 4001 */
    @Test
    void verify_codeNotFound_returns4001() throws Exception {
        mockMvc.perform(post("/api/pickup/verify")
                        .with(user(headUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOT_EXIST_CODE", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(4001)));
    }

    /** 订单已是 PICKED → 4002 */
    @Test
    void verify_alreadyPicked_returns4002() throws Exception {
        mockMvc.perform(post("/api/pickup/verify")
                        .with(user(headUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CG10000007", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(4002)));
    }

    /** 自提点不存在 → 4101 */
    @Test
    void verify_pointNotFound_returns4101() throws Exception {
        mockMvc.perform(post("/api/pickup/verify")
                        .with(user(headUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CG10000001", 999999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(4101)));
    }

    /** 越权：居民核销 → 403 */
    @Test
    void verify_residentForbidden_returns403() throws Exception {
        mockMvc.perform(post("/api/pickup/verify")
                        .with(user(residentUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CG10000001", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(403)));
    }

    /** 参数校验：核销码为空 → 400 */
    @Test
    void verify_blankCode_returns400() throws Exception {
        mockMvc.perform(post("/api/pickup/verify")
                        .with(user(headUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verificationCode\":\"\",\"pickupPointId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)));
    }
}
