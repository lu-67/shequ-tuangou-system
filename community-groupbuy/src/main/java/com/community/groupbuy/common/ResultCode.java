package com.community.groupbuy.common;

import lombok.Getter;

/**
 * 响应码常量（魔法值统一收敛于此）
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(500, "业务处理失败"),
    SYSTEM_ERROR(500, "系统异常"),

    // 业务细分
    USER_EXISTS(1001, "用户名已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "账号或密码错误"),
    ACTIVITY_NOT_FOUND(2001, "团购活动不存在"),
    ACTIVITY_NOT_ONGOING(2002, "活动未在进行中"),
    STOCK_NOT_ENOUGH(2003, "库存不足/已抢光"),
    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_STATUS_ERROR(3002, "订单状态异常"),
    PAY_FAILED(3003, "支付失败"),
    VERIFY_CODE_INVALID(4001, "核销码无效"),
    VERIFY_ALREADY_DONE(4002, "该订单已核销，不可重复核销"),
    RATE_LIMIT(5001, "请求过于频繁，请稍后再试"),
    ;

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
