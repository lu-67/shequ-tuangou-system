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
}
