package com.demo.redisson.service;

import org.redisson.api.RCountDownLatch;
import org.redisson.api.RFencedLock;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redisson 分布式锁和同步器教学示例。
 *
 * <p>分布式锁的重点不是“会调用 lock()”，而是理解以下几件事：</p>
 * <ul>
 *     <li>锁的 key 决定了竞争范围，同一个 key 才会互斥；</li>
 *     <li>tryLock 通常比 lock 更适合业务系统，因为可以设置等待时间；</li>
 *     <li>拿到锁后必须在 finally 中释放，避免业务异常导致锁长期占用；</li>
 *     <li>释放锁前要确认当前线程持有锁，避免误释放其他线程的锁。</li>
 * </ul>
 */
@Service
public class RedissonLockDemoService {

    // 锁相关的 key 单独分组，方便在 Redis 里区分数据演示和锁演示。
    private static final String PREFIX = "redisson-demo:lock:";

    private final RedissonClient redissonClient;

    /**
     * 注入 RedissonClient。
     *
     * <p>锁、信号量、闭锁等同步器都通过 RedissonClient 获取。
     * Redisson 内部会负责和 Redis 通信，并维护锁的看门狗续期等机制。</p>
     */
    public RedissonLockDemoService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 执行所有锁和同步器示例。
     *
     * <p>这个方法适合学习返回值，不建议拿它做压测。
     * 真正压测分布式锁时，需要多个 JVM 或多个服务实例同时请求同一个锁 key。</p>
     */
    public Map<String, Object> runAllLockDemos() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        // 每个方法都演示一种锁/同步器，便于逐个接口观察语义差异。
        result.put("lock", lock());
        result.put("fairLock", fairLock());
        result.put("readWriteLock", readWriteLock());
        result.put("multiLock", multiLock());
        result.put("redLock", redLock());
        result.put("spinLock", spinLock());
        result.put("fencedLock", fencedLock());
        result.put("semaphore", semaphore());
        result.put("countDownLatch", countDownLatch());
        return result;
    }

    /**
     * 普通可重入锁 RLock。
     *
     * <p>最常用的分布式锁类型，适合保护一个共享资源：
     * 库存扣减、订单状态更新、定时任务防重复执行等。</p>
     */
    public Map<String, Object> lock() throws InterruptedException {
        // 普通可重入锁：最常见，适合单资源临界区。
        RLock lock = redissonClient.getLock(PREFIX + "normal");
        // tryLock(waitTime, leaseTime, unit)
        // waitTime=3：最多等待 3 秒获取锁，超过就返回 false；
        // leaseTime=10：拿到锁后 10 秒自动释放，避免进程崩溃造成死锁。
        boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
        try {
            Map<String, Object> result = baseLockResult("RLock", locked);
            result.put("usage", "普通可重入锁，适合保护库存扣减、订单状态流转等临界区。");
            result.put("holdCount", locked ? lock.getHoldCount() : 0);
            return result;
        } finally {
            // 分布式锁必须在 finally 释放，保证业务异常时也能归还锁。
            unlockIfHeld(lock);
        }
    }

    /**
     * 公平锁 RFairLock。
     *
     * <p>公平锁按请求顺序排队，适合对顺序敏感的场景。
     * 代价是性能通常低于普通锁，因为 Redis 需要维护等待队列。</p>
     */
    public Map<String, Object> fairLock() throws InterruptedException {
        // 公平锁：按申请顺序排队，避免“插队”，但吞吐一般会低一些。
        RLock lock = redissonClient.getFairLock(PREFIX + "fair");
        boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
        try {
            Map<String, Object> result = baseLockResult("RFairLock", locked);
            result.put("usage", "公平锁按请求顺序排队，吞吐低一些，但适合强顺序场景。");
            return result;
        } finally {
            unlockIfHeld(lock);
        }
    }

    /**
     * 读写锁 RReadWriteLock。
     *
     * <p>多个读锁可以同时存在，但写锁会排斥其他读锁和写锁。
     * 适合读多写少的共享资源，比如配置缓存、规则表刷新等。</p>
     */
    public Map<String, Object> readWriteLock() throws InterruptedException {
        // 读写锁：读锁可并发，写锁独占，适合读多写少。
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(PREFIX + "read-write");
        RLock readLock = readWriteLock.readLock();
        RLock writeLock = readWriteLock.writeLock();
        // 先拿读锁，模拟“当前有读操作正在进行”。
        boolean readLocked = readLock.tryLock(3, 10, TimeUnit.SECONDS);
        try {
            // 在读锁未释放时尝试拿写锁。
            // 正常情况下写锁拿不到，writeLockedWhileReading 应为 false。
            boolean writeLockedWhileReading = writeLock.tryLock(200, 1, TimeUnit.MILLISECONDS);
            if (writeLockedWhileReading) {
                writeLock.unlock();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "RReadWriteLock");
            result.put("readLocked", readLocked);
            result.put("writeLockedWhileReading", writeLockedWhileReading);
            result.put("usage", "读多写少时用读写锁，提高并发读能力，写锁会排他。");
            return result;
        } finally {
            // 这里只需要释放读锁；写锁如果意外拿到，上面已经释放。
            unlockIfHeld(readLock);
        }
    }

    /**
     * 联锁 RMultiLock。
     *
     * <p>联锁会同时持有多个独立锁。只有所有锁都获取成功，整体才算成功。
     * 适合一次业务操作同时占用多个资源，例如库存和优惠券必须同时锁住。</p>
     */
    public Map<String, Object> multiLock() throws InterruptedException {
        // 联锁：多个独立锁必须一起拿到，才进入临界区。
        RLock inventoryLock = redissonClient.getLock(PREFIX + "multi:inventory");
        RLock couponLock = redissonClient.getLock(PREFIX + "multi:coupon");
        // getMultiLock 会把多个 RLock 组合为一个逻辑锁，释放时也会一起释放。
        RLock multiLock = redissonClient.getMultiLock(inventoryLock, couponLock);
        boolean locked = multiLock.tryLock(3, 10, TimeUnit.SECONDS);
        try {
            Map<String, Object> result = baseLockResult("RMultiLock", locked);
            result.put("usage", "联锁要求多个资源同时加锁成功，适合一次操作同时占用库存和优惠券。");
            return result;
        } finally {
            unlockIfHeld(multiLock);
        }
    }

    /**
     * 红锁 RRedLock。
     *
     * <p>红锁设计目标是多个独立 Redis 节点上的多数派加锁。
     * 本 demo 使用同一个 RedisClient 创建三个 key，只展示 API 形式；
     * 生产环境如果使用红锁，应连接多个相互独立的 Redis 实例。</p>
     */
    public Map<String, Object> redLock() throws InterruptedException {
        // 红锁：面向多个 Redis 实例的多数派加锁算法。
        RLock lock1 = redissonClient.getLock(PREFIX + "red:1");
        RLock lock2 = redissonClient.getLock(PREFIX + "red:2");
        RLock lock3 = redissonClient.getLock(PREFIX + "red:3");
        // getRedLock 会在多个锁中按红锁算法尝试获取多数派。
        RLock redLock = redissonClient.getRedLock(lock1, lock2, lock3);
        boolean locked = redLock.tryLock(3, 10, TimeUnit.SECONDS);
        try {
            Map<String, Object> result = baseLockResult("RRedLock", locked);
            result.put("usage", "红锁用于多个独立 Redis 节点的多数派加锁；单 Redis demo 只展示 API 形式。");
            return result;
        } finally {
            unlockIfHeld(redLock);
        }
    }

    /**
     * 自旋锁 RSpinLock。
     *
     * <p>自旋锁不会马上进入长时间阻塞，而是短间隔反复尝试。
     * 适合临界区非常短的场景；如果业务执行时间较长，普通 RLock 更稳妥。</p>
     */
    public Map<String, Object> spinLock() throws InterruptedException {
        // 自旋锁：短时间内反复尝试获取锁，适合超短临界区。
        RLock lock = redissonClient.getSpinLock(PREFIX + "spin");
        boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
        try {
            Map<String, Object> result = baseLockResult("RSpinLock", locked);
            result.put("usage", "自旋锁适合锁持有时间极短的场景，避免频繁阻塞唤醒。");
            return result;
        } finally {
            unlockIfHeld(lock);
        }
    }

    /**
     * 栅栏锁 RFencedLock。
     *
     * <p>普通锁只能保证“同一时间尽量只有一个客户端进入临界区”。
     * 栅栏锁额外返回递增 token，下游数据库或存储系统可以拒绝旧 token 写入，
     * 用来处理客户端长 GC、网络抖动、锁过期后又恢复执行的风险。</p>
     */
    public Map<String, Object> fencedLock() throws InterruptedException {
        // 栅栏锁：返回递增 token，调用方写入下游系统时可校验 token 新旧。
        RFencedLock lock = redissonClient.getFencedLock(PREFIX + "fenced");
        // tryLockAndGetToken 成功时返回 fencing token；失败时返回 null。
        Long token = lock.tryLockAndGetToken(3, 10, TimeUnit.SECONDS);
        try {
            Map<String, Object> result = baseLockResult("RFencedLock", token != null);
            result.put("token", token);
            result.put("usage", "栅栏锁会返回递增 token，下游存储可用 token 拒绝过期客户端写入。");
            return result;
        } finally {
            unlockIfHeld(lock);
        }
    }

    /**
     * 信号量 RSemaphore。
     *
     * <p>信号量用于控制并发数量，不要求互斥。
     * 比如一个接口最多允许 2 个任务同时执行，就设置 2 个 permit。</p>
     */
    public Map<String, Object> semaphore() {
        // 信号量：不是“互斥”，而是“并发数控制”。
        RSemaphore semaphore = redissonClient.getSemaphore(PREFIX + "semaphore");
        semaphore.delete();
        semaphore.trySetPermits(2);
        // 这里连续尝试 3 次。因为只有 2 个 permit，第三次应该失败。
        boolean first = semaphore.tryAcquire();
        boolean second = semaphore.tryAcquire();
        boolean third = semaphore.tryAcquire();
        if (first) {
            semaphore.release();
        }
        if (second) {
            semaphore.release();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "RSemaphore");
        result.put("firstAcquire", first);
        result.put("secondAcquire", second);
        result.put("thirdAcquire", third);
        result.put("availablePermits", semaphore.availablePermits());
        result.put("usage", "信号量限制并发数量，比如最多 2 个任务同时处理。");
        return result;
    }

    /**
     * 分布式闭锁 RCountDownLatch。
     *
     * <p>闭锁适合“等待多个节点完成后再继续”的场景：
     * 例如多个服务实例预热缓存，全部完成后网关再放量。</p>
     */
    public Map<String, Object> countDownLatch() throws InterruptedException {
        // 闭锁：主线程等待多个工作线程全部完成后再继续。
        String latchName = PREFIX + "count-down-latch";
        RCountDownLatch latch = redissonClient.getCountDownLatch(latchName);
        latch.delete();
        // 设置计数为 2，表示需要两个 countDown 才会打开闭锁。
        latch.trySetCount(2);

        // Java 本地 CountDownLatch 只用于等待本 demo 中的两个线程执行完成。
        // Redisson 的 RCountDownLatch 才是跨 JVM 可见的分布式闭锁。
        CountDownLatch workersDone = new CountDownLatch(2);
        AtomicInteger completed = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            Thread worker = new Thread(() -> {
                try {
                    completed.incrementAndGet();
                    // 每个工作线程完成后，对 Redis 中的分布式闭锁计数减 1。
                    redissonClient.getCountDownLatch(latchName).countDown();
                } finally {
                    workersDone.countDown();
                }
            });
            worker.start();
        }

        // 等待本地两个线程都跑完，避免接口过早返回。
        boolean workersFinished = workersDone.await(2, TimeUnit.SECONDS);
        // 等待 Redis 中的分布式闭锁打开。
        boolean latchOpened = latch.await(2, TimeUnit.SECONDS);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "RCountDownLatch");
        result.put("workersFinished", workersFinished);
        result.put("latchOpened", latchOpened);
        result.put("completedWorkers", completed.get());
        result.put("usage", "闭锁适合等待多个节点完成初始化或批处理任务后再继续。");
        return result;
    }

    /**
     * 异步加锁示例。
     *
     * <p>tryLockAsync 立即返回 RFuture，不会在调用处直接阻塞。
     * 本 demo 为了接口结果清晰，仍然调用 get 等待结果；真实响应式链路可以继续组合 future。</p>
     */
    public Map<String, Object> asyncLock() throws Exception {
        // 异步加锁：适合不想阻塞当前线程的业务链路。
        RLock lock = redissonClient.getLock(PREFIX + "async");
        RFuture<Boolean> future = lock.tryLockAsync(3, 10, TimeUnit.SECONDS);
        boolean locked = future.toCompletableFuture().get(5, TimeUnit.SECONDS);
        try {
            Map<String, Object> result = baseLockResult("RLock Async", locked);
            result.put("usage", "异步锁适合响应式或不想阻塞业务线程的场景。");
            return result;
        } finally {
            unlockIfHeld(lock);
        }
    }

    /**
     * 构造锁类接口的公共返回字段。
     *
     * @param type 锁类型名称
     * @param locked 是否成功获取锁
     * @return 用于 HTTP 响应的 Map
     */
    private Map<String, Object> baseLockResult(String type, boolean locked) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("locked", locked);
        result.put("thread", Thread.currentThread().getName());
        result.put("time", LocalDateTime.now());
        return result;
    }

    /**
     * 安全释放锁。
     *
     * <p>isHeldByCurrentThread 很重要：Redisson 锁和 Java ReentrantLock 一样，
     * 应由持有它的线程释放。如果当前线程没有持有锁，直接 unlock 会抛异常。</p>
     */
    private void unlockIfHeld(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
