package com.demo.producer;

import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 简单消息生产者
 *
 * ============================================
 * 什么是简单消息？
 * ============================================
 * - RocketMQ 最基本的消息类型
 * - 不保证顺序，但保证可靠（会重试）
 * - 适合 90% 的业务场景
 *
 * ============================================
 * 使用场景举例
 * ============================================
 * 1. 发送邮件/短信通知
 * 2. 记录操作日志
 * 3. 触发异步任务
 * 4. 数据同步
 *
 * ============================================
 * 三种发送方式对比
 * ============================================
 *
 * | 方式 | 等待响应 | 可靠性 | 性能 | 适用场景 |
 * |------|---------|--------|------|---------|
 * | 同步 | 是 | 高 | 中 | 重要业务（推荐） |
 * | 异步 | 否 | 高 | 高 | 对响应时间敏感 |
 * | 单向 | 否 | 低 | 最高 | 日志记录（不推荐） |
 */
@Slf4j
@Component
public class SimpleProducer {

    // RocketMQ 核心模板类，Spring Boot 自动注入
    // 提供了发送消息的所有方法
    private final RocketMQTemplate rocketMQTemplate;

    public SimpleProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 同步发送（推荐）⭐
     *
     * ============================================
     * 工作流程
     * ============================================
     * 1. Producer 发送消息到 Broker
     * 2. Broker 持久化到磁盘
     * 3. Broker 返回成功响应
     * 4. Producer 收到响应后，方法才返回
     *
     * ============================================
     * 为什么推荐同步发送？
     * ============================================
     * - 可靠性高：确认消息已存储才返回
     * - 易于处理：发送失败直接抛异常
     * - 性能够用：3000ms 超时，绝大多数在 10ms 内完成
     *
     * ============================================
     * 什么时候用？
     * ============================================
     * - 订单创建后发送通知（必须确保消息送达）
     * - 支付成功后更新库存（不能丢消息）
     * - 关键业务流程（失败要能知道）
     *
     * @param order 订单对象
     * @return SendResult 发送结果（包含 msgId、队列信息等）
     */
    public SendResult sendSync(OrderDTO order) {
        try {
            // destination 格式：topic:tag
            // - topic: 消息主题（类似数据库的表）
            // - tag: 消息标签（类似表的分类字段，用于过滤）
            String destination = "demo-topic:order-created";

            // 构建消息
            // 为什么要用 MessageBuilder？
            // - 可以设置 Header（消息头，用于传递元数据）
            // - 可以设置 payload（消息体，实际业务数据）
            SendResult result = rocketMQTemplate.syncSend(
                destination,
                MessageBuilder.withPayload(order)  // 消息体：订单对象（自动序列化为 JSON）
                    .setHeader("messageId", UUID.randomUUID().toString())  // 消息ID（用于去重）
                    .setHeader("orderId", order.getOrderId())  // 订单ID（方便查询）
                    .build()
            );

            // 发送成功，记录日志
            // msgId: RocketMQ 自动生成的消息唯一标识
            log.info("✅ 同步发送成功 orderId= msgId={}",
                     order.getOrderId(), result.getMsgId());

            return result;

        } catch (Exception e) {
            // 发送失败的可能原因：
            // 1. 网络故障
            // 2. Broker 宕机
            // 3. 超时（默认 3000ms）
            log.error("❌ 同步发送失败 orderId={}", order.getOrderId(), e);

            // 抛出异常，让调用方知道消息发送失败
            // 调用方可以选择：
            // - 重试
            // - 记录到数据库，后续补偿
            // - 降级处理（例如同步处理业务）
            throw new RuntimeException("消息发送失败", e);
        }
    }

    /**
     * 异步发送
     *
     * ============================================
     * 工作流程
     * ============================================
     * 1. Producer 发送消息到 Broker（不等待）
     * 2. 方法立即返回
     * 3. Broker 处理完成后，回调通知结果
     *
     * ============================================
     * 为什么用异步发送？
     * ============================================
     * - 性能高：不阻塞主线程
     * - 响应快：接口立即返回
     *
     * ============================================
     * 什么时候用？
     * ============================================
     * - 对响应时间要求极高的接口
     * - 发送大量消息（批量通知）
     * - 消息发送失败不影响主流程
     *
     * ============================================
     * 注意事项
     * ============================================
     * - 必须处理回调（onSuccess 和 onException）
     * - 发送失败需要记录，后续补偿
     *
     * @param order 订单对象
     */
    public void sendAsync(OrderDTO order) {
        String destination = "demo-topic:order-created";

        // asyncSend 需要传入回调对象
        // 回调在 Broker 响应后触发（可能在另一个线程）
        rocketMQTemplate.asyncSend(
            destination,
            order,
            new SendCallback() {
                /**
                 * 发送成功回调
                 * 在 Broker 确认消息后执行
                 */
                @Override
                public void onSuccess(SendResult result) {
                    log.info("✅ 异步发送成功 orderId={} msgId={}",
                             order.getOrderId(), result.getMsgId());

                    // 这里可以：
                    // - 更新数据库状态
                    // - 记录发送成功日志
                    // - 触发后续流程
                }

                /**
                 * 发送失败回调
                 * 在重试多次仍失败后执行
                 */
                @Override
                public void onException(Throwable e) {
                    log.error("❌ 异步发送失败 orderId={}", order.getOrderId(), e);

                    // ⚠️ 重要：必须处理失败情况
                    // 常见做法：
                    // 1. 记录到数据库的"待发送表"
                    // 2. 定时任务扫描重试
                    // 3. 或者降级处理（同步处理业务）

                    // 示例：记录到数据库
                    // msgFailedService.save(order.getOrderId(), e.getMessage());
                }
            }
        );

        // 注意：这行日志会在发送完成前输出
        // 因为 asyncSend 不等待 Broker 响应就返回
        log.info("📤 异步发送请求已提交 orderId={}", order.getOrderId());
    }

    /**
     * 单向发送（不推荐）⚠️
     *
     * ============================================
     * 工作流程
     * ============================================
     * 1. Producer 发送消息到 Broker
     * 2. 不等待响应，直接返回
     * 3. 不关心发送结果
     *
     * ============================================
     * 为什么不推荐？
     * ============================================
     * - 不可靠：消息可能丢失
     * - 不知道是否发送成功
     * - 无法处理失败情况
     *
     * ============================================
     * 什么时候用？
     * ============================================
     * - 日志记录（丢失几条不影响业务）
     * - 监控打点（允许少量丢失）
     * - 性能要求极高且允许丢消息
     *
     * ============================================
     * 警告
     * ============================================
     * 对于重要业务，千万别用单向发送！
     *
     * @param order 订单对象
     */
    public void sendOneWay(OrderDTO order) {
        String destination = "demo-topic:order-created";

        // sendOneWay 没有返回值
        // 发送后立即返回，不管成功还是失败
        rocketMQTemplate.sendOneWay(destination, order);

        // 这行日志可能在消息还没发到 Broker 就输出了
        log.info("📤 单向发送 orderId={} （不关心结果）", order.getOrderId());

        // ⚠️ 注意：如果这里马上关闭 JVM，消息可能来不及发送
    }
}

