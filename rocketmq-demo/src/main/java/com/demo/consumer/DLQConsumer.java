package com.demo.consumer;

import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者
 *
 * ============================================
 * 什么是死信队列（Dead Letter Queue）？
 * ============================================
 * - 消息消费失败，重试 16 次后仍失败
 * - RocketMQ 自动将消息投递到死信队列
 * - Topic 命名规则：%DLQ%消费者组名
 *
 * ============================================
 * 死信队列的消息来源
 * ============================================
 * 1. 消费端抛异常，重试 16 次都失败
 * 2. 消息过期（默认3天）
 * 3. 队列满了，新消息被丢弃
 *
 * ============================================
 * 为什么需要死信队列消费者？
 * ============================================
 * - 记录失败消息，便于排查
 * - 发送告警，通知运维
 * - 提供手动重试机制
 * - 避免消息彻底丢失
 *
 * ============================================
 * 处理策略
 * ============================================
 * 1. 记录详细日志（消息内容、失败原因）
 * 2. 保存到数据库（t_dlq_message表）
 * 3. 发送告警（钉钉、邮件、短信）
 * 4. 提供后台管理界面，运维可手动重试
 *
 * ============================================
 * ⚠️ 重要提示
 * ============================================
 * - 死信队列的消费者不要抛异常！
 * - 否则死信消息会再次进入死信队列（无限循环）
 * - 即使处理失败，也要正常返回，只记录日志
 */
@Slf4j
@Component
@RocketMQMessageListener(
    // 死信队列 Topic 命名规则：%DLQ% + 原消费者组名
    topic = "%DLQ%demo-consumer-group",
    consumerGroup = "demo-dlq-consumer-group"  // 死信队列专用消费者组
)
public class DLQConsumer implements RocketMQListener<OrderDTO> {

    @Override
    public void onMessage(OrderDTO order) {
        log.error("💀 【死信队列】收到失败消息 orderId={} product= amount={}",
                  order.getOrderId(),
                  order.getProductName(),
                  order.getAmount());

        try {
            // 1. 记录详细信息
            logFailedMessage(order);

            // 2. 保存到数据库（示例代码）
            // dlqMessageService.save(order);

            // 3. 发送告警（示例代码）
            // alertService.sendDLQAlert(order);

            log.error("💀 【死信队列】消息已记录，等待人工处理 orderId={}", order.getOrderId());

        } catch (Exception e) {
            // ⚠️ 即使处理失败，也不要抛异常
            // 只记录日志，避免死信消息再次进入死信队列
            log.error("💀 【死信队列】处理异常 orderId={}", order.getOrderId(), e);
        }

        // ✅ 正常返回，确认消费成功
        // 死信消息不会再次重试
    }

    /**
     * 记录失败消息的详细信息
     */
    private void logFailedMessage(OrderDTO order) {
        String alertMessage = String.format("""

            ========================================
            ⚠️  死信队列告警
            ========================================
            订单ID：%s
            用户ID：%s
            商品：%s
            数量：%d
            金额：%.2f
            状态：%s

            原因：消费失败，重试16次后进入死信队列

            处理建议：
            1. 检查消费者代码是否有BUG
            2. 检查数据库/Redis是否正常
            3. 检查消息内容是否合法
            4. 排查后可通过管理后台手动重试
            ========================================

            """,
            order.getOrderId(),
            order.getUserId(),
            order.getProductName(),
            order.getQuantity(),
            order.getAmount(),
            order.getStatus()
        );

        log.error(alertMessage);
    }
}

/*
============================================
生产环境增强建议
============================================

1. 保存到数据库
   CREATE TABLE t_dlq_message (
       id BIGINT PRIMARY KEY AUTO_INCREMENT,
       message_id VARCHAR(64) UNIQUE NOT NULL,
       topic VARCHAR(128) NOT NULL,
       consumer_group VARCHAR(128) NOT NULL,
       message_body TEXT NOT NULL,
       retry_count INT DEFAULT 0,
       error_message TEXT,
       status TINYINT DEFAULT 0 COMMENT '0:待处理 1:已重试 2:已忽略',
       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
   );

2. 钉钉告警
   @Autowired
   private DingTalkService dingTalkService;

   dingTalkService.sendAlert(
       "死信队列告警",
       String.format("订单 %s 进入死信队列，请尽快处理！", order.getOrderId())
   );

3. 邮件通知
   @Autowired
   private EmailService emailService;

   emailService.sendToOps(
       "RocketMQ死信队列告警",
       alertMessage
   );

4. 手动重试接口
   @RestController
   @RequestMapping("/admin/dlq")
   public class DLQController {

       @PostMapping("/retry/{messageId}")
       public Result retryDLQMessage(@PathVariable String messageId) {
           // 1. 从数据库查询死信消息
           DLQMessage dlqMsg = dlqMessageService.getById(messageId);

           // 2. 重新发送到原始Topic
           OrderDTO order = JSON.parseObject(dlqMsg.getMessageBody(), OrderDTO.class);
           simpleProducer.sendSync(order);

           // 3. 更新死信记录状态
           dlqMessageService.markAsRetried(messageId);

           return Result.ok("重试成功");
       }

       @GetMapping("/list")
       public Result listDLQMessages() {
           // 查询所有待处理的死信消息
           List<DLQMessage> list = dlqMessageService.listPending();
           return Result.ok(list);
       }
   }
*/
