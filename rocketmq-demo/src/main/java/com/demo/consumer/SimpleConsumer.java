package com.demo.consumer;

import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 简单消息消费者
 *
 * ============================================
 * 什么是消费者？
 * ============================================
 * - 从 RocketMQ 接收消息并处理的角色
 * - 一个 Topic 可以有多个消费者
 * - 每个消费者属于一个 Consumer Group
 *
 * ============================================
 * Consumer Group 是什么？
 * ============================================
 * - 一组消费者的逻辑集合
 * - 同一个 Group 内的消费者共同消费一个 Topic
 * - 一条消息只会被 Group 内的一个消费者处理
 *
 * 举例：
 * - Group A 有 3 个消费者：Consumer 1、2、3
 * - 一条消息只会被其中一个消费（负载均衡）
 * - 但 Group B 也能收到这条消息（广播）
 *
 * ============================================
 * @RocketMQMessageListener 注解说明
 * ============================================
 * - topic: 订阅哪个 Topic
 * - consumerGroup: 所属的消费者组（必须全局唯一）
 * - selectorExpression: 过滤 Tag（只消费指定 Tag 的消息）
 *   - "*" 表示消费所有 Tag
 *   - "order-created" 表示只消费 tag=order-created 的消息
 *   - "tagA || tagB" 表示消费 tagA 或 tagB
 *
 * ============================================
 * 消费模式（默认是并发消费）
 * ============================================
 * 1. 并发消费（CONCURRENTLY）：
 *    - 多个消息同时处理（多线程）
 *    - 不保证顺序
 *    - 性能高
 *    - 适合 90% 的场景
 *
 * 2. 顺序消费（ORDERLY）：
 *    - 同一队列的消息按顺序处理
 *    - 单线程处理
 *    - 性能较低
 *    - 适合需要顺序的场景（见 OrderedConsumer）
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "demo-topic",                    // 订阅 demo-topic
    consumerGroup = "demo-consumer-group",   // 消费者组名（全局唯一）
    selectorExpression = "order-created"     // 只消费 tag=order-created 的消息
)
public class SimpleConsumer implements RocketMQListener<OrderDTO> {

    /**
     * 消息处理方法
     *
     * ============================================
     * 何时调用？
     * ============================================
     * - RocketMQ 推送消息时自动调用
     * - 每条消息调用一次
     * - 可能在不同线程中调用（并发消费）
     *
     * ============================================
     * 方法执行流程
     * ============================================
     * 1. RocketMQ 推送消息到消费者
     * 2. 自动反序列化为 OrderDTO 对象
     * 3. 调用 onMessage 方法
     * 4. 如果方法正常返回 → 消费成功，ACK
     * 5. 如果方法抛异常 → 消费失败，重试
     *
     * ============================================
     * 重试机制
     * ============================================
     * - 消费失败（抛异常）会自动重试
     * - 默认重试 16 次
     * - 重试间隔递增：10s, 30s, 1m, 2m, 3m, 4m, 5m, 6m, 7m, 8m, 9m, 10m, 20m, 30m, 1h, 2h
     * - 16 次都失败后，进入死信队列
     *
     * ============================================
     * 死信队列
     * ============================================
     * - Topic 名称：%DLQ%消费者组名
     * - 例如：%DLQ%demo-consumer-group
     * - 需要人工处理或专门的消费者处理
     *
     * ============================================
     * 幂等性问题 ⚠️
     * ============================================
     * 消息可能重复消费，原因：
     * 1. 消费成功但 ACK 失败（网络抖动）
     * 2. Broker 认为消费超时，重新投递
     * 3. 消费者重启，重新拉取消息
     *
     * 解决方案：
     * 1. 数据库唯一索引（最简单）
     * 2. Redis 去重（高性能）
     * 3. 业务逻辑天然幂等（如设置状态）
     *
     * @param order 订单对象（已自动反序列化）
     */
    @Override
    public void onMessage(OrderDTO order) {
        log.info("📨 收到订单消息 orderId={} product={} quantity={} amount={}",
                 order.getOrderId(),
                 order.getProductName(),
                 order.getQuantity(),
                 order.getAmount());

        try {
            processOrder(order);
            log.info("✅ 订单处理成功 orderId={}", order.getOrderId());

        } catch (Exception e) {
            log.error("❌ 订单处理失败 orderId={}", order.getOrderId(), e);
            throw new RuntimeException("处理失败", e);
        }
    }

    /**
     * 业务处理方法（详细注释见文件末尾）
     */
    private void processOrder(OrderDTO order) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("💼 处理订单业务: 发送通知、更新库存、记录日志");
    }
}

/*
============================================
processOrder 方法详细说明
============================================

这里写你的业务逻辑，常见操作：
1. 幂等性检查（防止重复消费）
2. 数据库操作（插入/更新）
3. 调用其他服务（发邮件、发短信）
4. 记录处理结果

============================================
幂等性实现示例
============================================
// 方案1：数据库唯一索引
try {
    orderMapper.insert(order); // 唯一索引：orderId
} catch (DuplicateKeyException e) {
    log.warn("订单已存在，跳过 orderId={}", order.getOrderId());
    return; // 幂等：重复消息不报错
}

// 方案2：Redis 去重
String key = "processed:" + order.getOrderId();
if (redis.exists(key)) {
    log.warn("订单已处理，跳过 orderId={}", order.getOrderId());
    return;
}
// 处理业务...
redis.set(key, "1", 1, TimeUnit.DAYS);

============================================
真实代码示例
============================================
// 1. 幂等性检查
if (orderMapper.existsById(order.getOrderId())) {
    log.warn("订单已存在 orderId={}", order.getOrderId());
    return;
}

// 2. 插入数据库
Order entity = new Order();
entity.setId(order.getOrderId());
entity.setUserId(order.getUserId());
entity.setProductName(order.getProductName());
entity.setAmount(order.getAmount());
entity.setStatus("CREATED");
entity.setCreatedAt(LocalDateTime.now());
orderMapper.insert(entity);

// 3. 发送通知
notificationService.sendOrderCreated(order.getUserId(), order.getOrderId());

// 4. 更新库存
goodsClient.deductStock(order.getProductName(), order.getQuantity());
*/
