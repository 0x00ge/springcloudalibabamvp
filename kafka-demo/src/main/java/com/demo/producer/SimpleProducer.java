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
 * 什么是「简单消息」？
 * ============================================
 * - 发送到指定 Topic，不依赖全局顺序
 * - 使用 key 参与分区路由（本 Demo 用 orderId 作 key，便于观察分区）
 * - Broker 按 acks 策略确认后，Producer 拿到 RecordMetadata
 * - 适合通知、日志、异步任务、数据同步等大部分场景
 *
 * ============================================
 * KafkaTemplate 是什么？
 * ============================================
 * - Spring Kafka 封装的发送入口（类似 RocketMQ 的 RocketMQTemplate）
 * - Key 类型 String，Value 类型 OrderDTO（由 JsonSerializer 编成 JSON）
 * - 底层 Producer 由 spring.kafka.producer.* 配置（acks、幂等、重试等）
 *
 * ============================================
 * 同步 vs 异步
 * ============================================
 * | 方式 | 是否等待 Broker | 可靠性体感 | 性能 | 适用 |
 * |------|-----------------|------------|------|------|
 * | 同步 get() | 是 | 高，失败立刻抛错 | 中 | 重要业务（推荐演示） |
 * | 异步 whenComplete | 否 | 高，失败在回调处理 | 高 | 接口要快速返回 |
 *
 * ============================================
 * 与 application.yml 的关系
 * ============================================
 * - acks: all + enable.idempotence: true → 更强的「写成功」语义（配合重试）
 * - JsonSerializer → value 自动变 JSON，无需手写 ObjectMapper
 *
 * ============================================
 * Header 的用途
 * ============================================
 * - messageId：业务消息唯一 ID，便于链路追踪与幂等（Broker 的 offset 是存储坐标，不是业务 ID）
 * - orderId：冗余一份在 header，消费侧或中间件可在不解析 body 时做路由/过滤
 */
@Slf4j
@Component
public class SimpleProducer {

    /**
     * Spring Boot 自动配置的 KafkaTemplate。
     * <p>泛型：Key=String，Value=OrderDTO（与 yml 中 serializer 一致）</p>
     */
    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;

    public SimpleProducer(KafkaTemplate<String, OrderDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 同步发送（推荐理解「写成功」语义时使用）
     *
     * ============================================
     * 工作流程
     * ============================================
     * 1. 构造 ProducerRecord（topic、key、value、headers）
     * 2. kafkaTemplate.send(...) 返回 CompletableFuture
     * 3. future.get(3, SECONDS) 阻塞等待 Broker 确认（受 acks 影响）
     * 4. 成功：封装 topic/partition/offset 返回
     * 5. 超时或失败：抛 RuntimeException，由调用方决定重试或给用户报错
     *
     * ============================================
     * 为什么要设超时 get(3, SECONDS)？
     * ============================================
     * - 避免 Broker 不可达时 HTTP 线程无限阻塞
     * - 3 秒仅 Demo 值，生产应按 SLA 与网络情况调整
     *
     * @param order 订单对象（会写入 value）
     * @return KafkaSendResult 发送结果（messageId、topic、partition、offset）
     */
    public KafkaSendResult sendSync(OrderDTO order) {
        // 业务侧消息 ID：写入 header，便于日志关联；不等于 Kafka offset
        String messageId = UUID.randomUUID().toString();

        try {
            // send 异步提交到 Producer 缓冲；get 等待元数据返回
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
            // 包含超时、序列化失败、Broker 拒绝等；向上抛出让 Controller 感知失败
            log.error(">>> kafkaSyncSendFail : orderId={}", order.getOrderId(), e);
            throw new RuntimeException("Kafka 同步发送失败", e);
        }
    }

    /**
     * 异步发送
     *
     * ============================================
     * 工作流程
     * ============================================
     * 1. send 后立即返回（本方法不阻塞等 Broker）
     * 2. whenComplete 在发送完成线程回调成功/失败
     * 3. 方法末尾的 log 只表示「已提交发送请求」，不代表 Broker 已落盘
     *
     * ============================================
     * 使用注意
     * ============================================
     * - HTTP 接口若马上返回「成功」，此时消息可能尚未写入 Broker
     * - 失败只在回调里打日志；生产应接入指标/告警或本地补偿表
     * - 回调线程不要做过重业务，避免拖垮 Producer 网络线程
     *
     * @param order 订单对象
     */
    public void sendAsync(OrderDTO order) {
        String messageId = UUID.randomUUID().toString();
        CompletableFuture<SendResult<String, OrderDTO>> future =
            kafkaTemplate.send(buildRecord(order, messageId));

        // 异步回调：成功打印分区信息，失败打印异常
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

        // 注意：这行只代表请求已交给 KafkaTemplate，不是 Broker ack
        log.info(">>> kafkaAsyncSendCommit : orderId={} messageId={}", order.getOrderId(), messageId);
    }

    /**
     * 构造 ProducerRecord：指定 Topic、分区键、消息体与业务 Header。
     *
     * ============================================
     * 分区路由规则（默认）
     * ============================================
     * - key != null：hash(key) % partitionCount → 固定分区（相同 key 同分区）
     * - key == null：轮询/粘性分区策略（版本不同实现略有差异）
     * - 本 Demo key=orderId，便于和顺序消息对照理解
     *
     * @param order     消息体
     * @param messageId 写入 header 的业务消息 ID
     */
    private ProducerRecord<String, OrderDTO> buildRecord(OrderDTO order, String messageId) {
        // 构造：topic、key、value（未指定 partition 时由分区器根据 key 选择）
        ProducerRecord<String, OrderDTO> record = new ProducerRecord<>(
            KafkaConfig.ORDER_CREATED_TOPIC,
            order.getOrderId(),
            order
        );
        // Header 值为字节数组；消费侧如需读取要用同样编码
        record.headers().add(new RecordHeader("messageId", messageId.getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("orderId", order.getOrderId().getBytes(StandardCharsets.UTF_8)));
        return record;
    }
}
