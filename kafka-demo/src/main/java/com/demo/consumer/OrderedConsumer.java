package com.demo.consumer;

import com.demo.config.KafkaConfig;
import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 顺序消息消费者
 *
 * ============================================
 * 和 SimpleConsumer 的差异
 * ============================================
 * - 订阅 Topic：demo-order-status-changed（状态机事件）
 * - 消费组：demo-status-consumer-group（与简单消息隔离，互不影响 offset）
 * - 依赖生产者用 orderId 作 key，保证同一订单落在同一 partition
 *
 * ============================================
 * 如何验证「有序」？
 * ============================================
 * 1. 对同一 ORDER_ID 连续调用：
 *    CREATED → PAID → SHIPPED → COMPLETED
 * 2. 观察日志中 partition 是否始终相同
 * 3. 观察 offset 是否递增，status 顺序是否与发送一致
 *
 * ============================================
 * 什么会破坏顺序？
 * ============================================
 * - 发送时 key 不一致（有时用 orderId，有时用 null）
 * - 消费方法内再丢线程池并行处理同一分区消息且乱序写库
 * - 多实例消费组 + 业务侧未按分区串行（Kafka 已按分区分配，单 listener 默认串行处理其分配到的分区）
 *
 * ============================================
 * 失败与 DLT
 * ============================================
 * 与简单消费相同：抛异常 → DefaultErrorHandler 重试 → demo-order-status-changed.DLT
 */
@Slf4j
@Component
public class OrderedConsumer {

    /**
     * 消费订单状态变更消息。
     *
     * <p>打印 key/partition/offset，便于对照生产者顺序发送日志。</p>
     */
    @KafkaListener(
        topics = KafkaConfig.ORDER_STATUS_CHANGED_TOPIC,
        groupId = "demo-status-consumer-group"
    )
    public void onMessage(ConsumerRecord<String, OrderDTO> record) {
        OrderDTO order = record.value();
        // key 应为 orderId；partition 对同一 key 应固定
        log.info("<<< kafkaOrderlyConsume : orderId={} status={} key={} partition={} offset={}",
            order.getOrderId(),
            order.getStatus(),
            record.key(),
            record.partition(),
            record.offset());

        // 模拟按序更新 DB 状态；真实系统应校验状态机合法性（例如禁止 COMPLETED → PAID）
        updateOrderStatus(order);

        log.info("<<< kafkaOrderStatusUpdated : orderId={} newStatus={}",
            order.getOrderId(), order.getStatus());
    }

    /**
     * 模拟落库更新订单状态。
     *
     * <p>生产环境建议：乐观锁 / 状态机版本号，防止乱序或重复消费写坏状态。</p>
     */
    private void updateOrderStatus(OrderDTO order) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("<<< kafkaUpdateDatabase : orderId={} status={}",
            order.getOrderId(), order.getStatus());
    }
}
