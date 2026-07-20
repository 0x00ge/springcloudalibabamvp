package com.demo.consumer;

import com.demo.config.KafkaConfig;
import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 死信 Topic 消费者（Dead Letter Topic，DLT）
 *
 * ============================================
 * 什么是 Kafka DLT？
 * ============================================
 * - 业务消费多次失败后，消息被转发到独立 Topic，避免堵死主消费进度
 * - Spring Kafka 常用命名：原 Topic + ".DLT"（本 Demo 与 KafkaConfig 一致）
 * - 对应 RocketMQ 的死信队列（DLQ）概念，实现机制不同、目的相同
 *
 * ============================================
 * 消息如何进入 DLT？
 * ============================================
 * 1. SimpleConsumer / OrderedConsumer 处理抛异常
 * 2. DefaultErrorHandler + FixedBackOff 重试耗尽
 * 3. DeadLetterPublishingRecoverer 将消息 publish 到 xxx.DLT
 * 4. 本类消费 DLT，做告警、落库、通知人工
 *
 * ============================================
 * DLT 消费者的职责（生产建议）
 * ============================================
 * 1. 结构化记录失败上下文（topic/partition/offset/key/payload）
 * 2. 告警（钉钉/企微/邮件/PagerDuty）
 * 3. 提供人工重试入口（修复数据后重新投递业务 Topic）
 * 4. 自身处理应尽量「不会再失败」；若 DLT 消费也失败，需监控积压
 *
 * ============================================
 * 本 Demo 的简化
 * ============================================
 * - 只打 error 日志与格式化告警文本
 * - 捕获异常不向上抛，避免 DLT 消息再次进入错误处理死循环（演示友好）
 * - 生产是否抛异常要按「DLT 是否允许丢失」严格设计
 */
@Slf4j
@Component
public class DLQConsumer {

    /**
     * 同时订阅两个业务 Topic 对应的 DLT。
     *
     * <p>使用独立 groupId：demo-dlq-consumer-group，与业务消费组隔离。</p>
     */
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
            // 演示：打印可读告警；生产可写 DB / 发通知
            logFailedMessage(order, record);
            log.error("<<< kafkaDLTRecorded : orderId={}", order.getOrderId());
        } catch (Exception e) {
            // DLT 处理失败只记日志，避免再次抛出导致二次死信风暴（Demo 策略）
            log.error("<<< kafkaDLTHandleError : orderId={}", order.getOrderId(), e);
        }
    }

    /**
     * 组装人工可读的告警正文。
     *
     * <p>包含定位三元组 topic + partition + offset，以及订单关键字段与处理建议。</p>
     */
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
            4. 排查后可通过管理后台手动重试（重新投递业务 Topic）
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
