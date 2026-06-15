# 秒杀功能 - 数据流转与异常处理（第5部分）

## 📊 Part 6: 完整数据流转图

### 6.1 正常流程数据流转

```
┌─────────────────────────────────────────────────────────────┐
│  阶段1：用户请求（Controller 层）                              │
└─────────────────────────────────────────────────────────────┘

用户请求数据：
{
  "goodsId": "019000...456",
  "buyCount": 1
}
Header: X-User-Id: "019000...123"

    ↓ OrderController.submit()
    
验证后的数据：
- userId: "019000...123"
- goodsId: "019000...456"
- buyCount: 1
- goods: GoodsInfoDto {
    id: "019000...456",
    name: "iPhone 15",
    seckillPrice: 4999.00,
    limitPerUser: 1,
    ...
  }

┌─────────────────────────────────────────────────────────────┐
│  阶段2：消息构建（Producer 层）                                │
└─────────────────────────────────────────────────────────────┘

    ↓ OrderProducerServiceImpl.buildMessage()

构建的消息：
OrderEventMessage {
  messageId: "550e8400-e29b-41d4-a716-446655440000",
  businessKey: "019000...123#019000...456",
  eventType: "order.placed",
  timestamp: 1750000000000,
  version: 1,
  payload: {
    userId: "019000...123",
    goodsId: "019000...456",
    buyCount: 1,
    goodsSnapshot: {
      id: "019000...456",
      name: "iPhone 15",
      seckillPrice: 4999.00,
      totalStock: 1000,
      limitPerUser: 1,
      startTime: "2026-06-15T10:00:00",
      endTime: "2026-06-15T23:59:59",
      status: 1
    }
  }
}

    ↓ RocketMQTemplate.syncSend()

┌─────────────────────────────────────────────────────────────┐
│  阶段3：RocketMQ 传输                                          │
└─────────────────────────────────────────────────────────────┘

消息属性：
- Topic: "order-events"
- Tag: "order.placed"
- Keys: messageId
- Headers:
    - messageId: "550e8400..."
    - businessKey: "019000...123#019000...456"

消息体：JSON 序列化后的 OrderEventMessage

    ↓ RocketMQ Broker 持久化
    ↓ Push 给 Consumer

┌─────────────────────────────────────────────────────────────┐
│  阶段4：消费者处理（Consumer 层）                              │
└─────────────────────────────────────────────────────────────┘

    ↓ OrderEventConsumer.onMessage()
    
第1层防护：Redis 分布式锁
- Key: "order:processing:019000...123#019000...456"
- Value: "1"
- Expire: 5 seconds

第2层防护：消息去重表查询
- SQL: SELECT COUNT(1) FROM t_order_message_processed 
        WHERE message_id = '550e8400...'
- Result: 0（未处理过）

    ↓ processOrder()

预扣库存 Feign 调用：
- Request: POST /goods/stock/019000...456/deduct?count=1
- Response: {"code":200,"data":true}

    ↓ orderTxService.createOrder()

┌─────────────────────────────────────────────────────────────┐
│  阶段5：订单落库（Transaction 层）                             │
└─────────────────────────────────────────────────────────────┘

组装订单对象：
Order {
  id: null,  // MyBatis-Plus 自动生成
  userId: "019000...123",
  goodsId: "019000...456",
  buyCount: 1,
  amount: 4999.00,
  status: 0,  // 待支付
  createdAt: null,  // 数据库自动填充
  updatedAt: null
}

    ↓ orderService.save(order)

执行的 SQL：
INSERT INTO t_order 
(id, user_id, goods_id, buy_count, amount, status, created_at, updated_at)
VALUES 
('019000...789', '019000...123', '019000...456', 1, 4999.00, 0, NOW(), NOW())

插入后的订单：
Order {
  id: "019000...789",  // 已生成
  userId: "019000...123",
  goodsId: "019000...456",
  buyCount: 1,
  amount: 4999.00,
  status: 0,
  createdAt: "2026-06-15 10:30:15",
  updatedAt: "2026-06-15 10:30:15"
}

    ↓ 返回结果

返回给 Consumer：
OrderResultDto {
  status: 1,  // 成功
  orderId: "019000...789",
  message: "秒杀成功"
}

┌─────────────────────────────────────────────────────────────┐
│  阶段6：缓存结果（Consumer 层）                                │
└─────────────────────────────────────────────────────────────┘

    ↓ cacheResult()

Redis 写入：
- Key: "seckill:result:019000...123:019000...456"
- Value: "1||019000...789|秒杀成功"
- TTL: 2 hours

    ↓ markAsProcessed()

去重表写入：
INSERT INTO t_order_message_processed
(id, message_id, business_key, processed_at, created_at)
VALUES
('019000...999', '550e8400...', '019000...123#019000...456', NOW(), NOW())

┌─────────────────────────────────────────────────────────────┐
│  阶段7：前端查询（Query 层）                                    │
└─────────────────────────────────────────────────────────────┘

用户轮询：
GET /order/result?goodsId=019000...456
Header: X-User-Id: 019000...123

    ↓ OrderController.result()
    ↓ orderService.queryResult()

第1层查询：Redis 缓存
- Key: "seckill:result:019000...123:019000...456"
- Value: "1||019000...789|秒杀成功"
- 命中 ✓

解码结果：
OrderResultDto {
  status: 1,
  orderId: "019000...789",
  message: "秒杀成功"
}

    ↓ 返回给前端

最终响应：
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": 1,
    "orderId": "019000...789",
    "message": "秒杀成功"
  }
}
```

