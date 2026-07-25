package com.demo.controller;

import com.demo.dto.OrderDTO;
import com.demo.producer.OrderedProducer;
import com.demo.producer.SimpleProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.web.bind.annotation.*;

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
     */
    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestBody OrderDTO orderDTO) {
        orderDTO.setOrderId(UUID.randomUUID().toString().substring(0, 8));
        orderDTO.setStatus("CREATED");

        log.info("接收创建订单请求 orderId={}", orderDTO.getOrderId());

        SendResult result = simpleProducer.sendSync(orderDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("orderId", orderDTO.getOrderId());
        response.put("msgId", result.getMsgId());
        response.put("message", "订单创建成功，消息已发送");

        return response;
    }

    /**
     * 场景2：异步发送
     */
    @PostMapping("/create-async")
    public Map<String, Object> createOrderAsync(@RequestBody OrderDTO orderDTO) {
        orderDTO.setOrderId(UUID.randomUUID().toString().substring(0, 8));
        orderDTO.setStatus("CREATED");

        log.info("🛒 接收异步创建订单请求 orderId={}", orderDTO.getOrderId());

        simpleProducer.sendAsync(orderDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("orderId", orderDTO.getOrderId());
        response.put("message", "订单创建请求已提交，异步处理中");

        return response;
    }

    /**
     * 场景3：更新订单状态（顺序消息）
     *
     * ORDER_ID="order-123"
     * curl -X POST "http://localhost:8080/order/${ORDER_ID}/status?status=CREATED"
     * curl -X POST "http://localhost:8080/order/${ORDER_ID}/status?status=PAID"
     * curl -X POST "http://localhost:8080/order/${ORDER_ID}/status?status=SHIPPED"
     * curl -X POST "http://localhost:8080/order/${ORDER_ID}/status?status=COMPLETED"
     */
    @PostMapping("/{orderId}/status")
    public Map<String, Object> updateStatus(@PathVariable String orderId,
                                           @RequestParam String status) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId(orderId);
        orderDTO.setStatus(status);

        log.info("更新订单状态 orderId={} status={}", orderId, status);

        // 用 orderId 作为 hashKey，保证同一订单的消息顺序
        SendResult result = orderedProducer.sendOrderly(orderDTO, orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("orderId", orderId);
        response.put("newStatus", status);
        response.put("msgId", result.getMsgId());
        response.put("message", "状态更新消息已发送");

        return response;
    }

}
