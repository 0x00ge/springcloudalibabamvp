package com.mvp.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Gateway 鉴权配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /**
     * 不需要 accessToken 的路径。
     */
    private List<String> whitelist = new ArrayList<>();

    /**
     * accessToken 登出黑名单 key 前缀。
     */
    private String blacklistKeyPrefix = "auth:blacklist:";
}
