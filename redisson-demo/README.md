# Redisson Demo

这个模块演示 Redisson 的基础数据结构和常见分布式锁用法。默认连接本机 Redis Cluster：

```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - 127.0.0.1:7001
          - 127.0.0.1:7002
          - 127.0.0.1:7003
          - 127.0.0.1:7004
          - 127.0.0.1:7005
          - 127.0.0.1:7006
      timeout: 3000ms
```

Redis Cluster 只支持 0 号库，不再配置 `database`。

## 代码阅读路线

建议按下面顺序阅读源码：

1. `pom.xml`：查看 demo 依赖，重点是 `redisson-spring-boot-starter` 如何提供 `RedissonClient`。
2. `src/main/resources/application.yml`：查看 Redis 连接、端口、日志级别配置。
3. `RedissonDemoApplication`：查看 Spring Boot 启动入口和组件扫描范围。
4. `DemoResponse`：查看接口统一响应结构。
5. `RedissonDataDemoController`：查看基础数据结构接口路径。
6. `RedissonDataDemoService`：查看每种 Redisson 数据结构的详细用法和注释。
7. `RedissonLockDemoController`：查看锁和同步器接口路径。
8. `RedissonLockDemoService`：查看每种锁、信号量、闭锁的详细用法和注释。

代码里的注释按“适用场景、关键参数、返回值含义、注意事项”来写，适合边调用接口边对照源码看。

## 启动

```bash
mvn -pl redisson-demo spring-boot:run
```

服务默认端口是 `8091`：

```bash
curl http://localhost:8091/redisson/data/all
curl http://localhost:8091/redisson/lock/all
```

## 基本数据用法

接口前缀：`/redisson/data`

| 接口 | Redisson 类型 | 说明 | 适用环境 | 典型场景 |
| --- | --- | --- | --- | --- |
| `/bucket` | `RBucket` | 字符串对象、TTL、getAndSet | 单机可用，分布式共享更有价值 | 缓存单值、验证码、登录态、配置项 |
| `/atomic-long` | `RAtomicLong` | 分布式原子计数器 | 单机可用，分布式共享更有价值 | 流水号、访问次数、库存计数 |
| `/map` | `RMap` | Redis Hash 映射 | 单机可用，分布式共享更有价值 | 用户字段、对象属性、局部字段更新 |
| `/list` | `RList` | Redis List 列表 | 单机可用，分布式共享更有价值 | 有序数据、任务步骤、消息缓冲 |
| `/set` | `RSet` | Redis Set 去重集合 | 单机可用，分布式共享更有价值 | 标签集合、去重用户 ID、活动参与记录 |
| `/queue` | `RQueue` | FIFO 队列 | 单机可用，分布式共享更有价值 | 简单生产消费、先进先出任务缓冲 |
| `/deque` | `RDeque` | 双端队列 | 单机可用，分布式共享更有价值 | 双端任务队列、优先任务插队、撤销/重做 |
| `/blocking-queue` | `RBlockingQueue` | 阻塞队列 | 更适合分布式 | 多消费者任务派发、异步任务等待 |
| `/scored-sorted-set` | `RScoredSortedSet` | 排行榜、分数、排名 | 更适合分布式 | 排行榜、热度榜、优先级排序 |
| `/bit-set` | `RBitSet` | 签到、布尔位统计 | 单机可用，分布式共享更有价值 | 签到、日活标记、功能开关 |
| `/bloom-filter` | `RBloomFilter` | 防穿透、快速判断是否可能存在 | 更适合分布式 | 缓存穿透保护、商品/用户是否可能存在 |
| `/hyper-log-log` | `RHyperLogLog` | UV 去重估算 | 更适合分布式 | UV 统计、独立访客、独立设备估算 |
| `/topic` | `RTopic` | 发布订阅 | 更适合分布式 | 事件广播、配置刷新、在线通知 |
| `/semaphore` | `RSemaphore` | 信号量 | 单机可用，分布式共享更有价值 | 控制最大并发数、资源池许可 |
| `/expirable-semaphore` | `RPermitExpirableSemaphore` | 带租约 permit 的信号量 | 更适合分布式 | 带过期时间的资源占用、异常退出自动释放 |
| `/rate-limiter` | `RRateLimiter` | 分布式限流 | 更适合分布式 | 接口限流、外部服务调用限流、资源保护 |
| `/time-series` | `RTimeSeries` | 时间序列数据 | 更适合分布式 | 监控指标、IoT 采集、价格走势 |

