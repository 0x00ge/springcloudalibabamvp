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
 * Kafka Consumer Group 核心概念
 * ============================================
 * - groupId 相同的多个消费者实例组成一个消费组
 * - 同一 Topic 的每个 partition，在同一时刻只分配给组内一个消费者
 * - 不同 groupId 彼此独立，各自收到完整消息流（发布订阅）
 * - 本 Demo：groupId = demo-consumer-group，订阅 demo-order-created
 *
 * ============================================
 * @KafkaListener 做了什么？
 * ============================================
 * - 启动后加入消费组，从 Broker 拉取消息
 * - 反序列化 key/value 后调用 onMessage
 * - 与 yml 中 listener.ack-mode=record 配合：方法正常结束 → 提交该条 offset
 *
 * ============================================
 * 重试和死信（本 Demo）
 * ============================================
 * 1. processOrder 或本方法抛异常
 * 2. DefaultErrorHandler（见 KafkaConfig）按 FixedBackOff 重试
 * 3. 仍失败 → 发到 demo-order-created.DLT
 * 4. DLQConsumer 消费 DLT 做告警日志
 *
 * 想本地验证 DLT：可在 processOrder 里临时 throw new RuntimeException("mock fail");
 *
 * ============================================
 * ConsumerRecord 常用字段
 * ============================================
 * topic / partition / offset / key / value / headers / timestamp
 */
@Slf4j
@Component
public class SimpleConsumer {

    /**
     * 消费「订单创建」Topic。
     *
     * <p>topics：订阅的 Topic 列表</p>
     * <p>groupId：消费组；改名后会作为新组从 auto-offset-reset 策略重新消费</p>
     *
     * @param record 原始记录，含元数据与反序列化后的 OrderDTO
     */
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
            // 模拟业务处理；抛异常会触发错误处理器重试 / DLT
            processOrder(order);
            log.info("<<< kafkaHandleSuccess : orderId={}", order.getOrderId());

        } catch (Exception e) {
            // 必须继续抛出（或抛出包装异常），否则 Spring Kafka 认为消费成功并提交 offset
            log.error("kafkaHandleFail : orderId={}", order.getOrderId(), e);
            throw new RuntimeException("Kafka 消息处理失败", e);
        }
    }

    /**
     * 模拟业务处理：发通知、改库存、写日志等。
     *
     * <p>Demo 仅 sleep 100ms 打日志；生产应保证幂等（同一 offset/业务 ID 重复消费安全）。</p>
     */
    private void processOrder(OrderDTO order) {
        try {
            // 模拟 IO / RPC 耗时
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // 恢复中断标志，避免吞掉中断
            Thread.currentThread().interrupt();
        }

        log.info("<<< kafkaHandling : 发送通知、更新库存、记录日志 orderId={}", order.getOrderId());
    }
}