---

## ⚠️ Part 7: 异常处理机制

### 7.1 Controller 层异常

#### 异常类型1：商品校验失败

**触发场景**：
- 商品不存在
- 商品未启用
- 秒杀未开始/已结束
- 超过限购数量

**处理逻辑**：
```java
catch (IllegalArgumentException ex) {
    log.warn("秒杀请求校验失败 userId={} goodsId={} reason={}", 
             userId, goodsId, ex.getMessage());
    OrderResultDto result = new OrderResultDto();
    result.setStatus(OrderResultDto.STATUS_FAIL);  // status = 2
    result.setMessage(ex.getMessage());
    return ResultVO.ok(result);  // HTTP 200，业务失败
}
```

**返回示例**：
```json
{
  "code": 200,
  "data": {
    "status": 2,
    "message": "商品未启用"
  }
}
```

---

#### 异常类型2：系统异常

**触发场景**：
- Feign 调用超时
- Redis 连接失败
- RocketMQ 发送失败（降级后同步模式失败）

**处理逻辑**：
```java
catch (Exception ex) {
    log.error("秒杀请求处理异常 userId={} goodsId={}", userId, goodsId, ex);
    return ResultVO.fail("系统异常，请稍后重试");  // HTTP 200，code = 500
}
```

**返回示例**：
```json
{
  "code": 500,
  "message": "系统异常，请稍后重试"
}
```

---

### 7.2 Producer 层异常

#### 异常类型：RocketMQ 发送失败

**触发场景**：
- RocketMQ NameServer 不可用
- 网络超时
- 消息体过大

**处理逻辑**：
```java
catch (Exception ex) {
    log.error("订单事件发送失败 userId={} goodsId={}", userId, requestDto.getGoodsId(), ex);
    return false;  // 返回 false
}
```

**Controller 降级处理**：
```java
if (!sent) {
    log.warn("消息发送失败，降级到同步处理");
    return ResultVO.ok(orderService.doSeckill(userId, requestDto));
}
```

**保障**：发送失败自动降级到同步模式，不丢失订单

---

### 7.3 Consumer 层异常

#### 异常类型1：获取分布式锁失败

**触发场景**：
- 其他消费者正在处理
- Redis 宕机

**处理逻辑**：
```java
if (!lock.tryLock(5, TimeUnit.SECONDS)) {
    log.warn("获取分布式锁失败，消息可能正在被其他消费者处理");
    return;  // 直接返回成功
}
```

**RocketMQ 处理**：消息标记为消费成功，不再重试

**风险**：如果锁超时，可能导致消息丢失
**缓解**：第2层（消息去重表）和第3层（唯一索引）兜底

---

#### 异常类型2：消息已处理

**触发场景**：
- 消息重投
- 消费者重启后重新消费

**处理逻辑**：
```java
if (processedService.isProcessed(messageId)) {
    log.info("消息已处理过，直接返回");
    return;  // 直接返回成功
}
```

**RocketMQ 处理**：消息标记为消费成功，不再重试

---

#### 异常类型3：库存不足

