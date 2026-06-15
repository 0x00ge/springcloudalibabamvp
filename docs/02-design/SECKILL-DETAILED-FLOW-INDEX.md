# 秒杀功能详细流程文档 - 总索引

## 📚 文档说明

本系列文档**超详细**地描述了秒杀系统的完整实现，包括每个文件、每个函数、每一步的详细逻辑，适合：
- 🎓 **深入学习**：理解秒杀系统的设计思路
- 🔍 **问题排查**：快速定位故障点
- 👨‍💻 **新人上手**：了解代码结构和调用关系
- 📖 **面试准备**：掌握高并发系统设计

---

## 📖 文档列表（共5部分）

### 第1部分：架构概览与流程图
**文件**：`SECKILL-DETAILED-FLOW.md`

**内容**：
- ✅ 系统架构图
- ✅ 完整流程图（从请求到响应）
- ✅ 涉及的所有文件清单
- ✅ 核心技术栈

**适合**：快速了解整体架构和流程

---

### 第2部分：控制层与生产者
**文件**：`SECKILL-DETAILED-FLOW-PART2.md`

**内容**：
- ✅ **OrderController**
  - `submit()` - 发起秒杀请求
  - `result()` - 查询秒杀结果
  - `loadAndValidateGoods()` - 商品校验
- ✅ **OrderProducerService**
  - `sendOrderEvent()` - 发送订单事件
  - `buildMessage()` - 构建消息体

**函数详解**：参数、返回值、执行步骤、异常处理

**适合**：了解请求入口和消息发送逻辑

---

### 第3部分：消息消费者
**文件**：`SECKILL-DETAILED-FLOW-PART3.md`

**内容**：
- ✅ **OrderEventConsumer**（核心）
  - `onMessage()` - 消息监听入口
  - `processOrder()` - 核心业务逻辑
  - `deductStock()` - 预扣库存
  - `rollbackStock()` - 回补库存
  - `cacheResult()` - 缓存结果
  - `convertToGoodsInfo()` - 转换商品快照
- ✅ **三层幂等性防护**
  - Redis 分布式锁
  - 消息去重表
  - 数据库唯一索引

**函数详解**：每个方法的行号、参数、逻辑、SQL

**适合**：了解异步处理和幂等性设计

---

### 第4部分：事务服务与查询
**文件**：`SECKILL-DETAILED-FLOW-PART4.md`

**内容**：
- ✅ **OrderTxService**
  - `createOrder()` - 事务性创建订单
  - 唯一索引冲突处理
- ✅ **OrderService**
  - `queryResult()` - 三层查询策略
  - Redis 缓存 → 数据库 → 返回排队
  - 编码/解码逻辑
- ✅ **性能对比**
  - 同步 vs 异步
  - 各层查询耗时
- ✅ **关键设计决策**
  - 为什么用分布式锁？
  - 为什么用竖线分隔？
  - 为什么需要商品快照？

**适合**：了解订单落库和结果查询逻辑

---

### 第5部分：数据流转与异常处理
**文件**：`SECKILL-DETAILED-FLOW-PART5.md`

**内容**：
- ✅ **完整数据流转图**
  - 每个阶段的数据格式
  - 每次调用的输入输出
  - SQL 和 Redis 操作
- ✅ **异常处理机制**
  - Controller 层异常
  - Producer 层异常
  - Consumer 层异常
  - Transaction 层异常
- ✅ **异常恢复流程**
  - 库存不一致修复
  - 死信队列处理
- ✅ **监控指标**
  - 核心业务指标
  - 异常指标
  - Grafana 大盘配置
- ✅ **最佳实践总结**
  - 幂等性设计
  - 异步设计
  - 降级策略

**适合**：了解异常处理、监控告警、运维实践

---

## 🎯 快速查找指南

### 想了解...

#### 🔹 整体流程
→ **第1部分**：架构概览与流程图

#### 🔹 请求如何接收和校验
→ **第2部分**：`OrderController.submit()` 和 `loadAndValidateGoods()`

#### 🔹 消息如何构建和发送
→ **第2部分**：`OrderProducerServiceImpl`

#### 🔹 消息如何消费和处理
→ **第3部分**：`OrderEventConsumer.onMessage()` 和 `processOrder()`

#### 🔹 如何保证幂等性
→ **第3部分**：三层幂等性防护详解

#### 🔹 库存如何扣减和回补
→ **第3部分**：`deductStock()` 和 `rollbackStock()`

#### 🔹 订单如何落库
→ **第4部分**：`OrderTxService.createOrder()`

#### 🔹 结果如何查询
→ **第4部分**：`OrderService.queryResult()` 三层查询

#### 🔹 数据如何流转
→ **第5部分**：完整数据流转图

