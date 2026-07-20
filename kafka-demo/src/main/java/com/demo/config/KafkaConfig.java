package com.demo.config;

import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka 配置类：Topic 声明 + 消费失败重试与死信
 *
 * ============================================
 * 本类解决什么问题？
 * ============================================
 * 1. 声明业务 Topic 与 DLT Topic（启动时由 KafkaAdmin 自动创建）
 * 2. 配置 DefaultErrorHandler：消费抛异常 → 重试 → 仍失败则进死信 Topic
 *
 * ============================================
 * RocketMQ Demo 到 Kafka Demo 的概念映射
 * ============================================
 * | RocketMQ              | Kafka / Spring Kafka              |
 * |-----------------------|-----------------------------------|
 * | Topic + Tag           | 独立 Topic 区分事件类型             |
 * | 顺序消息 hashKey       | 消息 key → 同一 partition 内有序    |
 * | 消费失败重试 + DLQ     | DefaultErrorHandler + xxx.DLT     |
 * | RocketMQTemplate      | KafkaTemplate                     |
 * | @RocketMQMessageListener | @KafkaListener                 |
 *
 * ============================================
 * Topic 设计（本 Demo）
 * ============================================
 * - demo-order-created          简单创建订单事件
 * - demo-order-status-changed   订单状态变更（演示 key 顺序）
 * - 上述两者 + ".DLT"            对应死信 Topic
 *
 * partitions=3：便于观察 key 路由到不同分区；本地 replicas=1（单节点无法多副本）
 *
 * ============================================
 * 注意
 * ============================================
 * - application.yml 中 spring.kafka.admin.auto-create=true 时，NewTopic Bean 会在启动时创建
 * - 生产环境 Topic 通常由运维/平台预先创建，并单独配置副本数、保留时间、ACL
 */
@Slf4j
@Configuration
public class KafkaConfig {

    /** 简单消息 Topic：创建订单事件（无严格业务顺序要求） */
    public static final String ORDER_CREATED_TOPIC = "demo-order-created";

    /** 顺序消息 Topic：订单状态变更（同一 orderId 作为 key，保证同分区有序） */
    public static final String ORDER_STATUS_CHANGED_TOPIC = "demo-order-status-changed";

    /**
     * 死信 Topic 命名约定：原 Topic 名 + ".DLT"
     * 与 DeadLetterPublishingRecoverer 默认后缀一致，方便排查
     */
    public static final String ORDER_CREATED_DLT_TOPIC = ORDER_CREATED_TOPIC + ".DLT";
    public static final String ORDER_STATUS_CHANGED_DLT_TOPIC = ORDER_STATUS_CHANGED_TOPIC + ".DLT";

    /**
     * 创建「订单创建」业务 Topic。
     *
     * <p>partitions=3：多分区提高并行度；同一 key 仍会落到同一分区。</p>
     * <p>replicas=1：本地单 Broker 只能设 1；生产至少 2～3。</p>
     */
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * 创建「订单状态变更」Topic，供顺序消息演示使用。
     */
    @Bean
    public NewTopic orderStatusChangedTopic() {
        return TopicBuilder.name(ORDER_STATUS_CHANGED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * 创建订单创建事件的死信 Topic。
     *
     * <p>分区数与业务 Topic 对齐，DeadLetterPublishingRecoverer 会尽量保持原 partition，
     * 便于按分区对照排查。</p>
     */
    @Bean
    public NewTopic orderCreatedDltTopic() {
        return TopicBuilder.name(ORDER_CREATED_DLT_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * 创建订单状态变更事件的死信 Topic。
     */
    @Bean
    public NewTopic orderStatusChangedDltTopic() {
        return TopicBuilder.name(ORDER_STATUS_CHANGED_DLT_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * 全局消费错误处理器（Spring Kafka 监听容器会自动装配同名/该类型的 ErrorHandler）。
     *
     * ============================================
     * 失败处理链路
     * ============================================
     * 1. @KafkaListener 方法抛出异常
     * 2. DefaultErrorHandler 按 FixedBackOff 重试：间隔 1s，最多 3 次
     * 3. 仍失败 → DeadLetterPublishingRecoverer 把原消息发到「原 topic + .DLT」
     * 4. DLQConsumer 消费 DLT，做告警/落库/人工补偿（本 Demo 仅打日志）
     *
     * ============================================
     * FixedBackOff(1000L, 3L) 含义
     * ============================================
     * - 第一次失败后等待 1000ms 再投递
     * - maxAttempts=3 表示额外重试 3 次（加上首次消费，共最多 4 次处理尝试）
     * - 具体次数语义以 Spring Kafka 当前版本文档为准，调参时建议打日志验证
     *
     * ============================================
     * 与 application.yml 的配合
     * ============================================
     * - enable-auto-commit: false + ack-mode: record
     *   → 方法正常返回才提交 offset；抛异常不提交，才会进入错误处理器重试逻辑
     *
     * @param kafkaTemplate 用于把失败消息重新 publish 到 DLT（value 类型与业务一致 OrderDTO）
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, OrderDTO> kafkaTemplate) {
        // Recoverer：重试耗尽后的「最终去向」——发到死信 Topic
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            // 自定义目标分区：保持与原消息相同的 partition，后缀 .DLT
            (record, ex) -> {
                log.error("消息进入 Kafka DLT topic={} key={} offset={}",
                    record.topic(), record.key(), record.offset(), ex);
                return new TopicPartition(record.topic() + ".DLT", record.partition());
            }
        );

        // 间隔 1 秒，最多再重试 3 次；仍失败则交给 recoverer
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
