package com.community.groupbuy.service;

import com.community.groupbuy.common.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * M7 缓存（C 同学）：通用 Cache-Aside 读取 + 分布式锁回源 + 随机 TTL
 *
 * 验收硬指标：热点 key 过期瞬间 100 并发请求，数据库回源不超过 1 次。
 * 做法：
 * 1) 先读 Redis，命中直接返回（100 个请求里 99 个走这里）；
 * 2) 未命中时抢分布式锁（SETNX + 过期时间），只有抢到的那个线程回源查库；
 * 3) 写缓存时 TTL = 基础 TTL + 随机偏移，避免同一批 key 同时过期造成雪崩；
 * 4) 未抢到锁的线程短暂轮询缓存（最多 500ms），等不到才降级自己查库，保证可用性；
 * 5) 全部 Redis 操作 fail-open：Redis 不可用直接查库，不阻断主流程。
 *
 * 锁释放使用 Lua 脚本比对 token，避免误删其他线程持有的锁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /** 释放锁 Lua：只有自己加的锁（token 匹配）才删除 */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private final DefaultRedisScript<Long> unlockScript = new DefaultRedisScript<>(UNLOCK_LUA, Long.class);

    /**
     * Cache-Aside 读取：命中则返回，未命中则加锁回源并回写缓存。
     *
     * @param key        缓存 key
     * @param type       缓存对象类型（JSON 反序列化用）
     * @param ttlSeconds 基础 TTL（秒），实际写入叠加随机偏移
     * @param loader     回源加载（通常是查库）
     */
    public <T> T getOrLoad(String key, Class<T> type, long ttlSeconds, Supplier<T> loader) {
        T cached = readCache(key, type);
        if (cached != null) {
            return cached;
        }

        String lockKey = Constants.REDIS_CACHE_LOCK_PREFIX + key;
        String token = UUID.randomUUID().toString();
        boolean locked = tryLock(lockKey, token);
        if (!locked) {
            T waited = waitForCache(key, type);
            if (waited != null) {
                return waited;
            }
            log.debug("等待缓存回源超时，降级直查数据库: key={}", key);
            return loader.get();
        }

        try {
            // 双检：抢到锁的线程可能已经回填过（前一个持锁者刚释放）
            T doubleCheck = readCache(key, type);
            if (doubleCheck != null) {
                return doubleCheck;
            }
            T value = loader.get();
            writeCache(key, value, ttlSeconds);
            return value;
        } finally {
            unlock(lockKey, token);
        }
    }

    /** 主动失效（数据变更后调用），失败只记日志 */
    public void evict(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("缓存失效失败（fail-open）key={}: {}", key, e.getMessage());
        }
    }

    private <T> T readCache(String key, Class<T> type) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("缓存读取失败（fail-open）key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, Object value, long ttlSeconds) {
        if (value == null) {
            return;
        }
        try {
            long ttl = ttlSeconds + ThreadLocalRandom.current().nextLong(0, Math.max(2, ttlSeconds / 5));
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value),
                    java.time.Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("缓存写入失败（fail-open）key={}: {}", key, e.getMessage());
        }
    }

    /** 抢锁：SETNX + 过期时间，redis 不可用时视为未抢到（走降级直查） */
    private boolean tryLock(String lockKey, String token) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, java.time.Duration.ofMillis(Constants.CACHE_LOCK_TTL_MILLIS));
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("获取缓存锁失败（降级直查）key={}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    /** 未抢到锁：轮询等待持锁线程回填缓存 */
    private <T> T waitForCache(String key, Class<T> type) {
        long deadline = System.currentTimeMillis() + Constants.CACHE_LOCK_WAIT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(Constants.CACHE_LOCK_SPIN_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            T cached = readCache(key, type);
            if (cached != null) {
                return cached;
            }
        }
        return null;
    }

    /** 释放锁：Lua 比对 token，避免误删他人锁 */
    private void unlock(String lockKey, String token) {
        try {
            stringRedisTemplate.execute(unlockScript, List.of(lockKey), token);
        } catch (Exception e) {
            log.warn("释放缓存锁失败 key={}: {}", lockKey, e.getMessage());
        }
    }
}
