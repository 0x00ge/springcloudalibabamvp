package com.demo.consumer;

import com.demo.config.KafkaConfig;
import com.demo.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 顺序消息消费者
 */
@Slf4j
@Component
public class OrderedConsumer {

    @KafkaListener(
        topics = KafkaConfig.ORDER_STATUS_CHANGED_TOPIC,
        groupId = "demo-status-consumer-group"
    )
    public void onMessage(ConsumerRecord<String, OrderDTO> record) {
        OrderDTO order = record.value();
        log.info("<<< kafkaOrderlyConsume : orderId={} status={} key={} partition={} offset={}",
            order.getOrderId(),
            order.getStatus(),
            record.key(),
            record.partition(),
            record.offset());

        updateOrderStatus(order);

        log.info("<<< kafkaOrderStatusUpdated : orderId={} newStatus={}",
            order.getOrderId(), order.getStatus());
    }

    private void updateOrderStatus(OrderDTO order) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("<<< kafkaUpdateDatabase : orderId={} status={}",
            order.getOrderId(), order.getStatus());
    }
}
