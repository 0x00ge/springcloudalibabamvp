package com.demo.consumer;

import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 顺序消息消费者
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "demo-topic",
    consumerGroup = "demo-status-consumer-group",
    selectorExpression = "order-status-changed",
    consumeMode = ConsumeMode.ORDERLY  // 顺序消费
)
public class OrderedConsumer implements RocketMQListener<OrderDTO> {

    @Override
    public void onMessage(OrderDTO order) {
        log.info("🔢 顺序消费订单状态 orderId={} status={}",
                 order.getOrderId(), order.getStatus());

        // 同一个订单的消息会按顺序消费
        // CREATED → PAID → SHIPPED → COMPLETED

        updateOrderStatus(order);

        log.info("✅ 订单状态更新完成 orderId={} newStatus={}",
                 order.getOrderId(), order.getStatus());
    }

    private void updateOrderStatus(OrderDTO order) {
        // 模拟更新数据库
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("💾 更新数据库 orderId={} status={}",
                 order.getOrderId(), order.getStatus());
    }
}
