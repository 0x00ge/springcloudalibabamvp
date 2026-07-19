package com.demo.consumer;

import com.demo.config.KafkaConfig;
import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 简单消息消费者
 *
 * ============================================
 * Kafka Consumer Group
 * ============================================
 * - 同一个 group 内的消费者共同消费 topic
 * - 每个 partition 同一时刻只会分配给 group 内的一个消费者
 * - 不同 group 会各自收到一份完整消息流
 *
 * ============================================
 * 重试和死信
 * ============================================
 * - 本 demo 在 KafkaConfig 中配置 DefaultErrorHandler
 * - 消费异常后重试 3 次
 * - 仍失败则投递到原 topic + ".DLT"
 */
@Slf4j
@Component
public class SimpleConsumer {

    @KafkaListener(
        topics = KafkaConfig.ORDER_CREATED_TOPIC,
        groupId = "demo-consumer-group"
    )
    public void onMessage(ConsumerRecord<String, OrderDTO> record) {
        OrderDTO order = record.value();
        log.info("<<< getKafkaOrderMessage : orderId={} product={} quantity={} amount={} topic={} partition={} offset={}",
            order.getOrderId(),
            order.getProductName(),
            order.getQuantity(),
            order.getAmount(),
            record.topic(),
            record.partition(),
            record.offset());

        try {
            log.info("<<< kafkaHandleStart : orderId={}", order.getOrderId());
            processOrder(order);
            log.info("<<< kafkaHandleSuccess : orderId={}", order.getOrderId());

        } catch (Exception e) {
            log.error("kafkaHandleFail : orderId={}", order.getOrderId(), e);
            throw new RuntimeException("Kafka 消息处理失败", e);
        }
    }

    private void processOrder(OrderDTO order) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("<<< kafkaHandling : 发送通知、更新库存、记录日志");
    }
}
