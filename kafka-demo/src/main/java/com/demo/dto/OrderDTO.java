package com.demo.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单消息体（演示用 DTO）
 *
 * ============================================
 * 在 Kafka 链路中的角色
 * ============================================
 * - Producer：序列化为 JSON 写入 Topic（JsonSerializer）
 * - Consumer：反序列化为本类（JsonDeserializer + trusted.packages）
 * - 必须可序列化：实现 Serializable 是习惯写法；Kafka 侧实际主要靠 JSON 编解码
 *
 * ============================================
 * 字段说明
 * ============================================
 * orderId      订单号；也常作为消息 key，保证同一订单进同一 partition
 * userId       用户标识
 * productName  商品名（演示字段）
 * quantity     数量
 * amount       金额
 * status       订单状态：CREATED / PAID / SHIPPED / COMPLETED
 *
 * ============================================
 * 与 application.yml 的对应关系
 * ============================================
 * spring.kafka.consumer.properties:
 *   spring.json.trusted.packages: com.demo.dto
 *   spring.json.value.default.type: com.demo.dto.OrderDTO
 * → 反序列化时只信任本包，默认类型为本类，避免任意类型反序列化风险
 *
 * ============================================
 * 注意
 * ============================================
 * - 本 Demo 为简化字段，生产应对 schema 演进（兼容字段增删）有明确策略
 * - 金额用 BigDecimal，避免 double 精度问题
 */
@Data
public class OrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单 ID；简单消息与顺序消息都会用到，顺序场景下建议同时作为 Kafka key */
    private String orderId;

    /** 下单用户 ID */
    private String userId;

    /** 商品名称（演示） */
    private String productName;

    /** 购买数量 */
    private Integer quantity;

    /** 订单金额 */
    private BigDecimal amount;

    /**
     * 订单状态。
     * <p>建议取值：CREATED → PAID → SHIPPED → COMPLETED</p>
     * <p>顺序消息 Demo 会按同一 orderId 连续发送多种 status，观察分区内顺序消费。</p>
     */
    private String status;
}
