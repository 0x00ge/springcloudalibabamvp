package com.mvp.order.consumer;

import com.mvp.order.dto.OrderEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者
 *
 * <p>处理进入死信队列的订单事件消息。消息经过多次重试仍未成功时，
 * RocketMQ 会自动将其投递到死信队列（DLQ）。</p>
 *
 * <p>死信队列的消息通常表示存在严重问题，需要：
 * <ul>
 *   <li>详细日志记录</li>
 *   <li>告警通知管理员</li>
 *   <li>人工排查原因并决定是否手动恢复</li>
 * </ul>
 *
 * <p>死信队列 Topic 命名规则：{@code %DLQ%{consumerGroup}}
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "%DLQ%order-consumer-group",
    consumerGroup = "order-dlq-consumer-group"
)
public class OrderDLQConsumer implements RocketMQListener<OrderEventMessage> {

    @Override
    public void onMessage(OrderEventMessage message) {
        String messageId = message.getMessageId();
        String businessKey = message.getBusinessKey();

        log.error("【死信队列】订单事件进入死信队列，需要人工处理！messageId={} businessKey={} payload={}",
                messageId,
                businessKey,
                message.getPayload());

        // TODO: 实现以下功能
        // 1. 保存到数据库表（例如 t_order_dlq_message）用于管理后台查询
        // 2. 发送告警通知（邮件、钉钉、企业微信等）
        // 3. 提供手动重试接口，供运维人员排查后恢复

        // 示例告警信息
        String alertMessage = String.format(
            "订单消息处理失败进入死信队列！\n" +
            "messageId: %s\n" +
            "businessKey: %s\n" +
            "userId: %s\n" +
            "goodsId: %s\n" +
            "buyCount: %d\n" +
            "请尽快排查原因并处理",
            messageId,
            businessKey,
            message.getPayload().getUserId(),
            message.getPayload().getGoodsId(),
            message.getPayload().getBuyCount()
        );

        log.error("【死信队列告警】{}", alertMessage);

        // 不抛异常，避免死信队列的消息再次进入死信队列
    }
}
