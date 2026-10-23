package com.community.groupbuy.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：统一把异常转换为 Result 结构
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex) {
        log.warn("业务异常: code={}, msg={}", ex.getCode(), ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    /** 参数校验失败 (@Valid) */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValid(Exception ex) {
        String msg = "参数校验失败";
        if (ex instanceof MethodArgumentNotValidException m) {
            FieldError fe = m.getBindingResult().getFieldError();
            if (fe != null) msg = fe.getDefaultMessage();
        } else if (ex instanceof BindException b) {
            FieldError fe = b.getBindingResult().getFieldError();
            if (fe != null) msg = fe.getDefaultMessage();
        }
        log.warn("参数校验失败: {}", msg);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /** 请求体解析失败（JSON 格式错误、日期格式不匹配等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(),
                "请求参数格式错误，日期时间请使用 yyyy-MM-dd HH:mm:ss 格式");
    }

    /** 其余未捕获异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleOther(Exception ex, HttpServletRequest request) {
        log.error("系统异常 uri={}", request.getRequestURI(), ex);
        return Result.error(ResultCode.SYSTEM_ERROR);
    }
}
