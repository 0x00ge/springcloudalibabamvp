package com.demo.producer;

import com.demo.config.KafkaConfig;
import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 简单消息生产者
 *
 * ============================================
 * Kafka 中的简单消息
 * ============================================
 * - 发送到指定 topic
 * - 使用 key 参与分区路由
 * - Broker 确认写入后返回 RecordMetadata
 */
@Slf4j
@Component
public class SimpleProducer {

    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;

    public SimpleProducer(KafkaTemplate<String, OrderDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 同步发送（推荐）
     *
     * @param order 订单对象
     * @return KafkaSendResult 发送结果（包含 messageId、topic、partition、offset）
     */
    public KafkaSendResult sendSync(OrderDTO order) {
        String messageId = UUID.randomUUID().toString();

        try {
            SendResult<String, OrderDTO> result = kafkaTemplate.send(buildRecord(order, messageId))
                .get(3, TimeUnit.SECONDS);

            log.info(">>> kafkaSyncSendSuccess : orderId={} messageId={} topic={} partition={} offset={}",
                order.getOrderId(),
                messageId,
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());

            return new KafkaSendResult(
                messageId,
                result.getRecordMetadata().topic(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset()
            );

        } catch (Exception e) {
            log.error(">>> kafkaSyncSendFail : orderId={}", order.getOrderId(), e);
            throw new RuntimeException("Kafka 同步发送失败", e);
        }
    }

    /**
     * 异步发送
     *
     * @param order 订单对象
     */
    public void sendAsync(OrderDTO order) {
        String messageId = UUID.randomUUID().toString();
        CompletableFuture<SendResult<String, OrderDTO>> future = kafkaTemplate.send(buildRecord(order, messageId));

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info(">>> kafkaAsyncSendSuccess : orderId={} messageId={} topic={} partition={} offset={}",
                    order.getOrderId(),
                    messageId,
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
                return;
            }

            log.error(">>> kafkaAsyncSendFail : orderId={} messageId={}", order.getOrderId(), messageId, ex);
        });

        log.info(">>> kafkaAsyncSendCommit : orderId={} messageId={}", order.getOrderId(), messageId);
    }

    private ProducerRecord<String, OrderDTO> buildRecord(OrderDTO order, String messageId) {
        ProducerRecord<String, OrderDTO> record = new ProducerRecord<>(
            KafkaConfig.ORDER_CREATED_TOPIC,
            order.getOrderId(),
            order
        );
        record.headers().add(new RecordHeader("messageId", messageId.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("orderId", order.getOrderId().getBytes(StandardCharsets.UTF_8)));
        return record;
    }
}
