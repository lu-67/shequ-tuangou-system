package com.community.groupbuy.common;

/**
 * 全局常量（魔法值统一收敛）
 */
public final class Constants {

    private Constants() {
    }

    /** 角色 */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_HEAD = "HEAD";
    public static final String ROLE_RESIDENT = "RESIDENT";

    /** 活动状态机 */
    public static final String ACTIVITY_PENDING = "PENDING";     // 待开始
    public static final String ACTIVITY_ONGOING = "ONGOING";     // 进行中
    public static final String ACTIVITY_SUCCESS = "SUCCESS";     // 已成团
    public static final String ACTIVITY_FAILED = "FAILED";       // 已失败

    /** 订单状态 */
    public static final String ORDER_UNPAID = "UNPAID";          // 待支付
    public static final String ORDER_PAID = "PAID";              // 已支付
    public static final String ORDER_CANCELLED = "CANCELLED";    // 已取消
    public static final String ORDER_PICKED = "PICKED";          // 已核销

    /** 核销状态 */
    public static final String VERIFY_PENDING = "PENDING";
    public static final String VERIFY_DONE = "DONE";
    public static final String VERIFY_INVALID = "INVALID";

    /** 支付状态 */
    public static final String PAY_PENDING = "PENDING";
    public static final String PAY_SUCCESS = "SUCCESS";
    public static final String PAY_REFUNDED = "REFUNDED";

    /** 库存预热 Redis key 前缀 */
    public static final String REDIS_STOCK_PREFIX = "groupbuy:stock:activity:";
    /** 令牌桶限流 Redis key 前缀 */
    public static final String REDIS_RATE_LIMIT_PREFIX = "groupbuy:ratelimit:order:";
    /** 默认令牌桶容量 */
    public static final int RATE_LIMIT_CAPACITY = 200;

    /** 默认支付渠道（M4） */
    public static final String PAY_CHANNEL_WECHAT = "WECHAT";
    /** 支付流水号前缀（按订单维度固定：PAY{orderId}，保证重复支付命中同一笔流水） */
    public static final String PAY_NO_PREFIX = "PAY";

    /** M7（C 同学）缓存 key 前缀：商品详情 */
    public static final String REDIS_CACHE_PRODUCT_PREFIX = "groupbuy:cache:product:";
    /** M7 缓存 key 前缀：活动详情 */
    public static final String REDIS_CACHE_ACTIVITY_PREFIX = "groupbuy:cache:activity:";
    /** M7 缓存回源分布式锁前缀 */
    public static final String REDIS_CACHE_LOCK_PREFIX = "groupbuy:lock:cache:";
    /** M7 商品详情缓存基础 TTL（秒），实际写入会叠加随机值防雪崩 */
    public static final long CACHE_PRODUCT_TTL_SECONDS = 60L;
    /** M7 活动详情缓存基础 TTL（秒），活动库存变化频繁，TTL 取小值 */
    public static final long CACHE_ACTIVITY_TTL_SECONDS = 20L;
    /** M7 回源锁持有时长（毫秒），防止持锁线程崩溃导致死锁 */
    public static final long CACHE_LOCK_TTL_MILLIS = 3000L;
    /** M7 未抢到锁时的最长等待时间（毫秒），超时降级为直接查库 */
    public static final long CACHE_LOCK_WAIT_MILLIS = 500L;
    /** M7 未抢到锁时的轮询间隔（毫秒） */
    public static final long CACHE_LOCK_SPIN_MILLIS = 20L;
}
