package com.demo.config;

import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类（扩展点预留）。
 *
 * <p>VIP 通道说明：
 * <ul>
 *   <li>VIP 端口 = Broker listenPort - 2（如 10931 → 10929）</li>
 *   <li>rocketmq-spring-boot 2.3.x 的 {@code RocketMQProperties} <b>没有</b>
 *       {@code rocketmq.producer.vip-channel-enabled} / {@code rocketmq.consumer.vip-channel-enabled}，
 *       yml 写了会无法解析（IDE 告警或 ignore-unknown=false 时启动失败）</li>
 *   <li>客户端 5.3.x 默认 {@code vipChannelEnabled=false}，本地 Docker 一般无需再关</li>
 *   <li>若要显式控制：JVM 参数 {@code -Dcom.rocketmq.sendMessageWithVIPChannel=false}，
 *       或拿到 {@code DefaultMQProducer} 后 {@code setVipChannelEnabled(false)}</li>
 * </ul>
 */
@Configuration
public class RocketMQConfig {
    // 业务侧配置见 application.yml 的 rocketmq.*（仅 starter 支持的属性）
}
