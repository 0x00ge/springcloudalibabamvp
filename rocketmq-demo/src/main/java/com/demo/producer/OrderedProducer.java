package com.demo.producer;

import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * 顺序消息生产者
 *
 * ============================================
 * 什么是顺序消息？
 * ============================================
 * - 保证消息按发送顺序消费
 * - 同一个 hashKey 的消息顺序发送到同一个队列
 * - 消费端按顺序依次处理
 *
 * ============================================
 * 为什么需要顺序消息？
 * ============================================
 * 某些业务场景必须保证顺序：
 * 1. 订单状态变更：创建 → 支付 → 发货 → 完成
 * 2. 数据库 binlog 同步：INSERT → UPDATE → DELETE
 * 3. 用户操作记录：登录 → 浏览 → 下单 → 支付
 *
 * 如果不保证顺序会怎样？
 * - 订单先收到"发货"，后收到"创建" → 数据错乱
 * - 数据库先 DELETE，后 INSERT → 数据丢失
 */
@Slf4j
@Component
public class OrderedProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public OrderedProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 发送顺序消息
     *
     * @param order 订单对象
     * @param hashKey 顺序键（同一 key 的消息按顺序发送）
     * @return SendResult 发送结果
     */
    public SendResult sendOrderly(OrderDTO order, String hashKey) {
        try {
            String destination = "demo-topic:order-status-changed";

            SendResult result = rocketMQTemplate.syncSendOrderly(
                destination,
                order,
                hashKey  // ⚠️ 关键参数：同一 hashKey 的消息进入同一队列
            );

            log.info("🔢 顺序消息发送成功 orderId={} status={} hashKey={}",
                     order.getOrderId(), order.getStatus(), hashKey);

            return result;

        } catch (Exception e) {
            log.error("❌ 顺序消息发送失败 orderId={}", order.getOrderId(), e);
            throw new RuntimeException("顺序消息发送失败", e);
        }
    }
}
