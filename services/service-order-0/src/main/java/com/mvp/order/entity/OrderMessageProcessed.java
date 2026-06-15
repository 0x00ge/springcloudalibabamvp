package com.mvp.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单事件消息已处理记录实体
 *
 * <p>用于 RocketMQ 消费端幂等性保障：
 * <ul>
 *   <li>消费者处理消息前，先查询此表判断消息是否已处理</li>
 *   <li>消息处理成功后，插入记录标记为已处理</li>
 *   <li>配合 Redis 分布式锁和订单表唯一索引，形成三层防重机制</li>
 * </ul>
 */
@Data
@TableName("t_order_message_processed")
public class OrderMessageProcessed {

    /**
     * 记录ID，32位无横杠 UUIDv7 格式
     * 与其他业务表主键保持一致
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * RocketMQ 消息唯一 ID
     * 对应 OrderEventMessage.messageId
     */
    private String messageId;

    /**
     * 业务唯一键，格式：{userId}#{goodsId}
     * 对应 OrderEventMessage.businessKey
     */
    private String businessKey;

    /**
     * 消息处理时间
     */
    private LocalDateTime processedAt;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;
}
