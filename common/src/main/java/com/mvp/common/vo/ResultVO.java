package com.mvp.common.vo;

import com.mvp.common.enums.ResultCode;
import lombok.Data;

/**
 * 统一返回结果
 *
 * @Author zt
 */
@Data
public class ResultVO<T> {

    /** 状态码 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 时间戳 */
    private Long timestamp;

    private ResultVO() {
        this.timestamp = System.currentTimeMillis();
    }

    private ResultVO(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== 成功 ====================

    public static <T> ResultVO<T> ok() {
        return new ResultVO<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> ResultVO<T> ok(T data) {
        return new ResultVO<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> ResultVO<T> ok(String message, T data) {
        return new ResultVO<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    // ==================== 失败 ====================

    public static <T> ResultVO<T> fail() {
        return new ResultVO<>(ResultCode.FAIL.getCode(), ResultCode.FAIL.getMessage(), null);
    }

    public static <T> ResultVO<T> fail(String message) {
        return new ResultVO<>(ResultCode.FAIL.getCode(), message, null);
    }

    public static <T> ResultVO<T> fail(Integer code, String message) {
        return new ResultVO<>(code, message, null);
    }

    // ==================== 带状态码 ====================

    public static <T> ResultVO<T> build(ResultCode resultCode) {
        return new ResultVO<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> ResultVO<T> build(ResultCode resultCode, T data) {
        return new ResultVO<>(resultCode.getCode(), resultCode.getMessage(), data);
    }

    // ==================== 便捷判断 ====================

    public boolean isSuccess() {
        return this.code != null && this.code.equals(ResultCode.SUCCESS.getCode());
    }
}
