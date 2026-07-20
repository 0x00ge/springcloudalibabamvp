package com.demo.producer;

/**
 * Kafka 发送成功后的结果摘要（演示用，便于 HTTP 接口回传）
 *
 * ============================================
 * 各字段含义
 * ============================================
 * messageId  业务侧生成的消息 ID（放在 header 中，Broker 不生成该字段）；
 *            顺序消息 Demo 未单独生成时可为 null
 * topic      实际写入的 Topic 名
 * partition  消息落入的分区号（0-based）
 * offset     该分区内的偏移量，可唯一定位一条消息（topic+partition+offset）
 *
 * ============================================
 * 与 RecordMetadata 的关系
 * ============================================
 * Spring 的 SendResult.getRecordMetadata() 提供 topic/partition/offset；
 * 本 record 只是把常用字段抽出来给 Controller 返回 JSON，避免接口层依赖 Kafka API。
 *
 * ============================================
 * 使用场景
 * ============================================
 * - 同步发送成功后返回给调用方，便于对照日志与 Kafka UI
 * - 异步发送通常不返回完整结果给 HTTP 调用方（结果在回调里打日志）
 */
public record KafkaSendResult(
    String messageId,
    String topic,
    int partition,
    long offset
) {
}
