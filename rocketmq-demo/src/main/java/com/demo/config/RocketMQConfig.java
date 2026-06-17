package com.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类
 *
 * ============================================
 * 解决问题：禁用 VIP 通道
 * ============================================
 * - RocketMQ 默认开启 VIP 通道
 * - VIP 通道端口 = Broker端口 - 2
 * - 例如：Broker 10931，VIP 通道 10929
 * - Docker 未映射 VIP 端口会导致连接失败
 *
 * ============================================
 * 什么是 VIP 通道？
 * ============================================
 * - 高优先级通道，用于重要消息
 * - 生产环境可以开启（需要额外端口映射）
 * - 开发环境建议关闭（简化配置）
 *
 * ============================================
 * 解决方案
 * ============================================
 * 通过 application.yml 配置禁用 VIP 通道：
 * rocketmq:
 *   producer:
 *     vip-channel-enabled: false
 *   consumer:
 *     vip-channel-enabled: false
 */
@Configuration
public class RocketMQConfig {
    // 配置已在 application.yml 中设置
    // 此类保留用于将来扩展其他配置
}
