package com.mvp.order.mq.consumer;

import com.mvp.common.vo.ResultVO;
import com.mvp.order.dto.GoodsInfoDto;
import com.mvp.order.dto.OrderEventMessage;
import com.mvp.order.dto.OrderResultDto;
import com.mvp.order.entity.Order;
import com.mvp.order.feign.GoodsStockClient;
import com.mvp.order.service.OrderMessageProcessedService;
import com.mvp.order.service.OrderTxService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 订单事件消费者
 *
 * <p>监听 order-events topic，异步处理秒杀下单请求：
 * <ol>
 *   <li>幂等性检查（分布式锁 + 消息去重表）</li>
 *   <li>库存预扣（Feign 调用 goods 服务）</li>
 *   <li>订单落库（独立事务）</li>
 *   <li>缓存结果到 Redis</li>
 *   <li>失败时回补库存</li>
 * </ol>
 *
 * <p>幂等性三层防护：
 * <ul>
 *   <li>第1层：Redis 分布式锁（按 businessKey），防止并发重复处理</li>
 *   <li>第2层：消息去重表（t_order_message_processed），防止消息重投</li>
 *   <li>第3层：订单表唯一索引（uk_order_user_goods），数据库层兜底</li>
 * </ul>
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "order-events",
    consumerGroup = "order-consumer-group",
    selectorExpression = "order.placed"
)
public class OrderEventConsumer implements RocketMQListener<OrderEventMessage> {

    private static final String RESULT_KEY_PREFIX = "seckill:result:";
    private static final String LOCK_KEY_PREFIX = "order:processing:";

    private final GoodsStockClient goodsStockClient;
    private final OrderTxService orderTxService;
    private final OrderMessageProcessedService processedService;
    private final RedissonClient redissonClient;

    public OrderEventConsumer(GoodsStockClient goodsStockClient,
                             OrderTxService orderTxService,
                             OrderMessageProcessedService processedService,
                             RedissonClient redissonClient) {
        this.goodsStockClient = goodsStockClient;
        this.orderTxService = orderTxService;
        this.processedService = processedService;
        this.redissonClient = redissonClient;
    }

    @Override
    public void onMessage(OrderEventMessage message) {
        String messageId = message.getMessageId();
        String businessKey = message.getBusinessKey();

        log.info("开始处理订单事件 messageId={} businessKey={}", messageId, businessKey);

        // 第1层：Redis 分布式锁，防止并发重复处理
        String lockKey = LOCK_KEY_PREFIX + businessKey;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试加锁，等待最多5秒
            if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                log.warn("获取分布式锁失败，消息可能正在被其他消费者处理 businessKey={}", businessKey);
                return;
            }

