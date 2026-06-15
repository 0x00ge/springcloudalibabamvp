# 秒杀功能详细流程文档

## 📚 文档说明

本文档详细描述秒杀系统的完整调用链路，包括：
- **每个涉及的文件路径**
- **每个函数的功能说明**
- **每一步的详细流程**
- **数据流转过程**
- **异常处理机制**

适用于：开发人员深入理解系统、新人快速上手、问题排查定位。

---

## 📊 架构概览

```
用户请求
    ↓
网关（Gateway）- 鉴权、透传 userId
    ↓
订单服务（OrderController）- 接收请求
    ↓
【异步模式】
    ├─ 生产者（OrderProducerService）- 发送消息
    │       ↓
    │   RocketMQ（order-events）
    │       ↓
    ├─ 消费者（OrderEventConsumer）- 异步处理
    │       ↓
    │   ├─ 幂等性检查（3层防护）
    │   ├─ 库存扣减（Feign 调用 goods 服务）
    │   ├─ 订单落库（OrderTxService）
    │   └─ 缓存结果（Redis）
    │
    └─ 前端轮询（/order/result）- 查询结果
```

---

## 🎯 核心流程图

```
┌────────────────────────────────────────────────────────────┐
│                      用户发起秒杀请求                         │
│                  POST /order/submit                         │
│                  Header: X-User-Id                          │
│                  Body: {goodsId, buyCount}                  │
└──────────────────────┬─────────────────────────────────────┘
                       ↓
┌────────────────────────────────────────────────────────────┐
│  Step 1: OrderController.submit()                          │
│  文件: OrderController.java:72-120                          │
│  功能: 接收秒杀请求，快速校验，发送消息                        │
└──────────────────────┬─────────────────────────────────────┘
                       ↓
        ┌──────────────┴──────────────┐
        │                             │
        ↓ 第1步：快速校验               ↓
┌──────────────────┐          ┌──────────────────┐
│ loadAndValidate  │          │ Feign 调用        │
│ Goods()          │─────────→│ GoodsStockClient │
│ 125-148行        │          │ .info()          │
└──────────────────┘          └──────────────────┘
        │                             │
        ↓                             ↓
    校验商品                        返回商品快照
    - 存在性                        GoodsInfoDto
    - 启用状态
    - 时间窗口
    - 限购数量
        │
        ↓ 第2步：防重检查
┌──────────────────────────────────┐
│ orderService.queryResult()       │
│ 查询是否已下单                     │
│ - Redis 结果缓存                  │
│ - 数据库订单表                     │
└──────────┬───────────────────────┘
          │
          ↓ 已存在？
     ┌────┴────┐
     │ YES     │ NO
     ↓         ↓
   返回现有   第3步：发送消息
   结果       │
              ↓
    ┌─────────────────────────────┐
    │ producerService             │
    │ .sendOrderEvent()           │
    │ 文件: OrderProducer         │
    │ ServiceImpl.java:37-67      │
    └──────────┬──────────────────┘
               ↓
    构建 OrderEventMessage
    - messageId (UUID)
    - businessKey (userId#goodsId)
    - payload (用户ID、商品快照、数量)
               ↓
    ┌─────────────────────────────┐
    │ RocketMQTemplate.syncSend() │
    │ Topic: order-events         │
    │ Tag: order.placed           │
    └──────────┬──────────────────┘
               ↓
    第4步：立即返回"排队中"
    └→ OrderResultDto(status=0)

═══════════════════════════════════════════════════════════════
                    【异步处理阶段】
═══════════════════════════════════════════════════════════════

┌────────────────────────────────────────────────────────────┐
│  RocketMQ 消息队列                                           │
│  Topic: order-events                                        │
│  Consumer Group: order-consumer-group                       │
└──────────────────────┬─────────────────────────────────────┘
                       ↓
┌────────────────────────────────────────────────────────────┐
│  OrderEventConsumer.onMessage()                             │
│  文件: OrderEventConsumer.java:73-118                        │
│  功能: 监听消息，异步处理订单                                   │
└──────────────────────┬─────────────────────────────────────┘
                       ↓
        第1层防护：Redis 分布式锁
        ┌──────────────────────────┐
        │ RLock lock = redisson    │
        │ .getLock(businessKey)    │
        │ lock.tryLock(5, SECONDS) │
        └──────────┬───────────────┘
                   ↓ 获取成功
        第2层防护：消息去重表
        ┌──────────────────────────┐
        │ processedService         │
        │ .isProcessed(messageId)  │
        │ 查询: t_order_message_   │
        │       processed          │
        └──────────┬───────────────┘
                   ↓ 未处理过
        执行核心业务逻辑
        ┌──────────────────────────┐
        │ processOrder(message)    │
        │ 文件: 123-160行          │
        └──────────┬───────────────┘
                   ↓
    ┌──────────────┴──────────────┐
    │                             │
    ↓ 步骤1：预扣库存              ↓
┌────────────────┐        ┌──────────────────┐
│ deductStock()  │───────→│ Feign 调用        │
│ 165-173行      │        │ GoodsStockClient │
│                │        │ .deduct()        │
└────────┬───────┘        └──────────────────┘
         ↓                        ↓
    扣减成功？              调用 goods 服务
    NO → 缓存失败结果       /goods/stock/{id}/deduct
         return            Redis: seckill:stock:{id}
         │                 DECR 原子操作
         ↓
    YES → 步骤2：订单落库
    ┌──────────────────────────────┐
    │ orderTxService.createOrder() │
    │ 文件: OrderTxServiceImpl     │
    │       .java:42-62            │
    │ 事务: @Transactional         │
    └──────────┬───────────────────┘
               ↓
    构建订单对象
    - id: UUIDv7 自动生成
    - userId, goodsId
    - buyCount, amount
    - status: 0 (待支付)
               ↓
    ┌──────────────────────────────┐
    │ orderService.save(order)     │
    │ MyBatis-Plus 插入            │
    │ INSERT INTO t_order          │
    └──────────┬───────────────────┘
               ↓
    第3层防护：唯一索引
    uk_order_user_goods(user_id, goods_id)
               ↓
         ┌─────┴─────┐
         │ 成功      │ 失败（DuplicateKey）
         ↓           ↓
    步骤3：       抛出异常
    缓存结果     "您已秒杀成功，请勿重复下单"
    ↓                ↓
┌────────────┐   触发事务回滚
│ cacheResult│       ↓
│ ()         │   回补库存
│ 190-194行  │   rollbackStock()
└─────┬──────┘       ↓
      ↓          缓存失败结果
  Redis KEY:        ↓
  seckill:result:   重新抛出异常
  {userId}:{goodsId}    ↓
  VALUE:        RocketMQ 自动重试
  "1||{orderId}|秒杀成功"
      ↓
  步骤4：标记已处理
  ┌──────────────────────────────┐
  │ processedService             │
  │ .markAsProcessed()           │
  │ INSERT INTO t_order_message_ │
  │ processed                    │
  └──────────────────────────────┘
      ↓
  解锁、返回
      ↓
═════════════════════════════════
    消息处理完成
═════════════════════════════════

┌────────────────────────────────────────────────────────────┐
│                    【前端轮询查询】                           │
└────────────────────────────────────────────────────────────┘

用户前端每隔 1-2 秒轮询
    ↓
GET /order/result?goodsId=xxx
Header: X-User-Id
    ↓
┌────────────────────────────────────────────────────────────┐
│  OrderController.result()                                   │
│  文件: OrderController.java:157-161                          │
│  功能: 查询秒杀结果                                            │
└──────────────────────┬─────────────────────────────────────┘
                       ↓
┌────────────────────────────────────────────────────────────┐
│  orderService.queryResult(userId, goodsId)                  │
│  文件: OrderServiceImpl.java:138-163                         │
│  功能: 三层查询策略                                            │
└──────────────────────┬─────────────────────────────────────┘
                       ↓
    第1层：Redis 结果缓存
    ┌──────────────────────────────┐
    │ redissonClient.getBucket()   │
    │ KEY: seckill:result:         │
    │      {userId}:{goodsId}      │
    └──────────┬───────────────────┘
               ↓
         ┌─────┴─────┐
         │ 命中      │ 未命中
         ↓           ↓
    解码返回      第2层：查订单表
    decodeResult()   ↓
         │       ┌──────────────────┐
         │       │ SELECT * FROM    │
         │       │ t_order WHERE    │
         │       │ user_id=? AND    │
         │       │ goods_id=?       │
         │       └────┬─────────────┘
         │            ↓
         │      ┌─────┴─────┐
         │      │ 存在      │ 不存在
         │      ↓           ↓
         │   返回成功    第3层：返回排队中
         │   status=1   status=0
         │      │           │
         └──────┴───────────┘
                ↓
    返回给前端（3种状态）
    ┌──────────────────────────────┐
    │ status=0: 排队中              │
    │ status=1: 成功（含订单ID）     │
    │ status=2: 失败（含失败原因）   │
    └──────────────────────────────┘

```