**触发场景**：
- Redis 库存已扣完
- goods 服务返回 `false`

**处理逻辑**：
```java
if (!deductStock(goodsId, buyCount)) {
    cacheResult(userId, goodsId, buildFailResult("库存不足"));
    return;  // 直接返回成功
}
```

**关键点**：
- 缓存失败结果（前端能查询到）
- 直接返回（不抛异常）
- 不触发 RocketMQ 重试

---

#### 异常类型4：订单落库失败

**触发场景**：
- 数据库连接超时
- 唯一索引冲突
- 事务死锁

**处理逻辑**：
```java
catch (Exception ex) {
    // 1. 回补库存
    rollbackStock(goodsId, buyCount);
    
    // 2. 缓存失败结果
    cacheResult(userId, goodsId, buildFailResult(ex.getMessage()));
    
    // 3. 重新抛出异常
    throw ex;
}
```

**RocketMQ 处理**：
- 消息标记为消费失败
- 触发自动重试（最多16次）
- 超过次数进入死信队列

**重试间隔**：
```
第1次：10s
第2次：30s
第3次：1m
第4次：2m
第5次：3m
...
第16次：2h
```

---

### 7.4 Transaction 层异常

#### 异常类型：唯一索引冲突

**触发场景**：
- 同一用户对同一商品重复下单
- 三层防护未拦截（极端情况）

**处理逻辑**：
```java
try {
    orderService.save(order);
} catch (DuplicateKeyException ex) {
    throw new IllegalArgumentException("您已秒杀成功，请勿重复下单", ex);
}
```

**事务回滚**：
- `@Transactional` 捕获异常
- 回滚整个事务
- 订单不会插入

**Consumer 层处理**：
- 捕获 `IllegalArgumentException`
- 回补库存
- 缓存失败结果
- 重新抛出（触发重试）

---

## 🔄 Part 8: 异常恢复流程

### 8.1 库存不一致修复

**场景**：回补库存失败

**检测方法**：
```sql
-- 统计订单数量
SELECT COUNT(*) FROM t_order WHERE goods_id = ?;

-- 查询 Redis 库存
GET seckill:stock:{goodsId}

-- 对比差异
数据库订单数 + Redis 剩余库存 != 商品总库存
```

**修复方案**：
```java
@Scheduled(cron = "0 0 4 * * ?")  // 每天凌晨4点
public void fixStockInconsistency() {
    List<Goods> goods = goodsService.list();
    
    for (Goods g : goods) {
        long orderCount = orderService.count(
            Wrappers.<Order>lambdaQuery().eq(Order::getGoodsId, g.getId())
        );
        Long redisStock = redisTemplate.opsForValue()
            .get("seckill:stock:" + g.getId());
        
        long totalUsed = orderCount + (redisStock == null ? 0 : redisStock);
        
        if (totalUsed != g.getTotalStock()) {
            log.warn("库存不一致 goodsId={} 订单数={} Redis库存={} 总库存=",
                     g.getId(), orderCount, redisStock, g.getTotalStock());
            
            // 修复：以订单为准
            long correctStock = g.getTotalStock() - orderCount;
            redisTemplate.opsForValue().set(
                "seckill:stock:" + g.getId(), correctStock
            );
            log.info("库存已修复 goodsId={} 修复后库存={}", g.getId(), correctStock);
        }
    }
}
```

---

### 8.2 死信队列处理

**场景**：消息重试16次仍失败

**检测方法**：
```java
@Component
public class OrderDLQConsumer implements RocketMQListener<OrderEventMessage> {
    
    @Override
    public void onMessage(OrderEventMessage message) {
        log.error("【死信队列】订单事件进入死信队列，需要人工处理！messageId={} businessKey={}",
                  message.getMessageId(),
                  message.getBusinessKey());
        
        // 保存到数据库
        saveToDLQTable(message);
        
        // 发送告警
        sendAlert(message);
    }
}
```

**人工处理流程**：
1. 查看 DLQ 表：`SELECT * FROM t_order_dlq_message;`
2. 分析失败原因（查看日志）
3. 修复问题（修复数据、重启服务）
4. 手动重投消息：
```java
@PostMapping("/admin/dlq/retry")
public ResultVO<Void> retryDLQMessage(@RequestParam String messageId) {
    OrderEventMessage message = dlqService.getById(messageId);
    producerService.sendOrderEvent(...);  // 重新发送
    dlqService.removeById(messageId);
    return ResultVO.ok();
}
```

