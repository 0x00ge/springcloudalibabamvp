# 秒杀功能 - 核心函数详细说明（第3部分）

## 🔍 Part 3: 消费者 - OrderEventConsumer

### 3.1 onMessage() - 消息监听入口

**文件位置**：`OrderEventConsumer.java:73-118`

**注解配置**：
```java
@RocketMQMessageListener(
    topic = "order-events",              // 监听的 Topic
    consumerGroup = "order-consumer-group",  // 消费者组
    selectorExpression = "order.placed"  // Tag 过滤
)
```

**函数签名**：
```java
@Override
public void onMessage(OrderEventMessage message)
```

**参数说明**：
- `message`：RocketMQ 消息体，已自动反序列化为 `OrderEventMessage` 对象

**返回值**：无
- 正常返回：RocketMQ 认为消息处理成功，提交 offset
- 抛出异常：RocketMQ 触发自动重试机制

**执行流程**（三层防护 + 核心业务）：

#### 第1层防护：Redis 分布式锁（第80-88行）
```java
String lockKey = LOCK_KEY_PREFIX + businessKey;  // "order:processing:{userId}#{goodsId}"
RLock lock = redissonClient.getLock(lockKey);

if (!lock.tryLock(5, TimeUnit.SECONDS)) {
    log.warn("获取分布式锁失败，消息可能正在被其他消费者处理");
    return;  // 直接返回，不抛异常
}
```

**作用**：防止并发重复处理
- 场景：消息重投、多消费者实例
- 锁超时：5 秒
- 获取失败：直接返回成功（其他消费者正在处理）

#### 第2层防护：消息去重表检查（第92-95行）
```java
if (processedService.isProcessed(messageId)) {
    log.info("消息已处理过，直接返回 messageId={}", messageId);
    return;
}
```

调用 `OrderMessageProcessedService.isProcessed()` 查询 `t_order_message_processed` 表：
```sql
SELECT COUNT(1) FROM t_order_message_processed WHERE message_id = ?
```

**作用**：防止消息重投导致重复处理
- 场景：消费者重启、网络波动导致 RocketMQ 重投消息
- 查询依赖唯一索引 `uk_message_id`

#### 核心业务逻辑（第98行）
```java
processOrder(message);
```

调用私有方法 `processOrder()` 执行订单处理（详见 3.2）

#### 标记消息已处理（第101行）
```java
processedService.markAsProcessed(messageId, businessKey);
```

插入去重表记录：
```sql
INSERT INTO t_order_message_processed 
(id, message_id, business_key, processed_at, created_at)
VALUES (?, ?, ?, NOW(), NOW())
```

**主键生成**：UUIDv7（MyBatis-Plus 自动填充）

#### 释放锁（第106行）
```java
lock.unlock();
```

在 `finally` 块中执行，确保锁一定被释放。

#### 异常处理

**中断异常**（第109-112行）：
```java
catch (InterruptedException ex) {
    Thread.currentThread().interrupt();
    throw new RuntimeException("消息处理被中断", ex);
}
```

**其他异常**（第114-117行）：
```java
catch (Exception ex) {
    log.error("订单事件处理失败，将触发重试 messageId={}", messageId, ex);
    throw ex;  // 重新抛出，触发 RocketMQ 重试
}
```

**RocketMQ 重试机制**：
- 重试间隔：10s → 30s → 1m → 2m → ... （最大 2h）
- 最大重试次数：16 次（默认）
- 超过次数：进入死信队列 `%DLQ%order-consumer-group`

---

### 3.2 processOrder() - 核心业务逻辑

**文件位置**：`OrderEventConsumer.java:123-160`

**函数签名**：
```java
private void processOrder(OrderEventMessage message)
```

**参数说明**：
- `message`：订单事件消息

**返回值**：无（通过异常表示失败）

**执行流程**（共4步）：

#### Step 1：解析消息内容（第124-131行）
```java
OrderEventMessage.OrderPlacedPayload payload = message.getPayload();
String userId = payload.getUserId();
String goodsId = payload.getGoodsId();
int buyCount = payload.getBuyCount();
OrderEventMessage.GoodsSnapshot snapshot = payload.getGoodsSnapshot();

// 转换商品快照
GoodsInfoDto goods = convertToGoodsInfo(snapshot);
```

