package com.community.groupbuy.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 统一响应结构: { code, message, data }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码: 200 成功, 其余为业务/错误码 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static Result<Void> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 仅带提示信息、无业务数据的成功响应 */
    public static Result<Void> success(String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.BUSINESS_ERROR.getCode(), message, null);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }
}
