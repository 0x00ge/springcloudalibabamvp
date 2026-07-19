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
 * Kafka 配置类
 *
 * ============================================
 * RocketMQ Demo 到 Kafka Demo 的映射
 * ============================================
 * - RocketMQ topic + tag：Kafka 使用独立 topic 承载不同事件类型
 * - RocketMQ 顺序消息 hashKey：Kafka 使用消息 key 路由到同一 partition
 * - RocketMQ 死信队列：Spring Kafka 使用 .DLT topic 承接消费失败消息
 */
@Slf4j
@Configuration
public class KafkaConfig {

    public static final String ORDER_CREATED_TOPIC = "demo-order-created";
    public static final String ORDER_STATUS_CHANGED_TOPIC = "demo-order-status-changed";
    public static final String ORDER_CREATED_DLT_TOPIC = ORDER_CREATED_TOPIC + ".DLT";
    public static final String ORDER_STATUS_CHANGED_DLT_TOPIC = ORDER_STATUS_CHANGED_TOPIC + ".DLT";

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic orderStatusChangedTopic() {
        return TopicBuilder.name(ORDER_STATUS_CHANGED_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic orderCreatedDltTopic() {
        return TopicBuilder.name(ORDER_CREATED_DLT_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic orderStatusChangedDltTopic() {
        return TopicBuilder.name(ORDER_STATUS_CHANGED_DLT_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, OrderDTO> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> {
                log.error("消息进入 Kafka DLT topic={} key={} offset={}",
                    record.topic(), record.key(), record.offset(), ex);
                return new TopicPartition(record.topic() + ".DLT", record.partition());
            }
        );

        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
