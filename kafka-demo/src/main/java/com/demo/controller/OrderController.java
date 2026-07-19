package com.demo.controller;

import com.demo.dto.OrderDTO;
import com.demo.producer.KafkaSendResult;
import com.demo.producer.OrderedProducer;
import com.demo.producer.SimpleProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    private final SimpleProducer simpleProducer;
    private final OrderedProducer orderedProducer;

    public OrderController(SimpleProducer simpleProducer,
                           OrderedProducer orderedProducer) {
        this.simpleProducer = simpleProducer;
        this.orderedProducer = orderedProducer;
    }

    /**
     * 场景1：创建订单（简单消息 - 同步发送）
     *
     * curl -X POST http://localhost:8091/order/create \
     *   -H "Content-Type: application/json" \
     *   -d '{"userId":"user001","productName":"iPhone 15","quantity":1,"amount":5999.00}'
     */
    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestBody OrderDTO orderDTO) {
        orderDTO.setOrderId(UUID.randomUUID().toString().substring(0, 8));
        orderDTO.setStatus("CREATED");

        log.info("接收 Kafka 创建订单请求 orderId={}", orderDTO.getOrderId());

        KafkaSendResult result = simpleProducer.sendSync(orderDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("orderId", orderDTO.getOrderId());
        response.put("messageId", result.messageId());
        response.put("topic", result.topic());
        response.put("partition", result.partition());
        response.put("offset", result.offset());
        response.put("message", "订单创建成功，Kafka 消息已发送");

        return response;
    }

    /**
     * 场景2：异步发送
     *
     * curl -X POST http://localhost:8091/order/create-async \
     *   -H "Content-Type: application/json" \
     *   -d '{"userId":"user002","productName":"MacBook Pro","quantity":1,"amount":12999.00}'
     */
    @PostMapping("/create-async")
    public Map<String, Object> createOrderAsync(@RequestBody OrderDTO orderDTO) {
        orderDTO.setOrderId(UUID.randomUUID().toString().substring(0, 8));
        orderDTO.setStatus("CREATED");

        log.info("接收 Kafka 异步创建订单请求 orderId={}", orderDTO.getOrderId());

        simpleProducer.sendAsync(orderDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("orderId", orderDTO.getOrderId());
        response.put("message", "订单创建请求已提交，Kafka 异步处理中");

        return response;
    }

    /**
     * 场景3：更新订单状态（顺序消息）
     *
     * ORDER_ID="order-123"
     * curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=CREATED"
     * curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=PAID"
     * curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=SHIPPED"
     * curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=COMPLETED"
     */
    @PostMapping("/{orderId}/status")
    public Map<String, Object> updateStatus(@PathVariable String orderId,
                                            @RequestParam String status) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId(orderId);
        orderDTO.setStatus(status);

        log.info("更新 Kafka 订单状态 orderId={} status={}", orderId, status);

        KafkaSendResult result = orderedProducer.sendOrderly(orderDTO, orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("orderId", orderId);
        response.put("newStatus", status);
        response.put("topic", result.topic());
        response.put("partition", result.partition());
        response.put("offset", result.offset());
        response.put("message", "状态更新消息已发送到 Kafka，同一 orderId 会进入同一 partition");

        return response;
    }

    /**
     * 首页
     */
    @GetMapping("/")
    public String index() {
        return """
            <html>
            <head><title>Kafka Demo</title></head>
            <body>
                <h1>Kafka Demo</h1>
                <h2>API 列表</h2>
                <ul>
                    <li><strong>场景1：简单消息（同步）</strong>
                        <pre>curl -X POST http://localhost:8091/order/create \\
  -H "Content-Type: application/json" \\
  -d '{"userId":"user001","productName":"iPhone 15","quantity":1,"amount":5999.00}'</pre>
                    </li>
                    <li><strong>场景2：简单消息（异步）</strong>
                        <pre>curl -X POST http://localhost:8091/order/create-async \\
  -H "Content-Type: application/json" \\
  -d '{"userId":"user002","productName":"MacBook Pro","quantity":1,"amount":12999.00}'</pre>
                    </li>
                    <li><strong>场景3：顺序消息</strong>
                        <pre>ORDER_ID="order-123"
curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=CREATED"
curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=PAID"
curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=SHIPPED"
curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=COMPLETED"</pre>
                    </li>
                </ul>
                <p>查看日志观察 Kafka 消息发送、消费和 DLT 处理过程</p>
            </body>
            </html>
            """;
    }
}
