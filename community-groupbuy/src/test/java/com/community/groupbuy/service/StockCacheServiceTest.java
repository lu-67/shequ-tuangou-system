package com.community.groupbuy.service;

import com.community.groupbuy.common.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M7 库存预热 Service 单元测试（Mockito）
 *
 * 覆盖：预热写入正确的 key/value、活动结束清除、读取缓存、Redis 异常 fail-open。
 */
@ExtendWith(MockitoExtension.class)
class StockCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private StockCacheService stockCacheService;

    @BeforeEach
    void setUp() {
        stockCacheService = new StockCacheService(stringRedisTemplate);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    /** 预热：写入 groupbuy:stock:activity:{id} = 当前库存（与 OrderService 预扣 key 一致） */
    @Test
    void preheat_writesStockKey() {
        stockCacheService.preheat(7L, 1000);
        verify(valueOperations).set(Constants.REDIS_STOCK_PREFIX + 7L, "1000");
    }

    /** 活动结束（成团/失败）：删除预热 key */
    @Test
    void evict_deletesStockKey() {
        stockCacheService.evict(7L);
        verify(stringRedisTemplate).delete(Constants.REDIS_STOCK_PREFIX + 7L);
    }

    /** 读取缓存库存 */
    @Test
    void getCachedStock_returnsValue() {
        when(valueOperations.get(Constants.REDIS_STOCK_PREFIX + 7L)).thenReturn("998");
        assertThat(stockCacheService.getCachedStock(7L)).isEqualTo(998);
    }

    /** 未预热：返回 null（OrderService 据此跳过 Redis 预扣） */
    @Test
    void getCachedStock_notPreheated_returnsNull() {
        when(valueOperations.get(Constants.REDIS_STOCK_PREFIX + 7L)).thenReturn(null);
        assertThat(stockCacheService.getCachedStock(7L)).isNull();
    }

    /** Redis 不可用：预热 fail-open，不抛异常 */
    @Test
    void preheat_redisUnavailable_noException() {
        doThrow(new RuntimeException("redis down")).when(valueOperations).set(anyString(), eq("1000"));
        stockCacheService.preheat(7L, 1000);
        verify(valueOperations).set(Constants.REDIS_STOCK_PREFIX + 7L, "1000");
    }
}
