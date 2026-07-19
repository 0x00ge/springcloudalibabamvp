package com.demo.producer;

import com.demo.config.KafkaConfig;
import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 顺序消息生产者
 *
 * ============================================
 * Kafka 如何保证同一订单有序？
 * ============================================
 * - 使用 orderId 作为消息 key
 * - 相同 key 的消息会进入同一个 partition
 * - Kafka 在单个 partition 内保证消息顺序
 */
@Slf4j
@Component
public class OrderedProducer {

    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;

    public OrderedProducer(KafkaTemplate<String, OrderDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public KafkaSendResult sendOrderly(OrderDTO order, String key) {
        try {
            SendResult<String, OrderDTO> result = kafkaTemplate
                .send(KafkaConfig.ORDER_STATUS_CHANGED_TOPIC, key, order)
                .get(3, TimeUnit.SECONDS);

            log.info(">>> kafkaOrderlySendSuccess : orderId={} status={} key={} topic={} partition={} offset={}",
                order.getOrderId(),
                order.getStatus(),
                key,
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());

            return new KafkaSendResult(
                null,
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset()
            );

        } catch (Exception e) {
            log.error(">>> kafkaOrderlySendFail : orderId={}", order.getOrderId(), e);
            throw new RuntimeException("Kafka 顺序消息发送失败", e);
        }
    }
}
