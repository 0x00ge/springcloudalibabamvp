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

/**
 * 订单事件生产者
 * <p>
 * 负责将订单下单事件发送到 RocketMQ 消息队列，供下游服务（如通知服务、数据分析服务等）消费。
 * 采用同步发送方式确保消息可靠投递。
 * </p>
 *
 * <p>消息格式：</p>
 * <ul>
 *   <li>Topic: order-events</li>
 *   <li>Tag: order.placed</li>
 *   <li>Headers: messageId（消息唯一ID）、businessKey（业务幂等键）</li>
 *   <li>Payload: 订单下单事件详情，包含用户ID、商品快照等信息</li>
 * </ul>
 *
 * @author mvp
 * @since 1.0
 */
@Slf4j
@Component
public class OrderEventProducer {

    /**
     * 消息目的地：Topic 为 order-events，Tag 为 order.placed
     * 格式：topic:tag
     */
    private static final String DESTINATION = "order-events:order.placed";

    private final RocketMQTemplate rocketMQTemplate;

    public OrderEventProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 发送订单下单事件到 RocketMQ
     * <p>
     * 使用同步发送方式，确保消息在返回前已成功投递到 Broker。
     * 消息包含用户ID、商品信息快照等关键业务数据，便于下游服务进行异步处理。
     * </p>
     *
     * <p>消息幂等性保证：</p>
     * <ul>
     *   <li>messageId: 全局唯一的消息ID（UUID）</li>
     *   <li>businessKey: 业务幂等键（userId#goodsId），用于消费端去重</li>
     * </ul>
     *
     * <p>失败重试机制：</p>
     * <ul>
     *   <li>自动重试3次（包含首次发送）</li>
     *   <li>重试间隔：100ms</li>
     *   <li>重试失败后抛出异常，由调用方降级处理</li>
     * </ul>
     *
     * @param userId 用户ID
     * @param requestDto 订单请求信息，包含商品ID和购买数量
     * @param goods 商品详细信息，将作为快照保存在消息中
     * @return true-发送成功，false-发送失败
     */
    public boolean sendOrderEvent(String userId, OrderRequestDto requestDto, GoodsInfoDto goods) {
        String goodsId = requestDto.getGoodsId();
        int maxRetries = 3;
        int retryIntervalMs = 100;

        OrderEventMessage message = buildMessage(userId, requestDto, goods);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 同步发送，阻塞直到 Broker 确认接收
                rocketMQTemplate.syncSend(
                    DESTINATION,
                    MessageBuilder.withPayload(message)
                        .setHeader("messageId", message.getMessageId())
                        .setHeader("businessKey", message.getBusinessKey())
                        .build()
                );

                log.info("订单事件发送成功 messageId={} businessKey={} userId={} goodsId={} attempt={}",
                        message.getMessageId(), message.getBusinessKey(), userId, goodsId, attempt);
                return true;

            } catch (Exception ex) {
                if (attempt < maxRetries) {
                    log.warn("订单事件发送失败，准备重试 userId={} goodsId={} attempt={}/{} error={}",
                            userId, goodsId, attempt, maxRetries, ex.getMessage());
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("重试等待被中断 userId={} goodsId={}", userId, goodsId);
                        return false;
                    }
                } else {
                    log.error("订单事件发送失败，已达最大重试次数 userId={} goodsId={} attempts={}",
                            userId, goodsId, maxRetries, ex);
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * 构建订单事件消息
     * <p>
     * 将订单请求信息和商品信息封装为标准的事件消息格式。
     * 商品信息作为快照保存，确保消费端看到的是下单时刻的商品状态，避免后续商品信息变更导致的数据不一致。
     * </p>
     *
     * @param userId 用户ID
     * @param requestDto 订单请求信息
     * @param goods 商品详细信息
     * @return 订单事件消息对象
     */
    private OrderEventMessage buildMessage(String userId, OrderRequestDto requestDto, GoodsInfoDto goods) {
        // 构建消息基础信息
        OrderEventMessage message = new OrderEventMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setBusinessKey(userId + "#" + goods.getId()); // 业务幂等键，用于消费端去重
        message.setEventType("order.placed");
        message.setTimestamp(System.currentTimeMillis());
        message.setVersion(1);

        // 构建订单业务负载
        OrderEventMessage.OrderPlacedPayload payload = new OrderEventMessage.OrderPlacedPayload();
        payload.setUserId(userId);
        payload.setGoodsId(goods.getId());
        payload.setBuyCount(requestDto.getBuyCount() != null ? requestDto.getBuyCount() : 1);

        // 构建商品快照，冻结下单时刻的商品状态
        OrderEventMessage.GoodsSnapshot snapshot = new OrderEventMessage.GoodsSnapshot();
        snapshot.setId(goods.getId());
        snapshot.setName(goods.getName());
        snapshot.setSeckillPrice(goods.getSeckillPrice());
        snapshot.setTotalStock(goods.getTotalStock());
        snapshot.setLimitPerUser(goods.getLimitPerUser());
        // 转换 Date 为 LocalDateTime，兼容不同时区
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
