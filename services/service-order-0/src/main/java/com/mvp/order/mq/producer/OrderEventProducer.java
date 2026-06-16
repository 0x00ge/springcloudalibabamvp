package com.mvp.order.mq.producer;

import com.mvp.order.dto.GoodsInfoDto;
import com.mvp.order.dto.OrderEventMessage;
import com.mvp.order.dto.OrderRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Component
public class OrderEventProducer {

    private static final String DESTINATION = "order-events:order.placed";

    private final RocketMQTemplate rocketMQTemplate;

    public OrderEventProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public boolean sendOrderEvent(String userId, OrderRequestDto requestDto, GoodsInfoDto goods) {
        try {
            OrderEventMessage message = buildMessage(userId, requestDto, goods);
            rocketMQTemplate.syncSend(
                DESTINATION,
                MessageBuilder.withPayload(message)
                    .setHeader("messageId", message.getMessageId())
                    .setHeader("businessKey", message.getBusinessKey())
                    .build()
            );
            log.info("订单事件发送成功 messageId={} businessKey={} userId={} goodsId={}",
                    message.getMessageId(), message.getBusinessKey(), userId, requestDto.getGoodsId());
            return true;
        } catch (Exception ex) {
            log.error("订单事件发送失败 userId={} goodsId={}", userId, requestDto.getGoodsId(), ex);
            return false;
        }
    }

    private OrderEventMessage buildMessage(String userId, OrderRequestDto requestDto, GoodsInfoDto goods) {
        OrderEventMessage message = new OrderEventMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setBusinessKey(userId + "#" + goods.getId());
        message.setEventType("order.placed");
        message.setTimestamp(System.currentTimeMillis());
        message.setVersion(1);

        OrderEventMessage.OrderPlacedPayload payload = new OrderEventMessage.OrderPlacedPayload();
        payload.setUserId(userId);
        payload.setGoodsId(goods.getId());
        payload.setBuyCount(requestDto.getBuyCount() != null ? requestDto.getBuyCount() : 1);

        OrderEventMessage.GoodsSnapshot snapshot = new OrderEventMessage.GoodsSnapshot();
        snapshot.setId(goods.getId());
        snapshot.setName(goods.getName());
        snapshot.setSeckillPrice(goods.getSeckillPrice());
        snapshot.setTotalStock(goods.getTotalStock());
        snapshot.setLimitPerUser(goods.getLimitPerUser());
        snapshot.setStartTime(goods.getStartTime() != null
            ? LocalDateTime.ofInstant(goods.getStartTime().toInstant(), ZoneId.systemDefault()) : null);
        snapshot.setEndTime(goods.getEndTime() != null
            ? LocalDateTime.ofInstant(goods.getEndTime().toInstant(), ZoneId.systemDefault()) : null);
        snapshot.setStatus(goods.getStatus());

        payload.setGoodsSnapshot(snapshot);
        message.setPayload(payload);
        return message;
    }
}
