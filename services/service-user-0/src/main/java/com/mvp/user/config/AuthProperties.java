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
}
