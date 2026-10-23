package com.community.groupbuy.common;

import lombok.Getter;

/**
 * 业务异常: 携带响应码与提示，由全局异常处理器统一转换为 Result
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String detail) {
        super(detail);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
