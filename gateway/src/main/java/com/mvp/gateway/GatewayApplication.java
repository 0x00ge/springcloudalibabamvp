package com.mvp.gateway;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * gateway 模块启动入口。
 *
 * <p>gateway 是整个后端的统一入口，主要负责：
 * 1. 从 Nacos 发现下游服务；
 * 2. 根据路由规则把请求转发到下游服务。</p>
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.mvp.gateway", "com.mvp.common"})
public class GatewayApplication {
    public static void main(String[] args) {
        // 启动 Spring Boot 应用，同时加载 gateway 路由、Nacos 和 JWT 工具配置。
        org.springframework.boot.SpringApplication.run(GatewayApplication.class, args);
    }
}
