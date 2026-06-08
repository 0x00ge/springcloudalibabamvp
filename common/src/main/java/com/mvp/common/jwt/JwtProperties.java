package com.mvp.common.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * common JWT 配置。
 *
 * <p>对应 application.yml 中的 jwt 配置段，供 JwtUtil 签发和校验 token。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * HMAC 签名密钥。
     *
     * <p>HS256 是对称签名算法，签发和验签使用同一个 secret。
     * 生产环境应放到 Nacos、环境变量或密钥管理系统里，不建议写死在代码仓库。</p>
     */
    private String secret;

    /**
     * accessToken 有效期，单位秒。
     */
    private long accessTokenSeconds = 30;

    /**
     * refreshToken 有效期，单位秒。
     */
    private long refreshTokenSeconds = 60;
}
