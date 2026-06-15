package com.mvp.order.service.impl;

import com.mvp.order.dto.GoodsInfoDto;
import com.mvp.order.dto.OrderEventMessage;
import com.mvp.order.dto.OrderRequestDto;
import com.mvp.order.service.OrderProducerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 订单消息生产者服务实现
 *
 * <p>负责将秒杀下单请求转换为 RocketMQ 消息并发送，
 * 实现异步削峰。
 */
@Slf4j
@Service
public class OrderProducerServiceImpl implements OrderProducerService {

    private static final String TOPIC = "order-events";
    private static final String TAG = "order.placed";

    private final RocketMQTemplate rocketMQTemplate;

    public OrderProducerServiceImpl(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public boolean sendOrderEvent(String userId, OrderRequestDto requestDto, GoodsInfoDto goods) {
        try {
            // 构建消息体
            OrderEventMessage message = buildMessage(userId, requestDto, goods);

            // 发送消息到 RocketMQ
            // 格式：topic:tag，tag 用于消息过滤
            String destination = TOPIC + ":" + TAG;

            rocketMQTemplate.syncSend(
                destination,
                MessageBuilder.withPayload(message)
                    .setHeader("messageId", message.getMessageId())
                    .setHeader("businessKey", message.getBusinessKey())
                    .build()
            );

            log.info("订单事件发送成功 messageId={} businessKey={} userId={} goodsId={}",
                    message.getMessageId(),
                    message.getBusinessKey(),
                    userId,
                    requestDto.getGoodsId());

            return true;

        } catch (Exception ex) {
            log.error("订单事件发送失败 userId={} goodsId={}", userId, requestDto.getGoodsId(), ex);
            // TODO: 可选优化 - 发送失败时保存到本地消息表，后续定时重发
            return false;
        }
    }

    /**
     * 构建订单事件消息
     */
    private OrderEventMessage buildMessage(String userId, OrderRequestDto requestDto, GoodsInfoDto goods) {
        OrderEventMessage message = new OrderEventMessage();

        // 消息ID：全局唯一，用于 RocketMQ 去重
        message.setMessageId(UUID.randomUUID().toString());

        // 业务key：用于消费端幂等性检查
        message.setBusinessKey(userId + "#" + goods.getId());

        message.setEventType("order.placed");
        message.setTimestamp(System.currentTimeMillis());
        message.setVersion(1);

        // 构建业务载荷
        OrderEventMessage.OrderPlacedPayload payload = new OrderEventMessage.OrderPlacedPayload();
        payload.setUserId(userId);
        payload.setGoodsId(goods.getId());
        payload.setBuyCount(requestDto.getBuyCount() != null ? requestDto.getBuyCount() : 1);

        // 构建商品快照
        OrderEventMessage.GoodsSnapshot snapshot = new OrderEventMessage.GoodsSnapshot();
        snapshot.setId(goods.getId());
        snapshot.setName(goods.getName());
        snapshot.setSeckillPrice(goods.getSeckillPrice());
        snapshot.setTotalStock(goods.getTotalStock());
        snapshot.setLimitPerUser(goods.getLimitPerUser());

        // Date 转 LocalDateTime
        snapshot.setStartTime(goods.getStartTime() != null
            ? LocalDateTime.ofInstant(goods.getStartTime().toInstant(), ZoneId.systemDefault())
            : null);
        snapshot.setEndTime(goods.getEndTime() != null
            ? LocalDateTime.ofInstant(goods.getEndTime().toInstant(), ZoneId.systemDefault())
            : null);

        snapshot.setStatus(goods.getStatus());

        payload.setGoodsSnapshot(snapshot);
        message.setPayload(payload);

        return message;
    }
}
