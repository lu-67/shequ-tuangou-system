package com.community.groupbuy.service;

import com.community.groupbuy.common.BusinessException;
import com.community.groupbuy.common.Constants;
import com.community.groupbuy.common.ResultCode;
import com.community.groupbuy.dto.CreateOrderDTO;
import com.community.groupbuy.dto.OrderQueryDTO;
import com.community.groupbuy.entity.GroupActivity;
import com.community.groupbuy.entity.GroupOrder;
import com.community.groupbuy.mapper.ActivityMapper;
import com.community.groupbuy.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * M3 参团下单（核心并发）：Redis 预扣 + DB CAS + Lua 限流，跨表写事务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ActivityMapper activityMapper;
    private final OrderMapper orderMapper;
    private final RateLimitService rateLimitService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.rate-limit.capacity:200}")
    private int rateCapacity;
    @Value("${app.rate-limit.refill-tokens:50}")
    private int rateRefill;
    @Value("${app.rate-limit.refill-seconds:1}")
    private int ratePeriod;

    /**
     * 参团下单：限流 -> 活动校验 -> Redis 预扣(可选) -> DB CAS 扣库存 -> 落订单。
     * 整个库存扣减与订单写入在同一事务内，保证不超卖。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, CreateOrderDTO dto) {
        // 1. 令牌桶限流（Lua）
        String rateKey = Constants.REDIS_RATE_LIMIT_PREFIX + "create";
        if (!rateLimitService.tryAcquire(rateKey, rateCapacity, rateRefill, ratePeriod)) {
            throw new BusinessException(ResultCode.RATE_LIMIT);
        }

        // 2. 活动校验
        GroupActivity activity = activityMapper.selectById(dto.getActivityId());
        if (activity == null) {
            throw new BusinessException(ResultCode.ACTIVITY_NOT_FOUND);
        }
        if (!Constants.ACTIVITY_ONGOING.equals(activity.getStatus())) {
            throw new BusinessException(ResultCode.ACTIVITY_NOT_ONGOING);
        }
        if (dto.getQuantity() == null || dto.getQuantity() < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "购买数量不合法");
        }
        if (dto.getQuantity() > activity.getPerLimit()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "超过每人限购数量");
        }

        // 3. Redis 预扣（Cache-Aside：仅当库存已预热时生效；Redis 不可用时自动降级，直接走 DB CAS）
        String stockKey = Constants.REDIS_STOCK_PREFIX + activity.getId();
        boolean redisPre = false;
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
                Long after = stringRedisTemplate.opsForValue().increment(stockKey, -dto.getQuantity());
                if (after == null || after < 0) {
                    if (after != null) {
                        stringRedisTemplate.opsForValue().increment(stockKey, dto.getQuantity());
                    }
                    throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
                }
                redisPre = true;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis 不可用：降级为纯 DB 扣减，核心流程不受影响
            log.warn("Redis 预扣不可用，降级为 DB 扣减: {}", e.getMessage());
        }

        // 4. DB CAS 扣库存（权威，保证不超卖）
        int rows = activityMapper.deductStockCAS(activity.getId(), dto.getQuantity());
        if (rows != 1) {
            if (redisPre) {
                restoreRedisStock(stockKey, dto.getQuantity());
            }
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }

        // 5. 落订单（UNPAID，待支付），并生成唯一核销码
        GroupOrder order = new GroupOrder();
        order.setOrderNo(generateOrderNo());
        order.setActivityId(activity.getId());
        order.setUserId(userId);
        order.setProductId(activity.getProductId());
        order.setQuantity(dto.getQuantity());
        order.setAmount(activity.getGroupPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
        order.setStatus(Constants.ORDER_UNPAID);
        orderMapper.insert(order);
        orderMapper.updateVerificationCode(order.getId(), "CG" + String.format("%08d", order.getId()));

        log.info("下单成功: orderId={}, activityId={}, qty={}, redisPre={}", order.getId(), activity.getId(), dto.getQuantity(), redisPre);
        return order.getId();
    }

    public GroupOrder getOrder(Long id) {
        GroupOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 我的订单（居民端）：按当前登录用户查询，倒序返回
     * 复用 selectByCondition 动态 SQL，仅限定 userId
     */
    public List<GroupOrder> listMyOrders(Long userId) {
        OrderQueryDTO query = new OrderQueryDTO();
        query.setUserId(userId);
        // 防御性拷贝：Mapper 返回的 List 可能不可变（如测试中的 List.of），拷贝后再排序
        List<GroupOrder> sorted = new ArrayList<>(orderMapper.selectByCondition(query));
        sorted.sort(Comparator.comparing(GroupOrder::getId).reversed());
        return sorted;
    }

    private String generateOrderNo() {
        long ts = System.currentTimeMillis();
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "NO" + ts + rand;
    }

    /** Redis 库存回补（失败仅记录，不影响主流程） */
    private void restoreRedisStock(String stockKey, int quantity) {
        try {
            stringRedisTemplate.opsForValue().increment(stockKey, quantity);
        } catch (Exception e) {
            log.warn("Redis 库存回补失败 key={}: {}", stockKey, e.getMessage());
        }
    }
}
