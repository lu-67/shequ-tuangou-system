package com.community.groupbuy.service;

import com.community.groupbuy.common.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * M7 库存预热（C 同学）
 *
 * M3 下单链路（OrderService）只有在 stockKey 存在时才做 Redis 预扣：
 *   hasKey(stockKey) → increment(stockKey, -qty)
 * 所以活动进入 ONGOING 时必须先把 DB 的 stock 写入该 key，否则预扣形同虚设。
 *
 * 预热点（由 ActivityService 调用，各一行）：
 * - 创建活动（初始即 ONGOING 时）
 * - 状态流转到 ONGOING
 * - 状态流转到 SUCCESS / FAILED，或成团判定成功 → 删除 key（活动已结束，无需预扣）
 *
 * 全部 fail-open：Redis 不可用只打日志，不影响活动创建/状态流转主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 预热：把活动当前库存写入 Redis（与 OrderService 的 key 完全一致） */
    public void preheat(Long activityId, Integer stock) {
        if (activityId == null || stock == null) {
            return;
        }
        String key = Constants.REDIS_STOCK_PREFIX + activityId;
        try {
            stringRedisTemplate.opsForValue().set(key, String.valueOf(stock));
            log.info("库存预热成功: activityId={}, stock={}, key={}", activityId, stock, key);
        } catch (Exception e) {
            log.warn("库存预热失败（fail-open，不影响主流程）activityId={}: {}", activityId, e.getMessage());
        }
    }

    /** 清除预热：活动结束（成团/失败）后调用 */
    public void evict(Long activityId) {
        if (activityId == null) {
            return;
        }
        String key = Constants.REDIS_STOCK_PREFIX + activityId;
        try {
            stringRedisTemplate.delete(key);
            log.info("库存缓存已清除: activityId={}, key={}", activityId, key);
        } catch (Exception e) {
            log.warn("库存缓存清除失败（fail-open）activityId={}: {}", activityId, e.getMessage());
        }
    }

    /** 读取当前缓存库存，未预热或 Redis 不可用返回 null */
    public Integer getCachedStock(Long activityId) {
        try {
            String value = stringRedisTemplate.opsForValue().get(Constants.REDIS_STOCK_PREFIX + activityId);
            return value == null ? null : Integer.valueOf(value);
        } catch (Exception e) {
            log.warn("读取库存缓存失败: activityId={}: {}", activityId, e.getMessage());
            return null;
        }
    }
}
