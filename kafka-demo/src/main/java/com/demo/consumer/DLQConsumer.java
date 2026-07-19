package com.demo.consumer;

import com.demo.config.KafkaConfig;
import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者
 *
 * ============================================
 * Kafka DLT（Dead Letter Topic）
 * ============================================
 * - Spring Kafka 捕获消费失败
 * - 达到重试上限后，将消息投递到原 topic + ".DLT"
 * - DLT 消费者负责记录、告警、人工补偿
 */
@Slf4j
@Component
public class DLQConsumer {

    @KafkaListener(
        topics = {
            KafkaConfig.ORDER_CREATED_DLT_TOPIC,
            KafkaConfig.ORDER_STATUS_CHANGED_DLT_TOPIC
        },
        groupId = "demo-dlq-consumer-group"
    )
    public void onMessage(ConsumerRecord<String, OrderDTO> record) {
        OrderDTO order = record.value();
        log.error("<<< kafkaDLTMessage : orderId={} key={} topic={} partition={} offset={}",
            order.getOrderId(),
            record.key(),
            record.topic(),
            record.partition(),
            record.offset());

        try {
            logFailedMessage(order, record);
            log.error("<<< kafkaDLTRecorded : orderId={}", order.getOrderId());
        } catch (Exception e) {
            log.error("<<< kafkaDLTHandleError : orderId={}", order.getOrderId(), e);
        }
    }

    private void logFailedMessage(OrderDTO order, ConsumerRecord<String, OrderDTO> record) {
        String alertMessage = String.format("""

            ========================================
            Kafka 死信 Topic 告警
            ========================================
            Topic：%s
            Partition：%d
            Offset：%d
            Key：%s
            订单ID：%s
            用户ID：%s
            商品：%s
            数量：%s
            金额：%s
            状态：%s

            原因：消费失败，达到重试上限后进入 DLT

            处理建议：
            1. 检查消费者代码是否有 BUG
            2. 检查数据库/Redis 是否正常
            3. 检查消息内容是否合法
            4. 排查后可通过管理后台手动重试
            ========================================

            """,
            record.topic(),
            record.partition(),
            record.offset(),
            record.key(),
            order.getOrderId(),
            order.getUserId(),
            order.getProductName(),
            order.getQuantity(),
            order.getAmount(),
            order.getStatus()
        );

        log.error(alertMessage);
    }
}
