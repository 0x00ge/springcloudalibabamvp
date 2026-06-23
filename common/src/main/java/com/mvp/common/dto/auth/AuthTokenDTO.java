package com.mvp.common.dto.auth;

import lombok.Data;

/**
 * 登录或刷新后的 token 返回值。
 */
@Data
public class AuthTokenDTO {

    /**
     * 请求头 token 类型。
     */
    private String tokenType = "Bearer";

    /**
     * 访问业务接口使用的短期 token。
     */
    private String accessToken;

    /**
     * accessToken 剩余有效期，单位秒。
     */
    private Long accessTokenExpiresIn;

    /**
     * 换取新 accessToken 使用的长期 token。
     *
     * <p>当前接口会把 refreshToken 写入 HttpOnly Cookie；Controller 写完 Cookie 后会把该字段置空，
     * 避免响应体把 refreshToken 暴露给前端 JS。</p>
     */
    private String refreshToken;

    /**
     * refreshToken 剩余有效期，单位秒。
     */
    private Long refreshTokenExpiresIn;
}
