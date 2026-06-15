package com.mvp.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单事件消息体
 *
 * <p>用于 RocketMQ 异步下单场景：
 * <ul>
 *   <li>生产者：OrderProducerService 构建并发送到 order-events topic</li>
 *   <li>消费者：OrderEventConsumer 接收并执行订单落库</li>
 * </ul>
 *
 * <p>幂等性保障：
 * <ul>
 *   <li>messageId：全局唯一，用于 RocketMQ 消息去重</li>
 *   <li>businessKey：业务唯一键 {userId}#{goodsId}，用于消费端防重表</li>
 *   <li>订单表唯一索引：uk_order_user_goods 最终兜底</li>
 * </ul>
 */
@Data
public class OrderEventMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一ID，使用 UUID 保证全局唯一性
     * RocketMQ 消息级别的去重依赖此字段
     */
    private String messageId;

    /**
     * 业务唯一键，格式：{userId}#{goodsId}
     * 用于消费端幂等性检查（消息去重表）
     */
    private String businessKey;

    /**
     * 事件类型，当前固定为 "order.placed"
     */
    private String eventType;

    /**
     * 事件发生时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 消息版本号，用于未来消息格式升级时的兼容处理
     */
    private Integer version;

    /**
     * 业务数据载荷
     */
    private OrderPlacedPayload payload;

    /**
     * 订单下单事件的业务数据
     */
    @Data
    public static class OrderPlacedPayload implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户ID（32位无横杠 UUIDv7）
         */
        private String userId;

        /**
         * 商品ID（32位无横杠 UUIDv7）
         */
        private String goodsId;

        /**
         * 购买数量
         */
        private Integer buyCount;

        /**
         * 商品快照，用于消费端校验和补偿
         * 避免消费时商品信息已变更导致逻辑错误
         */
        private GoodsSnapshot goodsSnapshot;
    }

    /**
     * 商品快照数据
     * 冗余存储商品信息，保证消费端使用的是下单时的状态
     */
    @Data
    public static class GoodsSnapshot implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;
        private String name;
        private BigDecimal seckillPrice;
        private Integer totalStock;
        private Integer limitPerUser;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer status;
    }
}