#### 🔹 异常如何处理
→ **第5部分**：异常处理机制（分层详解）

#### 🔹 如何监控和告警
→ **第5部分**：监控指标和 Grafana 配置

---

## 📊 核心数据结构速查

### 请求参数（OrderRequestDto）
```java
{
  "goodsId": "019000...456",
  "buyCount": 1
}
```

### 消息体（OrderEventMessage）
```java
{
  "messageId": "uuid",
  "businessKey": "{userId}#{goodsId}",
  "eventType": "order.placed",
  "timestamp": 1750000000000,
  "version": 1,
  "payload": {
    "userId": "019000...123",
    "goodsId": "019000...456",
    "buyCount": 1,
    "goodsSnapshot": { ... }
  }
}
```

### 订单实体（Order）
```java
{
  "id": "019000...789",        // UUIDv7
  "userId": "019000...123",
  "goodsId": "019000...456",
  "buyCount": 1,
  "amount": 4999.00,
  "status": 0,                 // 0-待支付
  "createdAt": "2026-06-15 10:30:15",
  "updatedAt": "2026-06-15 10:30:15"
}
```

### 返回结果（OrderResultDto）
```java
{
  "status": 1,                 // 0-排队中, 1-成功, 2-失败
  "orderId": "019000...789",
  "message": "秒杀成功"
}
```

---

## 🔧 关键配置速查

### RocketMQ 配置
```yaml
rocketmq:
  name-server: 127.0.0.1:9876;127.0.0.1:9877
  producer:
    group: order-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
```

### Redis Key 前缀
```
seckill:result:{userId}:{goodsId}     # 结果缓存
order:processing:{businessKey}        # 分布式锁
seckill:stock:{goodsId}               # 库存计数（goods 服务）
```

### 数据库表
```
t_order                         # 订单表
t_order_message_processed       # 消息去重表
t_goods                         # 商品表（goods 服务）
```

---

## 📈 性能指标速查

### 响应时间
| 模式 | 响应时间 | 提升 |
|-----|---------|------|
| 同步 | 1-2s | - |
| 异步 | <100ms | **10-20倍** |

### 系统吞吐量
| 模式 | QPS | 提升 |
|-----|-----|------|
| 同步 | 1000 | - |
| 异步 | 5000 | **5倍** |

### 数据库压力
| 模式 | TPS | 降低 |
|-----|-----|------|
| 同步 | 1000 | - |
| 异步 | 300 | **70%** |

---

## 🎓 学习路径建议

### 新手入门（2-3小时）
1. 阅读 **第1部分**：了解整体架构
2. 阅读 **第2部分**：理解请求入口
3. 浏览 **第5部分** 的数据流转图
4. 运行项目，发起一次秒杀请求，观察日志

### 深入理解（1天）
1. 完整阅读 **第2-4部分**
2. 对照代码逐个函数理解
3. 画出自己的流程图
4. 思考每个设计决策的原因

### 精通掌握（2-3天）
1. 阅读全部5部分文档
2. 研究异常处理和降级策略
3. 搭建监控告警系统
4. 进行压力测试，观察性能指标
5. 尝试优化或改进现有设计

---

## 🔗 相关文档

| 文档 | 说明 |
|-----|------|
| `ROCKETMQ-QUICKSTART.md` | RocketMQ 快速上手 |
| `rocketmq-async-order.md` | RocketMQ 异步落单设计文档 |
| `order-flow-design.md` | 订单流程设计文档 |
| `database-primary-key-design.md` | 主键设计规范 |
| `OrderMessageProcessedMapper-Usage.md` | Mapper 使用说明 |

---

## ✅ 文档检查清单

使用本系列文档，你应该能够回答以下问题：

- [ ] 秒杀请求从进入到返回经历了哪些步骤？
- [ ] 如何保证同一用户不会重复下单？（三层防护）
- [ ] 消息发送失败会怎样？（降级策略）
- [ ] 库存扣减在哪个服务完成？如何保证原子性？
- [ ] 订单落库失败为什么要回补库存？
- [ ] 前端如何知道订单最终状态？（三层查询）
- [ ] 消息重投会导致重复处理吗？如何避免？
- [ ] 为什么需要商品快照？
- [ ] 如何监控秒杀系统的健康状况？
- [ ] 死信队列的消息如何处理？

如果以上问题都能回答，说明你已经掌握了秒杀系统的核心设计！🎉

---

## 📞 获取帮助

如有疑问，请：
1. 先查阅相关章节
2. 对照代码验证理解
3. 查看日志排查问题
4. 参考监控指标定位异常

---

**文档创建时间**：2026-06-15  
**文档版本**：v1.0  
**维护者**：开发团队

**本文档旨在提供最详细、最准确的秒杀功能说明，帮助所有开发者快速理解和掌握系统设计！** 🚀