---

## 📈 Part 9: 监控指标

### 9.1 核心业务指标

| 指标 | 说明 | 监控方式 | 告警阈值 |
|-----|------|---------|---------|
| **下单 QPS** | 每秒下单请求数 | Prometheus | > 10000 |
| **下单成功率** | 成功订单 / 总请求 | 日志统计 | < 95% |
| **响应时间 P99** | 99% 请求的响应时间 | Prometheus | > 200ms |
| **消息堆积量** | RocketMQ Consumer Lag | Dashboard | > 10000 |
| **消费延迟** | 消息发送到消费的时间差 | 自定义埋点 | > 5s |

---

### 9.2 异常指标

| 指标 | 说明 | 监控方式 | 告警阈值 |
|-----|------|---------|---------|
| **RocketMQ 发送失败率** | 发送失败 / 总发送 | 日志统计 | > 1% |
| **消费失败率** | 消费异常 / 总消费 | 日志统计 | > 5% |
| **死信队列消息数** | DLQ 中的消息数量 | Dashboard | > 10 |
| **库存回补失败数** | 回补失败次数 | 日志统计 | > 10/min |
| **Redis 超时次数** | Redis 调用超时 | Prometheus | > 100/min |

---

### 9.3 Grafana 监控大盘

**配置示例**：
```yaml
# Prometheus 采集配置
- job_name: 'service-order-0'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ['localhost:8500']

# 指标定义
seckill_submit_total:            # 下单总数
  counter
seckill_submit_duration_seconds: # 下单耗时
  histogram
seckill_consumer_lag:            # 消费延迟
  gauge
```

**Grafana 面板**：
1. **下单趋势**：折线图，显示每分钟下单量
2. **成功率**：饼图，显示成功/失败/排队占比
3. **响应时间**：热力图，P50/P95/P99 分位数
4. **消息堆积**：面积图，Consumer Lag 趋势
5. **异常统计**：表格，各类异常的发生次数

---

## 🎓 Part 10: 最佳实践总结

### 10.1 幂等性设计

**三层防护**：
```
第1层：Redis 分布式锁     → 防并发
第2层：消息去重表         → 防重投
第3层：数据库唯一索引     → 最终兜底
```

**关键点**：
- 每层独立工作，互不依赖
- 任意两层失效，第三层仍能保证幂等
- 性能与可靠性的平衡

---

### 10.2 异步设计

**优势**：
- 削峰：消息队列缓冲流量
- 解耦：订单服务不依赖库存服务的响应时间
- 扩展：消费者可独立水平扩展

**代价**：
- 复杂度：需要处理消息重试、幂等、顺序
- 一致性：最终一致性，不是强一致
- 调试难度：异步链路难以追踪

**适用场景**：
- 高并发、流量突增
- 对响应时间敏感
- 允许最终一致性

---

### 10.3 降级策略

**降级触发条件**：
- RocketMQ 发送失败
- RocketMQ 不可用
- 消息堆积严重

**降级方案**：
```java
if (!sent) {
    // 降级到同步模式
    return orderService.doSeckill(userId, requestDto);
}
```

**权衡**：
- 保证可用性（同步仍可用）
- 牺牲性能（响应变慢）
- 避免订单丢失

---

## 📚 文档导航

| 文档 | 说明 |
|-----|------|
| **SECKILL-DETAILED-FLOW.md** | 第1部分：架构概览、流程图 |
| **SECKILL-DETAILED-FLOW-PART2.md** | 第2部分：Controller + Producer |
| **SECKILL-DETAILED-FLOW-PART3.md** | 第3部分：Consumer 消费逻辑 |
| **SECKILL-DETAILED-FLOW-PART4.md** | 第4部分：Transaction + Query |
| **SECKILL-DETAILED-FLOW-PART5.md** | 第5部分：数据流转 + 异常处理（本文档） |

---

**全部文档已完成！** 🎉

这是一份**超详细**的秒杀功能流程文档，涵盖：
- ✅ 每个文件的路径和职责
- ✅ 每个函数的参数、返回值、执行逻辑
- ✅ 完整的数据流转过程
- ✅ 所有异常处理机制
- ✅ 监控指标和最佳实践

适合：深入学习、问题排查、新人上手。
