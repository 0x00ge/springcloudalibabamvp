package com.mvp.user.config;

import com.mvp.common.enums.ResultCode;
import com.mvp.common.vo.ResultVO;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * user 服务统一异常处理。
 *
 * 认证接口里会主动抛出 IllegalArgumentException 表示验证码错误、refreshToken 缺失等可预期业务错误；
 * 这里统一转换成 ResultVO，避免 Spring MVC 把业务异常打印成完整 ERROR 堆栈。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务校验异常，例如 refreshToken Cookie 不存在、验证码错误、手机号已注册。
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResultVO<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResultVO.fail(ResultCode.PARAM_ERROR.getCode(), ex.getMessage());
    }

    /**
     * 处理 @RequestParam、@RequestHeader 等参数校验异常。
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResultVO<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        return ResultVO.fail(ResultCode.PARAM_ERROR.getCode(), ex.getMessage());
    }

    /**
     * 处理 @RequestBody DTO 字段校验异常。
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVO<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse(ResultCode.PARAM_ERROR.getMessage());

        return ResultVO.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理浏览器误用 GET 刷新 POST 接口这类请求方法错误。
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResultVO<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return ResultVO.fail(ResultCode.PARAM_ERROR.getCode(), "请求方法不支持");
    }

    /**
     * 兜底异常仍记录日志，方便排查真正的系统问题。
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(Exception.class)
    public ResultVO<Void> handleException(Exception ex) {
        log.error("user 服务异常", ex);

        return ResultVO.fail();
    }
}