            try {
                // 第2层：消息去重表检查
                if (processedService.isProcessed(messageId)) {
                    log.info("消息已处理过，直接返回 messageId={}", messageId);
                    return;
                }

                // 执行核心业务逻辑
                processOrder(message);

                // 标记消息为已处理
                processedService.markAsProcessed(messageId, businessKey);

                log.info("订单事件处理成功 messageId={} businessKey={}", messageId, businessKey);

            } finally {
                lock.unlock();
            }

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断 businessKey={}", businessKey, ex);
            throw new RuntimeException("消息处理被中断", ex);

        } catch (Exception ex) {
            log.error("订单事件处理失败，将触发RocketMQ自动重试 messageId={} businessKey={}", messageId, businessKey, ex);
            // 抛出异常，触发RocketMQ框架自动重试机制：
            // - 重试次数：最多16次
            // - 重试间隔：10s, 30s, 1m, 2m, 3m, 4m, 5m, 6m, 7m, 8m, 9m, 10m, 20m, 30m, 1h, 2h
            // - 超过16次后，消息进入死信队列：%DLQ%order-consumer-group
            throw ex;
        }
    }

    /**
     * 核心业务逻辑：库存扣减 + 订单落库
     */
    private void processOrder(OrderEventMessage message) {
        OrderEventMessage.OrderPlacedPayload payload = message.getPayload();
        String userId = payload.getUserId();
        String goodsId = payload.getGoodsId();
        int buyCount = payload.getBuyCount();
        OrderEventMessage.GoodsSnapshot snapshot = payload.getGoodsSnapshot();

        // 转换为 GoodsInfoDto
        GoodsInfoDto goods = convertToGoodsInfo(snapshot);

        try {
            // 预扣库存
            if (!deductStock(goodsId, buyCount)) {
                String errorMsg = "库存不足，无法下单";
                cacheResult(userId, goodsId, buildFailResult(errorMsg));
                log.warn("订单处理失败：{} userId={} goodsId={}", errorMsg, userId, goodsId);
                return;
            }

            // 订单落库（独立事务）
            OrderResultDto result = orderTxService.createOrder(userId, goods, buyCount);

            // 缓存成功结果
            cacheResult(userId, goodsId, result);

            log.info("订单创建成功 userId={} goodsId={} orderId={}", userId, goodsId, result.getOrderId());

        } catch (Exception ex) {
            // 失败补偿：回补库存
            rollbackStock(goodsId, buyCount);

            // 缓存失败结果
            cacheResult(userId, goodsId, buildFailResult(ex.getMessage()));

            log.error("订单落库失败并已回补库存 userId={} goodsId={}", userId, goodsId, ex);
            throw ex;
        }
    }

    /**
     * 预扣库存
     */
    private boolean deductStock(String goodsId, int buyCount) {
        try {
            ResultVO<Boolean> resp = goodsStockClient.deduct(goodsId, buyCount);
            return resp != null && resp.isSuccess() && Boolean.TRUE.equals(resp.getData());
        } catch (Exception ex) {
            log.error("预扣库存调用失败 goodsId={} buyCount={}", goodsId, buyCount, ex);
            return false;
        }
    }

    /**
     * 回补库存
     */
    private void rollbackStock(String goodsId, int buyCount) {
        try {
            goodsStockClient.rollback(goodsId, buyCount);
            log.info("库存回补成功 goodsId={} buyCount={}", goodsId, buyCount);
        } catch (Exception ex) {
            log.error("库存回补失败 goodsId={} buyCount={}", goodsId, buyCount, ex);
        }
    }

    /**
     * 缓存结果到 Redis
     */
    private void cacheResult(String userId, String goodsId, OrderResultDto result) {
        String resultKey = RESULT_KEY_PREFIX + userId + ":" + goodsId;
        String encodedResult = encodeResult(result);
        redissonClient.getBucket(resultKey).set(encodedResult, Duration.ofHours(2));
    }

    /**
     * 构建失败结果
     */
    private OrderResultDto buildFailResult(String message) {
        OrderResultDto result = new OrderResultDto();
        result.setStatus(OrderResultDto.STATUS_FAIL);
        result.setMessage(message != null ? message : "秒杀失败");
        return result;
    }

    /**
     * 转换商品快照为 GoodsInfoDto
     */
    private GoodsInfoDto convertToGoodsInfo(OrderEventMessage.GoodsSnapshot snapshot) {
        GoodsInfoDto goods = new GoodsInfoDto();
        goods.setId(snapshot.getId());
        goods.setName(snapshot.getName());
        goods.setSeckillPrice(snapshot.getSeckillPrice());
        goods.setTotalStock(snapshot.getTotalStock());
        goods.setLimitPerUser(snapshot.getLimitPerUser());

        // LocalDateTime 转 Date
        goods.setStartTime(snapshot.getStartTime() != null
            ? Date.from(snapshot.getStartTime().atZone(ZoneId.systemDefault()).toInstant())
            : null);
        goods.setEndTime(snapshot.getEndTime() != null
            ? Date.from(snapshot.getEndTime().atZone(ZoneId.systemDefault()).toInstant())
            : null);

        goods.setStatus(snapshot.getStatus());
        return goods;
    }

    /**
     * 编码结果为字符串
     */
    private String encodeResult(OrderResultDto result) {
        return String.join("|",
                String.valueOf(result.getStatus()),
                result.getRequestNo() != null ? result.getRequestNo() : "",
                result.getOrderId() != null ? result.getOrderId() : "",
                result.getMessage() != null ? result.getMessage() : "");
    }
}