调用 `convertToGoodsInfo()` 将消息中的 `GoodsSnapshot` 转换为 `GoodsInfoDto`（详见 3.5）

#### Step 2：预扣库存（第135-140行）
```java
if (!deductStock(goodsId, buyCount)) {
    String errorMsg = "库存不足，无法下单";
    cacheResult(userId, goodsId, buildFailResult(errorMsg));
    log.warn("订单处理失败：{} userId={} goodsId={}", errorMsg, userId, goodsId);
    return;  // 库存不足，直接返回（不重试）
}
```

调用 `deductStock()` 通过 Feign 调用 goods 服务扣减库存（详见 3.3）

**失败处理**：
- 缓存失败结果到 Redis
- 直接返回（不抛异常）
- RocketMQ 认为消息处理成功，不再重试

**为什么不重试？**
- 库存不足是业务失败，不是系统故障
- 重试也无法成功（库存已售罄）
- 避免无效重试占用资源

#### Step 3：订单落库（第143行）
```java
OrderResultDto result = orderTxService.createOrder(userId, goods, buyCount);
```

调用 `OrderTxService.createOrder()` 事务性插入订单（详见 Part 4）

**事务保障**：
- `@Transactional(rollbackFor = Exception.class)`
- 唯一索引冲突触发回滚

#### Step 4：缓存成功结果（第146行）
```java
cacheResult(userId, goodsId, result);
```

将结果写入 Redis（详见 3.4），供前端轮询查询。

#### 异常处理（第150-159行）
```java
catch (Exception ex) {
    // 回补库存
    rollbackStock(goodsId, buyCount);
    
    // 缓存失败结果
    cacheResult(userId, goodsId, buildFailResult(ex.getMessage()));
    
    log.error("订单落库失败并已回补库存 userId={} goodsId={}", userId, goodsId, ex);
    throw ex;  // 重新抛出
}
```

**补偿机制**：
1. 回补库存（Feign 调用 `rollback` 接口）
2. 缓存失败结果
3. 重新抛出异常（触发 RocketMQ 重试）

**为什么要重试？**
- 订单落库失败可能是临时故障（数据库连接超时、锁冲突）
- 重试可能成功
- 已回补库存，重试不会导致库存不一致

---

### 3.3 deductStock() - 预扣库存

**文件位置**：`OrderEventConsumer.java:165-173`

**函数签名**：
```java
private boolean deductStock(String goodsId, int buyCount)
```

**参数说明**：
- `goodsId`：商品ID
- `buyCount`：扣减数量

**返回值**：
- `true`：扣减成功
- `false`：扣减失败（库存不足或调用失败）

**执行流程**：
```java
try {
    ResultVO<Boolean> resp = goodsStockClient.deduct(goodsId, buyCount);
    return resp != null && resp.isSuccess() && Boolean.TRUE.equals(resp.getData());
} catch (Exception ex) {
    log.error("预扣库存调用失败 goodsId={} buyCount={}", goodsId, buyCount, ex);
    return false;
}
```

**Feign 调用**：
- 接口：`POST http://service-goods-0/goods/stock/{id}/deduct?count={buyCount}`
- 超时：默认 1 秒（Feign 配置）
- 重试：默认不重试（避免重复扣库存）

**goods 服务处理逻辑**（位于 `service-goods-0`）：
```java
// Redis 原子操作
Long remaining = redisTemplate.opsForValue()
    .decrement("seckill:stock:" + goodsId, buyCount);

if (remaining != null && remaining >= 0) {
    return true;  // 扣减成功
} else {
    // 回滚
    redisTemplate.opsForValue().increment("seckill:stock:" + goodsId, buyCount);
    return false;  // 库存不足
}
```

**异常情况**：
- 网络超时：返回 `false`
- goods 服务宕机：返回 `false`
- Redis 不可用：返回 `false`

---

### 3.4 cacheResult() - 缓存结果