---

## 📁 涉及的所有文件清单

### 1. 控制层（Controller）

| 文件 | 路径 | 核心方法 |
|-----|------|---------|
| **OrderController.java** | `services/service-order-0/src/main/java/com/mvp/order/controller/` | `submit()`, `result()`, `loadAndValidateGoods()` |

### 2. 服务层（Service）

| 文件 | 路径 | 核心方法 |
|-----|------|---------|
| **OrderService.java** | `.../service/` | `doSeckill()`, `queryResult()` |
| **OrderServiceImpl.java** | `.../service/impl/` | `doSeckill()`, `queryResult()`, `loadAndValidateGoods()` |
| **OrderProducerService.java** | `.../service/` | `sendOrderEvent()` |
| **OrderProducerServiceImpl.java** | `.../service/impl/` | `sendOrderEvent()`, `buildMessage()` |
| **OrderTxService.java** | `.../service/` | `createOrder()` |
| **OrderTxServiceImpl.java** | `.../service/impl/` | `createOrder()` |
| **OrderMessageProcessedService.java** | `.../service/` | `isProcessed()`, `markAsProcessed()` |
| **OrderMessageProcessedServiceImpl.java** | `.../service/impl/` | 实现幂等性检查 |

### 3. 消费者（Consumer）

| 文件 | 路径 | 核心方法 |
|-----|------|---------|
| **OrderEventConsumer.java** | `.../consumer/` | `onMessage()`, `processOrder()`, `deductStock()`, `rollbackStock()`, `cacheResult()` |
| **OrderDLQConsumer.java** | `.../consumer/` | `onMessage()` (处理死信队列) |