建议学习顺序：

1. 先看 `/bucket`、`/map`、`/list`、`/set`，理解 Redisson 如何封装 Redis 常见数据结构。
2. 再看 `/queue`、`/deque`、`/blocking-queue`，理解队列和阻塞等待。
3. 接着看 `/scored-sorted-set`、`/bit-set`、`/bloom-filter`、`/hyper-log-log`，理解业务型数据结构。
4. 最后看 `/semaphore`、`/expirable-semaphore`、`/rate-limiter`、`/time-series`，理解分布式同步和指标类能力。

## 锁用法

接口前缀：`/redisson/lock`

| 接口 | Redisson 类型 | 说明 | 适用环境 | 典型场景 |
| --- | --- | --- | --- | --- |
| `/lock` | `RLock` | 普通可重入锁 | 单机可用，分布式最常用 | 库存扣减、订单状态更新、定时任务防重复 |
| `/fair-lock` | `RFairLock` | 公平锁，按请求顺序排队 | 更适合分布式 | 强顺序执行、避免请求插队 |
| `/read-write-lock` | `RReadWriteLock` | 读写锁，读多写少场景 | 单机可用，分布式共享更有价值 | 配置缓存、规则表刷新、读多写少资源 |
| `/multi-lock` | `RMultiLock` | 联锁，多个资源都加锁成功才算成功 | 更适合分布式 | 同时占用库存和优惠券、多资源原子占用 |
| `/red-lock` | `RRedLock` | 红锁，多 Redis 节点多数派加锁 API 示例 | 更适合多 Redis 节点 | 多独立 Redis 实例多数派加锁 |
| `/spin-lock` | `RSpinLock` | 自旋锁，短临界区场景 | 更适合分布式短临界区 | 极短任务保护、避免频繁阻塞唤醒 |
| `/fenced-lock` | `RFencedLock` | 栅栏锁，返回递增 token 防止过期客户端写入 | 更适合分布式 | 防止锁过期后的旧客户端写回、下游 token 校验 |
| `/semaphore` | `RSemaphore` | 控制最大并发数 | 单机可用，分布式共享更有价值 | 限制并发任务数、控制资源池容量 |
| `/count-down-latch` | `RCountDownLatch` | 等待多个任务完成 | 单机可用，分布式共享更有价值 | 等待多个节点初始化、批处理完成后继续 |
| `/async-lock` | `tryLockAsync` | 异步加锁 | 更适合分布式异步链路 | 响应式流程、不阻塞业务线程的加锁 |

所有示例 key 都以 `redisson-demo:` 开头，方便在 Redis 里查看和清理。

锁接口阅读重点：

- `tryLock(waitTime, leaseTime, unit)` 的两个时间含义不同：`waitTime` 是最多等多久拿锁，`leaseTime` 是拿到后多久自动释放。
- 所有锁都要放在 `try/finally` 中释放，避免业务异常导致锁不归还。
- 释放前使用 `isHeldByCurrentThread()` 判断当前线程是否持有锁，避免误释放。
- `RLock` 是最常用选择，`RFairLock`、`RReadWriteLock`、`RMultiLock`、`RFencedLock` 根据场景再选。

## 简单结论

- 单机项目也能用 Redisson，但它更多像“Redis 数据结构客户端”。
- 真正体现价值的是多实例、多线程、多服务共享状态时。
- 锁类能力里，最常用的是 `RLock`、`RFairLock`、`RReadWriteLock`、`RSemaphore`、`RRateLimiter`。
