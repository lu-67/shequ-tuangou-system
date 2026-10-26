package com.community.groupbuy.service;

import com.community.groupbuy.common.Constants;
import com.community.groupbuy.entity.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M7 缓存 Service 单元测试（Mockito，不依赖真实 Redis）
 *
 * 覆盖：命中直接返回、未命中抢锁回源并回写（随机 TTL）、未抢到锁等待缓存、
 *       等待超时降级直查、Redis 异常 fail-open、缓存失效。
 */
@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    private static final String KEY = "groupbuy:cache:product:1";
    private static final String JSON = "{\"id\":1,\"name\":\"有机小白菜\",\"groupPrice\":4.90}";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cacheService = new CacheService(stringRedisTemplate, objectMapper);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private Product product() {
        Product p = new Product();
        p.setId(1L);
        p.setName("有机小白菜");
        p.setGroupPrice(new BigDecimal("4.90"));
        return p;
    }

    /** 命中缓存：不查库、不加锁 */
    @Test
    void getOrLoad_cacheHit_skipsLoader() {
        when(valueOperations.get(KEY)).thenReturn(JSON);
        AtomicInteger loadCount = new AtomicInteger();

        Product result = cacheService.getOrLoad(KEY, Product.class, 60L, () -> {
            loadCount.incrementAndGet();
            return product();
        });

        assertThat(result.getName()).isEqualTo("有机小白菜");
        assertThat(loadCount.get()).isZero();
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    /** 未命中且抢到锁：查库一次并回写缓存，TTL 带随机偏移 */
    @Test
    void getOrLoad_cacheMiss_lockAcquired_loadsOnceAndWrites() {
        when(valueOperations.get(KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        AtomicInteger loadCount = new AtomicInteger();

        Product result = cacheService.getOrLoad(KEY, Product.class, 60L, () -> {
            loadCount.incrementAndGet();
            return product();
        });

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(loadCount.get()).isEqualTo(1);
        verify(valueOperations).set(eq(KEY), anyString(), any(Duration.class));
    }

    /** 未抢到锁：等待持锁线程回填后读缓存，自己不查库 */
    @Test
    void getOrLoad_lockNotAcquired_waitsAndReadsCache() {
        when(valueOperations.get(KEY)).thenReturn(null, JSON);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        AtomicInteger loadCount = new AtomicInteger();

        Product result = cacheService.getOrLoad(KEY, Product.class, 60L, () -> {
            loadCount.incrementAndGet();
            return product();
        });

        assertThat(result.getName()).isEqualTo("有机小白菜");
        assertThat(loadCount.get()).isZero();
    }

    /** 未抢到锁且等待超时：降级自己查库，保证接口可用 */
    @Test
    void getOrLoad_lockNotAcquired_waitTimeout_fallsBackToLoader() {
        when(valueOperations.get(KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        AtomicInteger loadCount = new AtomicInteger();

        Product result = cacheService.getOrLoad(KEY, Product.class, 20L, () -> {
            loadCount.incrementAndGet();
            return product();
        });

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(loadCount.get()).isEqualTo(1);
    }

    /** Redis 异常：fail-open，直接查库不阻断业务 */
    @Test
    void getOrLoad_redisUnavailable_failsOpen() {
        when(valueOperations.get(KEY)).thenThrow(new RuntimeException("redis down"));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("redis down"));

        Product result = cacheService.getOrLoad(KEY, Product.class, 20L, this::product);

        assertThat(result.getId()).isEqualTo(1L);
    }

    /** 缓存失效：删除 key */
    @Test
    void evict_deletesKey() {
        cacheService.evict(KEY);
        verify(stringRedisTemplate).delete(KEY);
    }

    /** 缓存失效失败：只记日志不抛异常 */
    @Test
    void evict_redisUnavailable_noException() {
        when(stringRedisTemplate.delete(KEY)).thenThrow(new RuntimeException("redis down"));
        cacheService.evict(KEY);
        verify(stringRedisTemplate).delete(KEY);
    }

    /** 释放锁使用 Lua 脚本（比对 token 后删除） */
    @Test
    void getOrLoad_lockAcquired_releasesLockWithLua() {
        when(valueOperations.get(KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        cacheService.getOrLoad(KEY, Product.class, 10L, this::product);

        verify(stringRedisTemplate).execute(any(RedisScript.class), eq(List.of(Constants.REDIS_CACHE_LOCK_PREFIX + KEY)),
                any(Object[].class));
    }
}
