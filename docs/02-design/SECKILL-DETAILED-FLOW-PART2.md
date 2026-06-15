# 秒杀功能 - 核心函数详细说明（第2部分）

## 🔍 Part 1: 控制层 - OrderController

### 1.1 submit() - 发起秒杀请求

**文件位置**：`OrderController.java:71-120`

**函数签名**：
```java
@PostMapping("/submit")
public ResultVO<OrderResultDto> submit(
    @RequestHeader("X-User-Id") String userId,
    @Valid @RequestBody OrderRequestDto requestDto
)
```

**参数说明**：
- `userId`：从请求头 `X-User-Id` 获取，由网关鉴权后透传
- `requestDto`：请求体，包含 `goodsId`（商品ID）和 `buyCount`（购买数量）

**返回值**：
- `ResultVO<OrderResultDto>`：统一响应封装
  - 异步模式：立即返回 `status=0`（排队中）
  - 同步模式（降级）：返回最终结果

**执行流程**（共4步）：

#### Step 1：查询商品快照并校验（第78行）
```java
GoodsInfoDto goods = loadAndValidateGoods(goodsId, buyCount);
```

调用 `loadAndValidateGoods()` 私有方法，通过 Feign 查询商品信息并校验：
- 商品是否存在
- 是否已启用（`status=1`）
- 当前时间是否在秒杀时间窗口内
- 购买数量是否超过限购

**抛出异常**：`IllegalArgumentException`（校验失败）

#### Step 2：第一层防重检查（第82行）
```java
OrderResultDto existingResult = orderService.queryResult(userId, goodsId);
if (existingResult.getStatus() == OrderResultDto.STATUS_SUCCESS) {
    return ResultVO.ok(existingResult);  // 已成功，直接返回
}
if (existingResult.getStatus() == OrderResultDto.STATUS_FAIL) {
    return ResultVO.ok(existingResult);  // 已失败，直接返回
}
```

调用 `orderService.queryResult()` 检查：
- Redis 结果缓存（`seckill:result:{userId}:{goodsId}`）
- 数据库订单表（`t_order`）

**目的**：避免重复请求进入消息队列

#### Step 3：发送消息到 RocketMQ（第91行）
```java
boolean sent = producerService.sendOrderEvent(userId, requestDto, goods);
```

调用 `OrderProducerService.sendOrderEvent()` 将请求转换为消息并发送到 RocketMQ。

**消息内容**：
- `messageId`：UUID，全局唯一
- `businessKey`：`userId + "#" + goodsId`
- `payload`：用户ID、商品ID、购买数量、商品快照

**发送失败处理**（第93-97行）：
```java
if (!sent) {
    // 降级到同步模式
    log.warn("消息发送失败，降级到同步处理");
    return ResultVO.ok(orderService.doSeckill(userId, requestDto));
}
```

#### Step 4：立即返回"排队中"（第100-103行）
```java
OrderResultDto result = new OrderResultDto();
result.setStatus(OrderResultDto.STATUS_QUEUEING);  // status = 0
result.setMessage("排队中，请稍后查询结果");
return ResultVO.ok(result);
```

**响应时间**：< 100ms（相比同步模式的 1-2s）

**异常处理**：
- `IllegalArgumentException`：业务校验失败（商品不存在、未启用等）
  - 返回 `status=2`（失败）+ 错误信息
- 其他异常：系统异常
  - 返回 `code=500` + "系统异常，请稍后重试"

---

### 1.2 loadAndValidateGoods() - 商品校验

**文件位置**：`OrderController.java:125-148`

**函数签名**：
```java
private GoodsInfoDto loadAndValidateGoods(String goodsId, int buyCount)
```

**参数说明**：
- `goodsId`：商品ID
- `buyCount`：购买数量

**返回值**：
- `GoodsInfoDto`：商品信息快照

**执行流程**（共5项校验）：

#### 1. Feign 调用查询商品（第126行）
```java
ResultVO<GoodsInfoDto> resp = goodsStockClient.info(goodsId);
GoodsInfoDto goods = resp == null ? null : resp.getData();
```

远程调用：`GET http://service-goods-0/goods/stock/{id}/info`

#### 2. 商品存在性校验（第129行）
```java
if (goods == null) {
    throw new IllegalArgumentException("商品不存在");
}
```

#### 3. 商品启用状态校验（第132行）
```java
if (!Objects.equals(goods.getStatus(), GoodsInfoDto.STATUS_ENABLED)) {
    throw new IllegalArgumentException("商品未启用");
}
```

#### 4. 秒杀时间窗口校验（第136-143行）
```java
Date now = new Date();
if (goods.getStartTime() != null && now.before(goods.getStartTime())) {
    throw new IllegalArgumentException("秒杀尚未开始");
}
if (goods.getEndTime() != null && now.after(goods.getEndTime())) {
    throw new IllegalArgumentException("秒杀已结束");
}
```

#### 5. 限购数量校验（第144行）
```java
if (goods.getLimitPerUser() != null && buyCount > goods.getLimitPerUser()) {
    throw new IllegalArgumentException("超过限购数量");
}
```

