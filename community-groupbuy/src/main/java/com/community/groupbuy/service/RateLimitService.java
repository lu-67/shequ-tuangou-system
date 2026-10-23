package com.community.groupbuy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * M7 限流：基于 Lua 令牌桶的分布式限流。
 * Redis 不可用时自动放行（fail-open）。
 */
@Slf4j
@Service
public class RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;

    /** Lua 令牌桶脚本：原子地补充令牌并扣减，返回 1=放行 0=限流 */
    private static final String TOKEN_BUCKET_LUA =
            "local key = KEYS[1] "
            + "local capacity = tonumber(ARGV[1]) "
            + "local refill = tonumber(ARGV[2]) "
            + "local period = tonumber(ARGV[3]) "
            + "local now = tonumber(ARGV[4]) "
            + "local data = redis.call('HMGET', key, 'tokens', 'ts') "
            + "local tokens = tonumber(data[1]) "
            + "local ts = tonumber(data[2]) "
            + "if tokens == nil then tokens = capacity; ts = now end "
            + "local delta = math.floor((now - ts) / period) * refill "
            + "tokens = math.min(capacity, tokens + delta) "
            + "local allowed = 0 "
            + "if tokens >= 1 then tokens = tokens - 1; allowed = 1 end "
            + "redis.call('HMSET', key, 'tokens', tokens, 'ts', now) "
            + "redis.call('EXPIRE', key, period * 2 + 10) "
            + "return allowed ";

    private final DefaultRedisScript<Long> tokenBucketScript =
            new DefaultRedisScript<>(TOKEN_BUCKET_LUA, Long.class);

    public RateLimitService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 尝试获取令牌
     * @param key        限流键
     * @param capacity   桶容量
     * @param refill     每个周期补充的令牌数
     * @param periodSec  补充周期（秒）
     * @return true=放行
     */
    public boolean tryAcquire(String key, int capacity, int refill, int periodSec) {
        try {
            long nowSec = System.currentTimeMillis() / 1000L;
            Long result = stringRedisTemplate.execute(tokenBucketScript,
                    List.of(key),
                    String.valueOf(capacity),
                    String.valueOf(refill),
                    String.valueOf(periodSec),
                    String.valueOf(nowSec));
            return Long.valueOf(1L).equals(result);
        } catch (Exception e) {
            log.warn("限流脚本执行失败，自动放行 key={}: {}", key, e.getMessage());
            return true;
        }
    }
}