**文件位置**：`OrderEventConsumer.java:190-194`

**函数签名**：
```java
private void cacheResult(String userId, String goodsId, OrderResultDto result)
```

**参数说明**：
- `userId`：用户ID
- `goodsId`：商品ID
- `result`：处理结果

**执行流程**：
```java
String resultKey = RESULT_KEY_PREFIX + userId + ":" + goodsId;
String encodedResult = encodeResult(result);
redissonClient.getBucket(resultKey).set(encodedResult, Duration.ofHours(2));
```

**Redis Key**：`seckill:result:{userId}:{goodsId}`

**编码格式**（第232-237行）：
```java
private String encodeResult(OrderResultDto result) {
    return String.join("|",
        String.valueOf(result.getStatus()),           // 状态：0/1/2
        result.getRequestNo() != null ? result.getRequestNo() : "",
        result.getOrderId() != null ? result.getOrderId() : "",
        result.getMessage() != null ? result.getMessage() : "");
}
```

**示例**：
- 成功：`"1||01900000000000000000000000000123|秒杀成功"`
- 失败：`"2|||库存不足，无法下单"`
- 排队：`"0|||排队中"`

**TTL**：2 小时
- 覆盖前端轮询窗口
- 避免 Redis 内存无限增长

**为什么用竖线分隔？**
- 简单高效，无需 JSON 序列化
- 节省 Redis 内存
- 解析性能更好

---

### 3.5 convertToGoodsInfo() - 转换商品快照

**文件位置**：`OrderEventConsumer.java:209-227`

**函数签名**：
```java
private GoodsInfoDto convertToGoodsInfo(OrderEventMessage.GoodsSnapshot snapshot)
```

**参数说明**：
- `snapshot`：消息中的商品快照

**返回值**：
- `GoodsInfoDto`：标准的商品信息对象

**执行流程**：
```java
GoodsInfoDto goods = new GoodsInfoDto();
goods.setId(snapshot.getId());
goods.setName(snapshot.getName());
goods.setSeckillPrice(snapshot.getSeckillPrice());
goods.setTotalStock(snapshot.getTotalStock());
goods.setLimitPerUser(snapshot.getLimitPerUser());

// LocalDateTime 转 Date
goods.setStartTime(snapshot.getStartTime() != null
    ? Date.from(snapshot.getStartTime().atZone(ZoneId.systemDefault()).toInstant())
    : null);
goods.setEndTime(snapshot.getEndTime() != null
    ? Date.from(snapshot.getEndTime().atZone(ZoneId.systemDefault()).toInstant())
    : null);

goods.setStatus(snapshot.getStatus());
return goods;
```

**为什么需要转换？**
- 消息中使用 `LocalDateTime`（Java 8 时间 API）
- Service 层使用 `Date`（兼容旧代码）
- 类型不匹配需要转换

---

### 3.6 rollbackStock() - 回补库存

**文件位置**：`OrderEventConsumer.java:178-185`

**函数签名**：
```java
private void rollbackStock(String goodsId, int buyCount)
```

**参数说明**：
- `goodsId`：商品ID
- `buyCount`：回补数量

**返回值**：无

**执行流程**：
```java
try {
    goodsStockClient.rollback(goodsId, buyCount);
    log.info("库存回补成功 goodsId={} buyCount={}", goodsId, buyCount);
} catch (Exception ex) {
    log.error("库存回补失败 goodsId={} buyCount={}", goodsId, buyCount, ex);
    // 不抛异常，只记录日志
}
```

**Feign 调用**：
- 接口：`POST http://service-goods-0/goods/stock/{id}/rollback?count={buyCount}`

**goods 服务处理逻辑**：
```java
// Redis 原子操作
redisTemplate.opsForValue().increment("seckill:stock:" + goodsId, buyCount);
```

**回补失败处理**：
- 只记录错误日志
- 不抛异常（避免掩盖原始失败原因）
- 依赖人工介入或定时对账任务修复

---

**本文档共5部分，当前已完成第3部分。下一部分将详细讲解事务服务和查询逻辑。**
