# RocketMQ Demo

最小化的 RocketMQ 使用示例，演示生产、消费、顺序消息和死信队列。

## 快速开始

### 1. 启动 RocketMQ（使用 Proxy 模式）

```bash
# 确保 RocketMQ 运行在 127.0.0.1:8081（Proxy gRPC 端口）
```

### 2. 启动应用

```bash
mvn spring-boot:run
```

访问：http://localhost:8080

### 3. 测试接口

#### 场景 1：简单消息（同步发送）

```bash
curl -X POST http://localhost:8080/order/create \
  -H "Content-Type: application/json" \
  -d '{"userId":"user001","productName":"iPhone 15","quantity":1,"amount":5999.00}'
```

#### 场景 2：简单消息（异步发送）

```bash
curl -X POST http://localhost:8080/order/create-async \
  -H "Content-Type: application/json" \
  -d '{"userId":"user002","productName":"MacBook Pro","quantity":1,"amount":12999.00}'
```

#### 场景 3：顺序消息

```bash
ORDER_ID="order-123"
curl -X POST "http://localhost:8080/order/${ORDER_ID}/status?status=CREATED"
curl -X POST "http://localhost:8080/order/${ORDER_ID}/status?status=PAID"
curl -X POST "http://localhost:8080/order/${ORDER_ID}/status?status=SHIPPED"
curl -X POST "http://localhost:8080/order/${ORDER_ID}/status?status=COMPLETED"
```

## 项目结构

```
rocketmq-demo/
├── producer/
│   ├── SimpleProducer.java       # 简单消息生产者（同步/异步/单向）
│   └── OrderedProducer.java      # 顺序消息生产者
├── consumer/
│   ├── SimpleConsumer.java       # 简单消息消费者
│   ├── OrderedConsumer.java      # 顺序消息消费者
│   └── DLQConsumer.java          # 死信队列消费者
└── controller/
    └── OrderController.java      # REST API
```

## 核心概念

### 1. 发送方式对比

| 方式 | 等待响应 | 可靠性 | 性能 | 适用场景 |
|------|---------|-------|------|---------|
| 同步 | 是 | 高 | 中 | 重要业务（推荐） |
| 异步 | 否 | 高 | 高 | 响应时间敏感 |
| 单向 | 否 | 低 | 最高 | 日志（不推荐） |

### 2. 消费模式对比

| 模式 | 顺序性 | 性能 | 适用场景 |
|------|-------|------|---------|
| 并发消费 | 否 | 高 | 90% 的场景 |
| 顺序消费 | 是 | 低 | 订单状态、binlog 同步 |

### 3. 重试机制

- 消费失败自动重试 16 次
- 重试间隔：10s, 30s, 1m, 2m, 3m, 4m, 5m, 6m, 7m, 8m, 9m, 10m, 20m, 30m, 1h, 2h
- 16 次都失败后进入死信队列

## 配置说明

```yaml
# application.yml
rocketmq:
  name-server: 127.0.0.1:8081  # Proxy gRPC 端口
  producer:
    group: demo-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
```

## 常见问题

### 1. 消息丢失怎么办？
- 使用同步发送（推荐）
- 检查 Broker 是否正常运行
- 查看日志确认发送结果

### 2. 消息重复消费怎么办？
- 业务逻辑做幂等性处理
- 使用数据库唯一索引
- 使用 Redis 去重

### 3. 如何保证顺序？
- 使用 `syncSendOrderly` 发送
- 相同 hashKey 的消息进入同一队列
- 消费者使用 `ConsumeMode.ORDERLY`

## 扩展阅读

查看完整的业务实现请参考：
- `services/service-order-0`：订单服务（异步秒杀）
- `docs/Goods-Order服务交互流程.md`：详细技术文档
