package com.mvp.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 鉴权相关 Redis key 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /**
     * accessToken 登出黑名单 key 前缀。
     */
    private String blacklistKeyPrefix = "auth:blacklist:";

    /**
     * refreshToken 白名单 key 前缀。
     */
    private String refreshKeyPrefix = "auth:refresh:";

    /**
     * 注册短信验证码 key 前缀。
     */
    private String registerSmsCodeKeyPrefix = "auth:register:sms:";

    /**
     * 注册短信验证码有效期，单位秒。
     */
    private long registerSmsCodeSeconds = 300;

    /**
     * refreshToken Cookie 名称。
     */
    private String refreshTokenCookieName = "refreshToken";

    /**
     * refreshToken Cookie path。
     */
    private String refreshTokenCookiePath = "/";

    /**
     * refreshToken Cookie SameSite 策略。
     */
    private String refreshTokenCookieSameSite = "Lax";

    /**
     * refreshToken Cookie 是否只允许 HTTPS 传输。
     *
     * <p>本地开发使用 http://127.0.0.1 时必须为 false；生产环境 HTTPS 下建议改为 true。</p>
     */
    private boolean refreshTokenCookieSecure = false;
}
