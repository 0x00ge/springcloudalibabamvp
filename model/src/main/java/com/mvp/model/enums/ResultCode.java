package com.mvp.model.enums;

import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @Author zt
 */
@Getter
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "操作成功"),

    /** 失败 */
    FAIL(500, "操作失败"),

    /** 参数错误 */
    PARAM_ERROR(400, "参数错误"),

    /** 无权限 */
    FORBIDDEN(403, "没有访问权限"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 手机号已存在 */
    PHONE_EXIST(1001, "手机号已注册"),

    /** 邮箱已存在 */
    EMAIL_EXIST(1002, "邮箱已注册"),

    /** 用户不存在 */
    USER_NOT_FOUND(1003, "用户不存在"),

    /** 账号已禁用 */
    ACCOUNT_DISABLED(1005, "账号已被禁用");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