### 4. Feign 客户端（Remote Call）

| 文件 | 路径 | 核心方法 |
|-----|------|---------|
| **GoodsStockClient.java** | `.../feign/` | `info()`, `deduct()`, `rollback()` |

### 5. 数据传输对象（DTO）

| 文件 | 路径 | 说明 |
|-----|------|------|
| **OrderRequestDto.java** | `.../dto/` | 下单请求参数 |
| **OrderResultDto.java** | `.../dto/` | 下单结果 |
| **OrderEventMessage.java** | `.../dto/` | RocketMQ 消息体 |
| **GoodsInfoDto.java** | `.../dto/` | 商品信息 |

### 6. 实体类（Entity）

| 文件 | 路径 | 说明 |
|-----|------|------|
| **Order.java** | `.../entity/` | 订单实体 |
| **OrderMessageProcessed.java** | `.../entity/` | 消息去重实体 |

### 7. 数据访问层（Mapper）

| 文件 | 路径 | 说明 |
|-----|------|------|
| **OrderMapper.java** | `.../mapper/` | 订单 Mapper 接口 |
| **OrderMapper.xml** | `.../resources/mapper/` | 订单 Mapper XML |
| **OrderMessageProcessedMapper.java** | `.../mapper/` | 去重表 Mapper 接口 |
| **OrderMessageProcessedMapper.xml** | `.../resources/mapper/` | 去重表 Mapper XML |

### 8. 配置文件

| 文件 | 路径 | 说明 |
|-----|------|------|
| **application.yml** | `.../resources/` | 服务配置（数据库、Redis、RocketMQ） |
| **pom.xml** | `services/service-order-0/` | Maven 依赖 |

---

## 🔍 核心函数详细说明

由于内容过长，我将分成多个部分继续编写...
