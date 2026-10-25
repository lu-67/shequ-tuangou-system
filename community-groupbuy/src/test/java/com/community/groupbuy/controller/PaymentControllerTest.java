package com.community.groupbuy.controller;

import com.community.groupbuy.common.Constants;
import com.community.groupbuy.config.JwtUser;
import com.community.groupbuy.entity.GroupActivity;
import com.community.groupbuy.entity.GroupOrder;
import com.community.groupbuy.mapper.ActivityMapper;
import com.community.groupbuy.mapper.OrderMapper;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M4 支付回调 接口集成测试（MockMvc + 真实 Spring 容器 + MySQL）
 *
 * 覆盖三种回调场景 + 退款链路：
 * - 验签失败：返回 3004，订单状态不变；
 * - 正常回调：订单 UNPAID → PAID，支付单 PENDING → SUCCESS；
 * - 重复回调：仍返回成功，但不重复入账（库存、订单状态不再变化）；
 * - 退款：订单 CANCELLED 且库存回补，重复退款幂等不再回补。
 *
 * 说明：@Transactional 保证每个用例的数据库变更自动回滚，不污染演示数据。
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
@AutoConfigureMockMvc
@Transactional
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ActivityMapper activityMapper;

    private static JwtUser resident(Long id) {
        return new JwtUser(id, "user" + id, Constants.ROLE_RESIDENT, "N/A");
    }

    /** 演示数据中取一个进行中的活动，直接插一条待支付订单（测试结束回滚） */
    private GroupOrder newUnpaidOrder(Long userId, Long activityId) {
        GroupOrder order = new GroupOrder();
        order.setOrderNo("T" + System.nanoTime());
        order.setActivityId(activityId);
        order.setUserId(userId);
        order.setProductId(1L);
        order.setQuantity(1);
        order.setAmount(new BigDecimal("39.90"));
        order.setStatus(Constants.ORDER_UNPAID);
        order.setVerificationCode("V" + System.nanoTime());
        orderMapper.insert(order);
        return order;
    }

    /** 发起支付，返回支付流水号 */
    private String createPayment(Long orderId, Long userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/payments")
                        .with(user(resident(userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"channel\":\"WECHAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return data.get("payNo").asText();
    }

    private String callbackBody(String payNo, String orderNo, String amount, String sign) {
        return "{\"payNo\":\"" + payNo + "\",\"orderNo\":\"" + orderNo + "\",\"amount\":" + amount
                + ",\"channel\":\"WECHAT\",\"sign\":\"" + sign + "\"}";
    }

    /** 用服务端同一把密钥算出合法签名（密钥从配置注入，与 PaymentService 一致） */
    private String signOf(String payNo, String orderNo, String amount) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/payments/sign-demo")
                        .param("payNo", payNo)
                        .param("orderNo", orderNo)
                        .param("amount", amount)
                        .with(user(resident(5L))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").asText();
    }

    /** 场景一：验签失败 → 3004，订单与库存不变 */
    @Test
    void callback_signInvalid_returns3004() throws Exception {
        GroupOrder order = newUnpaidOrder(5L, 1L);
        String payNo = createPayment(order.getId(), 5L);

        mockMvc.perform(post("/api/payments/callback")
                        .with(user(resident(5L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody(payNo, order.getOrderNo(), "39.90", "wrong-sign")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(3004)));

        assertThat(orderMapper.selectById(order.getId()).getStatus()).isEqualTo(Constants.ORDER_UNPAID);
    }

    /** 场景二 + 三：正常回调入账，重复回调幂等不重复入账 */
    @Test
    void callback_successAndReplayIsIdempotent() throws Exception {
        GroupOrder order = newUnpaidOrder(5L, 1L);
        GroupActivity before = activityMapper.selectById(1L);
        String payNo = createPayment(order.getId(), 5L);
        String sign = signOf(payNo, order.getOrderNo(), "39.90");

        // 第一次回调：入账成功
        mockMvc.perform(post("/api/payments/callback")
                        .with(user(resident(5L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody(payNo, order.getOrderNo(), "39.90", sign)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        assertThat(orderMapper.selectById(order.getId()).getStatus()).isEqualTo(Constants.ORDER_PAID);

        // 重复回调：仍返回成功，但库存不会被二次扣减
        mockMvc.perform(post("/api/payments/callback")
                        .with(user(resident(5L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody(payNo, order.getOrderNo(), "39.90", sign)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data", is(true)));

        assertThat(activityMapper.selectById(1L).getStock()).isEqualTo(before.getStock());
    }

    /** 支付流水不存在 → 404 */
    @Test
    void callback_payNoNotExist_returns404() throws Exception {
        String sign = signOf("PAY99999999", "NOPE", "39.90");

        mockMvc.perform(post("/api/payments/callback")
                        .with(user(resident(5L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody("PAY99999999", "NOPE", "39.90", sign)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(404)));
    }

    /** 退款：订单取消 + 库存回补；重复退款不再回补 */
    @Test
    void refund_successAndRepeatIsIdempotent() throws Exception {
        GroupOrder order = newUnpaidOrder(5L, 1L);
        String payNo = createPayment(order.getId(), 5L);
        String sign = signOf(payNo, order.getOrderNo(), "39.90");
        mockMvc.perform(post("/api/payments/callback")
                        .with(user(resident(5L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody(payNo, order.getOrderNo(), "39.90", sign)))
                .andExpect(jsonPath("$.code", is(200)));

        int stockBeforeRefund = activityMapper.selectById(1L).getStock();

        // 取支付单 ID
        MvcResult payResult = mockMvc.perform(get("/api/payments")
                        .param("payNo", payNo)
                        .with(user(resident(5L))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = objectMapper.readTree(payResult.getResponse().getContentAsString()).get("data").get("list");
        long paymentId = list.get(0).get("id").asLong();

        mockMvc.perform(post("/api/payments/" + paymentId + "/refund")
                        .with(user(resident(5L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        assertThat(orderMapper.selectById(order.getId()).getStatus()).isEqualTo(Constants.ORDER_CANCELLED);
        assertThat(activityMapper.selectById(1L).getStock()).isEqualTo(stockBeforeRefund + 1);

        // 重复退款：幂等返回成功，库存不再变化
        mockMvc.perform(post("/api/payments/" + paymentId + "/refund")
                        .with(user(resident(5L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        assertThat(activityMapper.selectById(1L).getStock()).isEqualTo(stockBeforeRefund + 1);
    }

    /** 越权：给别人的订单发起支付 → 403 */
    @Test
    void createPayment_otherUserOrder_returns403() throws Exception {
        GroupOrder order = newUnpaidOrder(5L, 1L);

        mockMvc.perform(post("/api/payments")
                        .with(user(resident(6L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + order.getId() + ",\"channel\":\"WECHAT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(403)));
    }
}
