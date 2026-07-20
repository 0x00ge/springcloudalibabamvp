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

/**
 * Kafka Demo HTTP 入口
 *
 * ============================================
 * 提供三个演示场景
 * ============================================
 * 1. POST /order/create        简单消息 · 同步发送
 * 2. POST /order/create-async  简单消息 · 异步发送
 * 3. POST /order/{id}/status   顺序消息 · 同一 orderId 作 key
 *
 * ============================================
 * 推荐体验顺序
 * ============================================
 * 1. 打开 http://localhost:8091/order/ 看 curl 示例
 * 2. 调 create，观察控制台：SimpleProducer 发送 + SimpleConsumer 消费
 * 3. 调 create-async，对比「先返回接口、后出现发送成功日志」
 * 4. 固定 ORDER_ID 连续改 status，观察 partition 不变、offset 递增
 * 5.（可选）在 SimpleConsumer 里临时抛异常，观察重试与 DLT 日志
 *
 * ============================================
 * 依赖组件
 * ============================================
 * - SimpleProducer  → demo-order-created
 * - OrderedProducer → demo-order-status-changed
 * - 消费者与错误处理见 consumer 包与 KafkaConfig
 */
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
     * ============================================
     * 行为说明
     * ============================================
     * - 补齐 orderId、status=CREATED
     * - 同步等待 Kafka 写入成功后再返回 JSON（含 partition/offset）
     * - 适合对照日志：HTTP 返回时消息一定已进 Broker（在 acks 语义下）
     *
     * 示例：
     * <pre>
     * curl -X POST http://localhost:8091/order/create \
     *   -H "Content-Type: application/json" \
     *   -d '{"userId":"user001","productName":"iPhone 15","quantity":1,"amount":5999.00}'
     * </pre>
     */
    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestBody OrderDTO orderDTO) {
        // Demo 自动生成短 orderId，真实系统一般由订单服务发号
        orderDTO.setOrderId(UUID.randomUUID().toString().substring(0, 8));
        orderDTO.setStatus("CREATED");

        log.info("接收 Kafka 创建订单请求 orderId={}", orderDTO.getOrderId());

        // 阻塞直到 Broker 确认或超时失败
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
     * 场景2：创建订单（简单消息 - 异步发送）
     *
     * ============================================
     * 行为说明
     * ============================================
     * - 只把发送请求交给 KafkaTemplate，不在此等待 Broker ack
     * - HTTP 快速返回；真正成功/失败看日志里的 whenComplete 回调
     * - 适合演示「接口延迟」与「投递确认」分离
     *
     * 示例：
     * <pre>
     * curl -X POST http://localhost:8091/order/create-async \
     *   -H "Content-Type: application/json" \
     *   -d '{"userId":"user002","productName":"MacBook Pro","quantity":1,"amount":12999.00}'
     * </pre>
     */
    @PostMapping("/create-async")
    public Map<String, Object> createOrderAsync(@RequestBody OrderDTO orderDTO) {
        orderDTO.setOrderId(UUID.randomUUID().toString().substring(0, 8));
        orderDTO.setStatus("CREATED");

        log.info("接收 Kafka 异步创建订单请求 orderId={}", orderDTO.getOrderId());

        // 非阻塞发送；结果在回调中打印
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
     * ============================================
     * 行为说明
     * ============================================
     * - path 中的 orderId 同时作为 Kafka message key
     * - 同一 orderId 多次调用应进入同一 partition，便于验证有序
     * - 建议固定 ORDER_ID，按 CREATED → PAID → SHIPPED → COMPLETED 依次调用
     *
     * 示例：
     * <pre>
     * ORDER_ID="order-123"
     * curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=CREATED"
     * curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=PAID"
     * curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=SHIPPED"
     * curl -X POST "http://localhost:8091/order/${ORDER_ID}/status?status=COMPLETED"
     * </pre>
     *
     * @param orderId 订单 ID（也是分区键）
     * @param status  目标状态
     */
    @PostMapping("/{orderId}/status")
    public Map<String, Object> updateStatus(@PathVariable String orderId,
                                            @RequestParam String status) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderId(orderId);
        orderDTO.setStatus(status);

        log.info("更新 Kafka 订单状态 orderId={} status={}", orderId, status);

        // key = orderId，保证同订单同分区
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
     * 首页：返回简易 HTML，内嵌 curl 示例，方便浏览器直接打开演示。
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