**返回**：校验通过的商品快照

---

### 1.3 result() - 查询秒杀结果

**文件位置**：`OrderController.java:157-161`

**函数签名**：
```java
@GetMapping("/result")
public ResultVO<OrderResultDto> result(
    @RequestHeader("X-User-Id") String userId,
    @RequestParam String goodsId
)
```

**参数说明**：
- `userId`：用户ID（请求头）
- `goodsId`：商品ID（查询参数）

**返回值**：
- `OrderResultDto`：秒杀结果
  - `status=0`：排队中
  - `status=1`：成功（含 `orderId`）
  - `status=2`：失败（含失败原因）

**执行流程**：
```java
return ResultVO.ok(orderService.queryResult(userId, goodsId));
```

委托给 `OrderService.queryResult()` 处理，采用三层查询策略（详见后续章节）。

---

## 🔍 Part 2: 生产者 - OrderProducerService

### 2.1 sendOrderEvent() - 发送订单事件

**文件位置**：`OrderProducerServiceImpl.java:37-67`

**函数签名**：
```java
public boolean sendOrderEvent(
    String userId, 
    OrderRequestDto requestDto, 
    GoodsInfoDto goods
)
```

**参数说明**：
- `userId`：用户ID
- `requestDto`：下单请求参数
- `goods`：商品快照（已校验）

**返回值**：
- `true`：发送成功
- `false`：发送失败

**执行流程**（共3步）：

#### Step 1：构建消息体（第40行）
```java
OrderEventMessage message = buildMessage(userId, requestDto, goods);
```

调用私有方法 `buildMessage()` 构建消息对象。

#### Step 2：发送到 RocketMQ（第43-52行）
```java
String destination = TOPIC + ":" + TAG;  // "order-events:order.placed"

rocketMQTemplate.syncSend(
    destination,
    MessageBuilder.withPayload(message)
        .setHeader("messageId", message.getMessageId())
        .setHeader("businessKey", message.getBusinessKey())
        .build()
);
```

**发送方式**：同步发送（`syncSend`）
- 阻塞等待 Broker 确认
- 确保消息不丢失
- 超时时间：3 秒（配置在 `application.yml`）

**消息头**：
- `messageId`：消息唯一ID
- `businessKey`：业务唯一键（用于消费端幂等）

#### Step 3：记录日志（第54-58行）
```java
log.info("订单事件发送成功 messageId={} businessKey={} userId={} goodsId={}",
    message.getMessageId(),
    message.getBusinessKey(),
    userId,
    requestDto.getGoodsId());
```

**异常处理**：
```java
catch (Exception ex) {
    log.error("订单事件发送失败 userId={} goodsId=", userId, requestDto.getGoodsId(), ex);
    return false;  // 返回 false 触发降级
}
```

---

### 2.2 buildMessage() - 构建消息体

**文件位置**：`OrderProducerServiceImpl.java:72-113`

**函数签名**：
```java
private OrderEventMessage buildMessage(
    String userId, 
    OrderRequestDto requestDto, 
    GoodsInfoDto goods
)
```

**返回值**：`OrderEventMessage` - 完整的消息对象

**构建内容**：

#### 1. 消息元数据（第73-83行）
```java
message.setMessageId(UUID.randomUUID().toString());  // 全局唯一ID
message.setBusinessKey(userId + "#" + goods.getId()); // 业务唯一键
message.setEventType("order.placed");                 // 事件类型
message.setTimestamp(System.currentTimeMillis());     // 时间戳
message.setVersion(1);                                // 消息版本
```

#### 2. 业务载荷（第86-89行）
```java
OrderEventMessage.OrderPlacedPayload payload = new OrderEventMessage.OrderPlacedPayload();
payload.setUserId(userId);
payload.setGoodsId(goods.getId());
payload.setBuyCount(requestDto.getBuyCount() != null ? requestDto.getBuyCount() : 1);
```

#### 3. 商品快照（第92-107行）
```java
OrderEventMessage.GoodsSnapshot snapshot = new OrderEventMessage.GoodsSnapshot();
snapshot.setId(goods.getId());
snapshot.setName(goods.getName());
snapshot.setSeckillPrice(goods.getSeckillPrice());
snapshot.setTotalStock(goods.getTotalStock());
snapshot.setLimitPerUser(goods.getLimitPerUser());

// Date 转 LocalDateTime
snapshot.setStartTime(goods.getStartTime() != null
    ? LocalDateTime.ofInstant(goods.getStartTime().toInstant(), ZoneId.systemDefault())
    : null);
snapshot.setEndTime(goods.getEndTime() != null
    ? LocalDateTime.ofInstant(goods.getEndTime().toInstant(), ZoneId.systemDefault())
    : null);

snapshot.setStatus(goods.getStatus());
```

**为什么需要商品快照？**
- 消费时商品信息可能已变更（价格调整、状态变更）
- 快照保证按下单时的状态处理
- 避免消费端再次调用 Feign 查询商品

---

**本文档共5部分，当前已完成第2部分。下一部分将详细讲解消费者逻辑。**
