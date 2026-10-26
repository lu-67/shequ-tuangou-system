package com.community.groupbuy.service;

import com.community.groupbuy.entity.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * M7 验收证据：热点 key 过期瞬间 100 并发回源，数据库（loader）调用次数不得超过 1 次。
 *
 * 说明：
 * - 不启动 Spring 容器（避免依赖 MySQL），直接连本机 Redis 6379 构造 CacheService；
 * - 本机没起 Redis 时自动跳过（Assume），不影响其余用例与构建；
 * - 验收演示时启动 Redis 运行本用例，输出即为报告可引用的证据。
 */
class CacheServiceHotKeyConcurrencyTest {

    private static final int CONCURRENCY = 100;

    private static StringRedisTemplate redisTemplate;

    private static boolean redisAvailable;

    @BeforeAll
    static void connectRedis() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory("127.0.0.1", 6379);
        try {
            factory.afterPropertiesSet();
            factory.start();
            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.afterPropertiesSet();
            // ping 一下确认真的可用
            String pong = template.getConnectionFactory().getConnection().ping();
            redisAvailable = pong != null;
            redisTemplate = template;
        } catch (Exception e) {
            redisAvailable = false;
        }
    }

    private Product product() {
        Product p = new Product();
        p.setId(1L);
        p.setName("并发回源测试商品");
        p.setGroupPrice(new BigDecimal("4.90"));
        return p;
    }

    @Test
    void hotKeyExpired_hundredConcurrent_onlyOneDatabaseLoad() throws Exception {
        assumeTrue(redisAvailable, "本机未启动 Redis(6379)，跳过并发回源用例；验收环境请启动 Redis 后运行");
        CacheService cacheService = new CacheService(redisTemplate, new ObjectMapper());
        String key = "groupbuy:cache:product:hotkey-test:" + System.currentTimeMillis();
        redisTemplate.delete(key);

        AtomicInteger dbLoadCount = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(CONCURRENCY);

        for (int i = 0; i < CONCURRENCY; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    Product p = cacheService.getOrLoad(key, Product.class, 30L, () -> {
                        dbLoadCount.incrementAndGet();
                        return product();
                    });
                    assertThat(p.getId()).isEqualTo(1L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = doneGate.await(15, TimeUnit.SECONDS);
        pool.shutdownNow();
        redisTemplate.delete(key);

        assertThat(finished).as("100 并发必须在 15 秒内全部返回").isTrue();
        assertThat(dbLoadCount.get())
                .as("热点 key 过期瞬间 100 并发回源，数据库查询次数必须 ≤ 1")
                .isLessThanOrEqualTo(1);
    }

    @Test
    void warmKey_hundredConcurrent_noDatabaseLoad() throws Exception {
        assumeTrue(redisAvailable, "本机未启动 Redis(6379)，跳过并发回源用例；验收环境请启动 Redis 后运行");
        CacheService cacheService = new CacheService(redisTemplate, new ObjectMapper());
        String key = "groupbuy:cache:product:warmkey-test:" + System.currentTimeMillis();
        // 先写入缓存（预热）
        cacheService.getOrLoad(key, Product.class, 30L, this::product);

        AtomicInteger dbLoadCount = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(CONCURRENCY);

        for (int i = 0; i < CONCURRENCY; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    cacheService.getOrLoad(key, Product.class, 30L, () -> {
                        dbLoadCount.incrementAndGet();
                        return product();
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }
        startGate.countDown();
        doneGate.await(15, TimeUnit.SECONDS);
        pool.shutdownNow();
        redisTemplate.delete(key);

        assertThat(dbLoadCount.get()).as("命中缓存时不应回源数据库").isZero();
    }
}
