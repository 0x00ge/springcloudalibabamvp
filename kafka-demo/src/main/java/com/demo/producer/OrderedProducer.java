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
 * Kafka 如何保证「同一订单」有序？
 * ============================================
 * Kafka 只保证【单个 partition 内】的消息有序，不保证跨分区全局有序。
 *
 * 做法：
 * 1. 发送时指定 key（本 Demo 用 orderId）
 * 2. 相同 key → 相同 partition
 * 3. 同一 partition 内按 offset 递增顺序被消费
 *
 * 因此：同一 orderId 的 CREATED → PAID → SHIPPED → COMPLETED
 * 会进入同一分区，在单线程/按序处理的消费者下可按发送顺序处理。
 *
 * ============================================
 * 和 RocketMQ 顺序消息的对比
 * ============================================
 * | 点           | RocketMQ                    | Kafka                          |
 * |--------------|-----------------------------|--------------------------------|
 * | 顺序粒度     | 队列（MessageQueue）          | 分区（Partition）                |
 * | 路由键       | hashKey / sharding key      | message key                    |
 * | 全局有序     | 单队列可近似                  | 单分区；多分区无法全局有序         |
 * | 吞吐 vs 顺序 | 队列数折中                    | 分区数折中：分区多吞吐高、顺序域变碎 |
 *
 * ============================================
 * 使用注意
 * ============================================
 * 1. key 必须稳定：同一业务实体始终用同一 key，否则顺序被打散
 * 2. 消费者侧：同一分区通常由 group 内一个消费者处理；若业务多线程乱序处理，分区有序也会被破坏
 * 3. 生产者若开启过多 in-flight 且关闭幂等，极端情况下可能影响可见顺序；本 Demo yml 已开幂等
 * 4. 本方法同步 get 等待发送完成，便于演示时立刻在响应里看到 partition/offset
 */
@Slf4j
@Component
public class OrderedProducer {

    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;

    public OrderedProducer(KafkaTemplate<String, OrderDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 按 key 发送顺序消息（同步）
     *
     * ============================================
     * 调用示例
     * ============================================
     * sendOrderly(order, order.getOrderId());
     * // 多次调用同一 orderId、不同 status，应看到相同 partition、递增 offset
     *
     * ============================================
     * Topic 选择
     * ============================================
     * 使用 ORDER_STATUS_CHANGED_TOPIC，与「创建订单」简单消息 Topic 分离，
     * 避免两种语义混在同一消费组里不好演示。
     *
     * @param order 订单（至少含 orderId、status）
     * @param key   分区键，必须与「需要有序的业务维度」一致，通常等于 orderId
     * @return 发送结果（topic/partition/offset）；messageId 本 Demo 未生成，为 null
     */
    public KafkaSendResult sendOrderly(OrderDTO order, String key) {
        try {
            // send(topic, key, data)：key 决定分区；value 为 JSON 序列化后的 OrderDTO
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

            // 顺序场景 Demo 未单独生成 messageId，用 null 占位
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
