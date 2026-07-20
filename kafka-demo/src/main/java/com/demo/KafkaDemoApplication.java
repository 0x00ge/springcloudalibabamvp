package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kafka Demo 启动类
 *
 * ============================================
 * 本模块在项目中的定位
 * ============================================
 * - 独立演示 Spring Kafka 的生产、消费、顺序消息和死信 Topic
 * - 与 rocketmq-demo 对照学习：业务事件可走 RocketMQ，本模块专门练 Kafka 客户端用法
 * - 不依赖 Nacos / 业务微服务，启动后直接调 HTTP 接口即可观察日志
 *
 * ============================================
 * 启动前准备
 * ============================================
 * 1. 先启动本地 Kafka（项目内 Docker）：
 *    cd deploy/docker/kafka-cluster
 *    docker compose -f docker-compose-kafka-cluster.yml up -d
 * 2. Broker 地址默认 127.0.0.1:9092（见 application.yml）
 * 3. 可选 UI：http://127.0.0.1:8083
 * 4. 启动本应用后访问：http://localhost:8091/order/
 *
 * ============================================
 * 版本约定（与父工程 Spring Boot 3.3.4 对齐）
 * ============================================
 * - spring-kafka：Boot 托管，约 3.2.4
 * - kafka-clients：Boot 托管，约 3.7.1
 * - Docker Broker：apache/kafka:3.8.0（KRaft 单节点）
 * - 客户端与 Broker 小版本不完全一致是正常的，协议兼容
 *
 * @see com.demo.config.KafkaConfig Topic 与错误处理器
 * @see com.demo.controller.OrderController HTTP 演示入口
 */
@SpringBootApplication
public class KafkaDemoApplication {

    public static void main(String[] args) {
        // 启动 Spring 容器：自动装配 KafkaTemplate、KafkaListener 容器、Admin 建 Topic 等
        SpringApplication.run(KafkaDemoApplication.class, args);
        System.out.println("\n=================================");
        System.out.println("Kafka Demo 启动成功！");
        System.out.println("访问: http://localhost:8091/order/");
        System.out.println("Kafka UI: http://127.0.0.1:8083");
        System.out.println("=================================\n");
    }
}
