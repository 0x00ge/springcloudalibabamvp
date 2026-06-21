package com.demo.redisson.service;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RBitSet;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBucket;
import org.redisson.api.RDeque;
import org.redisson.api.RHyperLogLog;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RTopic;
import org.redisson.api.RQueue;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RSemaphore;
import org.redisson.api.RTimeSeries;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 基础数据结构教学示例。
 *
 * <p>本类每个 public 方法对应一个 HTTP 接口，也对应一种 Redisson 数据结构。
 * 方法内部通常分为三步：</p>
 * <ul>
 *     <li>通过 RedissonClient 获取一个 Redis 分布式对象；</li>
 *     <li>执行该对象最常见的一组操作；</li>
 *     <li>把关键返回值放入 Map，方便接口直接观察结果。</li>
 * </ul>
 *
 * <p>注意：demo 为了每次调用都得到稳定结果，很多集合类示例会先 clear/delete。
 * 真实业务中不要随意清空 key，要按业务生命周期管理数据。</p>
 */
@Service
public class RedissonDataDemoService {

    // 所有 demo 的 Redis key 统一加前缀，避免和其他项目或本地测试数据冲突。
    private static final String PREFIX = "redisson-demo:data:";

    private final RedissonClient redissonClient;

    /**
     * RedissonClient 由 redisson-spring-boot-starter 自动创建。
     *
     * <p>只要 application.yml 中配置了 spring.data.redis，Spring 就会把连接信息交给 Redisson。
     * 业务代码只需要注入 RedissonClient，然后通过 getXxx 方法获取不同的数据结构。</p>
     */
    public RedissonDataDemoService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 执行所有基础数据结构示例。
     *
     * <p>这个方法适合快速体检 Redis 是否可连接，也适合对比不同 API 返回的数据形态。
     * 因为 blockingQueue、expirableSemaphore 中使用了带超时等待的方法，所以这里声明 InterruptedException。</p>
     */
    public Map<String, Object> runAllDataDemos() throws InterruptedException {
        Map<String, Object> result = new LinkedHashMap<>();
        // 按常见数据结构顺序聚合，方便接口一次性查看所有示例返回值。
        result.put("bucket", bucket());
        result.put("atomicLong", atomicLong());
        result.put("map", map());
        result.put("list", list());
        result.put("set", set());
        result.put("queue", queue());
        result.put("deque", deque());
        result.put("blockingQueue", blockingQueue());
        result.put("scoredSortedSet", scoredSortedSet());
        result.put("bitSet", bitSet());
        result.put("bloomFilter", bloomFilter());
        result.put("hyperLogLog", hyperLogLog());
        result.put("topic", topic());
        result.put("semaphore", semaphore());
        result.put("expirableSemaphore", expirableSemaphore());
        result.put("rateLimiter", rateLimiter());
        result.put("timeSeries", timeSeries());
        return result;
    }

    /**
     * RBucket 示例。
     *
     * <p>RBucket 可以理解为 Redis String 的对象封装。
     * 适合保存单个缓存值、验证码、登录态、配置项、临时状态等。</p>
     */
    public Map<String, Object> bucket() {
        // RBucket 适合放单个值：配置项、缓存对象、状态标记等。
        RBucket<String> bucket = redissonClient.getBucket(PREFIX + "bucket");
        // 带 TTL 的写入，适合缓存类场景；过期时间由 Redis 统一管理。
        bucket.set("hello redisson", Duration.ofMinutes(5));
        // getAndSet 会先返回旧值，再写入新值；适合需要观察替换前状态的场景。
        String oldValue = bucket.getAndSet("hello redis");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("oldValue", oldValue);
        result.put("currentValue", bucket.get());
        result.put("remainTimeToLiveMillis", bucket.remainTimeToLive());
        return result;
    }

    /**
     * RAtomicLong 示例。
     *
     * <p>RAtomicLong 的自增、自减、加法操作在 Redis 侧具备原子性。
     * 多个应用实例同时调用 incrementAndGet，也不会出现 JVM 本地计数器那种竞争问题。</p>
     */
    public Map<String, Object> atomicLong() {
        // RAtomicLong 是分布式原子计数器，适合库存编号、流水号、访问次数等。
        RAtomicLong counter = redissonClient.getAtomicLong(PREFIX + "counter");
        // demo 每次重置为 100，保证接口重复调用时结果稳定。
        counter.set(100);
        // incrementAndGet：先自增 1，再返回新值。
        long afterIncrement = counter.incrementAndGet();
        // addAndGet：加指定增量，再返回新值。
        long afterAdd = counter.addAndGet(9);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("afterIncrement", afterIncrement);
        result.put("afterAdd", afterAdd);
        return result;
    }

    /**
     * RMap 示例。
     *
     * <p>RMap 对应 Redis Hash，适合一个 key 下保存多个字段。
     * 例如 user:10001 下面保存 name、role、city 等字段。</p>
     */
    public Map<String, Object> map() {
        // RMap 对应 Redis Hash，适合结构化对象的部分字段读写。
        RMap<String, String> userMap = redissonClient.getMap(PREFIX + "map");
        // 为了 demo 可重复执行，先清空本 key 下的旧字段。
        userMap.clear();
        // put 会返回旧值；这里不关心旧值，只演示写入。
        userMap.put("id", "10001");
        userMap.put("name", "alice");
        userMap.put("role", "admin");
        // fastRemove 不返回被删除的值，少一次返回值处理，适合只关心删除结果的场景。
        userMap.fastRemove("role");

        Map<String, Object> result = new LinkedHashMap<>();
        // readAllMap 一次性读取全部字段；真实大 Map 不建议频繁全量读。
        result.put("values", userMap.readAllMap());
        result.put("containsName", userMap.containsKey("name"));
        return result;
    }

    /**
     * RList 示例。
     *
     * <p>RList 对应 Redis List，保留插入顺序，支持按下标访问。
     * 适合小规模有序数据；如果是高吞吐任务消费，更推荐队列类结构。</p>
     */
    public Map<String, Object> list() {
        // RList 对应 Redis List，适合有序数据、消息缓冲、任务流。
        RList<String> taskList = redissonClient.getList(PREFIX + "list");
        taskList.clear();
        // add 会追加到列表尾部，顺序为 create-order -> pay-order -> send-message。
        taskList.add("create-order");
        taskList.add("pay-order");
        taskList.add("send-message");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("all", taskList.readAll());
        result.put("second", taskList.get(1));
        return result;
    }

    /**
     * RSet 示例。
     *
     * <p>RSet 对应 Redis Set，集合元素不会重复。
     * 常见业务场景：用户标签、已参与活动用户、去重 ID 集合。</p>
     */
    public Map<String, Object> set() {
        // RSet 对应 Redis Set，天然去重，适合标签、用户 ID 集合等。
        RSet<String> tagSet = redissonClient.getSet(PREFIX + "set");
        tagSet.clear();
        // Set.of 本身已经去重；这里表达的是“集合语义不允许重复元素”。
        tagSet.addAll(Set.of("java", "spring", "redis", "java"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("all", tagSet.readAll());
        result.put("containsRedis", tagSet.contains("redis"));
        return result;
    }

    /**
     * RQueue 示例。
     *
     * <p>RQueue 是非阻塞 FIFO 队列：poll 时如果没有元素会立刻返回 null。
     * 适合简单的先进先出缓冲。</p>
     */
    public Map<String, Object> queue() {
        // RQueue 是普通 FIFO 队列，适合生产者/消费者场景。
        RQueue<String> queue = redissonClient.getQueue(PREFIX + "queue");
        queue.clear();
        queue.add("job-1");
        queue.add("job-2");

        Map<String, Object> result = new LinkedHashMap<>();
        // poll 取出并删除队头元素，所以 remaining 中只剩 job-2。
        result.put("firstPoll", queue.poll());
        result.put("remaining", new ArrayList<>(queue));
        return result;
    }

    /**
     * RDeque 示例。
     *
     * <p>RDeque 是双端队列，队头和队尾都能插入/弹出。
     * 适合需要两端操作的业务，比如优先任务插队、撤销/重做栈等。</p>
     */
    public Map<String, Object> deque() {
        // RDeque 支持队头和队尾操作，适合双端任务队列、撤销/重做一类操作。
        RDeque<String> deque = redissonClient.getDeque(PREFIX + "deque");
        deque.clear();
        // addFirst 写入队头，addLast 写入队尾。
        deque.addFirst("first");
        deque.addLast("last");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pollFirst", deque.pollFirst());
        result.put("pollLast", deque.pollLast());
        return result;
    }

    /**
     * RBlockingQueue 示例。
     *
     * <p>阻塞队列适合消费者线程等待任务。poll(timeout) 在队列为空时会等待一段时间，
     * 比 while 循环反复查询 Redis 更友好。</p>
     */
    public Map<String, Object> blockingQueue() throws InterruptedException {
        // 阻塞队列适合“没数据就等”的消费者模型，常用于异步任务派发。
        RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue(PREFIX + "blocking-queue");
        blockingQueue.clear();
        // 这里先放入一个任务，因此第一次 poll 会立即取到 async-job。
        blockingQueue.offer("async-job");

        Map<String, Object> result = new LinkedHashMap<>();
        // 最多等待 1 秒获取任务；当前队列已有元素，所以不会真的等满 1 秒。
        result.put("pollWithTimeout", blockingQueue.poll(1, TimeUnit.SECONDS));
        // 第二次队列已空，等待 100ms 后返回 null，用来展示超时语义。
        result.put("emptyPoll", blockingQueue.poll(100, TimeUnit.MILLISECONDS));
        return result;
    }

    /**
     * RScoredSortedSet 示例。
     *
     * <p>RScoredSortedSet 对应 Redis Sorted Set。
     * 每个元素都有一个 score，Redis 会按 score 排序，非常适合排行榜。</p>
     */
    public Map<String, Object> scoredSortedSet() {
        // 排行榜最常见的 Redisson 数据结构，分数和排名都由 Redis 维护。
        RScoredSortedSet<String> ranking = redissonClient.getScoredSortedSet(PREFIX + "ranking");
        ranking.clear();
        // add(score, value)：写入分数和成员。
        ranking.add(95, "alice");
        ranking.add(88, "bob");
        ranking.add(100, "cindy");
        // addScore(value, delta)：给已有成员增加分数；如果成员不存在则按增量创建。
        ranking.addScore("alice", 10);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topPlayers", ranking.valueRangeReversed(0, 2));
        result.put("aliceScore", ranking.getScore("alice"));
        result.put("aliceRank", ranking.revRank("alice"));
        return result;
    }

    /**
     * RBitSet 示例。
     *
     * <p>BitSet 用一个 bit 表示一个布尔状态，空间利用率非常高。
     * 示例中用 bit 下标表示每月第几天是否签到。</p>
     */
    public Map<String, Object> bitSet() {
        // BitSet 适合签到、日活标记、功能开关等高密度布尔位存储。
        RBitSet signIn = redissonClient.getBitSet(PREFIX + "signin:2026-06");
        signIn.clear();
        // set(0) 表示第 1 天签到；set(4) 表示第 5 天签到。
        signIn.set(0);
        signIn.set(1);
        signIn.set(4);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("day1Signed", signIn.get(0));
        result.put("day3Signed", signIn.get(2));
        // cardinality 返回值为 true 的 bit 数量，也就是已签到天数。
        result.put("signedDays", signIn.cardinality());
        return result;
    }

    /**
     * RBloomFilter 示例。
     *
     * <p>布隆过滤器用于“快速判断某个值是否可能存在”。
     * 它可能误判存在，但不会误判不存在，常用于缓存穿透保护。</p>
     */
    public Map<String, Object> bloomFilter() {
        // 布隆过滤器适合防缓存穿透：先判断“可能存在”，再去查数据库。
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(PREFIX + "bloom:sku");
        // tryInit(expectedInsertions, falseProbability)：预计插入量和误判率。
        // 已经初始化过的 BloomFilter 再调用 tryInit 不会覆盖旧配置。
        bloomFilter.tryInit(1000, 0.01);
        bloomFilter.add("sku-10001");
        bloomFilter.add("sku-10002");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mightContainExisting", bloomFilter.contains("sku-10001"));
        result.put("mightContainMissing", bloomFilter.contains("sku-99999"));
        result.put("expectedInsertions", bloomFilter.getExpectedInsertions());
        return result;
    }

    /**
     * RHyperLogLog 示例。
     *
     * <p>HyperLogLog 用很少的内存估算去重数量，适合 UV、独立访客、独立设备统计。
     * 它是估算结构，不适合要求绝对精确的计数。</p>
     */
    public Map<String, Object> hyperLogLog() {
        // HyperLogLog 适合做 UV 估算，牺牲精确度换取极低内存占用。
        RHyperLogLog<String> uv = redissonClient.getHyperLogLog(PREFIX + "uv");
        uv.delete();
        // user-2 重复出现，但 count 估算的是去重后的数量。
        uv.addAll(List.of("user-1", "user-2", "user-2", "user-3"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("estimatedUniqueUsers", uv.count());
        return result;
    }

    /**
     * RTopic 示例。
     *
     * <p>RTopic 是 Redis 发布订阅封装，适合在线通知、配置刷新、轻量事件广播。
     * 它不是可靠消息队列：订阅者离线时可能收不到历史消息。</p>
     */
    public Map<String, Object> topic() {
        // Topic 是发布订阅模型，更适合事件通知，而不是可靠消息队列。
        RTopic topic = redissonClient.getTopic(PREFIX + "topic");
        // publish 返回收到消息的订阅者数量。当前 demo 没有注册监听器，一般为 0。
        long received = topic.publish("order-created");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("publishedMessage", "order-created");
        result.put("subscriberCount", received);
        return result;
    }

    /**
     * RSemaphore 示例。
     *
     * <p>信号量可以控制最大并发数。比如最多允许 2 个节点同时执行某个任务。
     * 和互斥锁不同，信号量允许多个持有者同时进入。</p>
     */
    public Map<String, Object> semaphore() {
        // 信号量限制并发数，拿到 permit 才能继续执行。
        RSemaphore semaphore = redissonClient.getSemaphore(PREFIX + "semaphore");
        // 先删除旧信号量，避免上一次 demo 的 permit 数影响本次结果。
        semaphore.delete();
        // 初始化 2 个 permit；如果 key 已存在，trySetPermits 不会覆盖。
        semaphore.trySetPermits(2);
        // tryAcquire 非阻塞，拿不到 permit 会直接返回 false。
        boolean acquired = semaphore.tryAcquire();
        if (acquired) {
            // 拿到 permit 后必须释放，否则可用许可会被占用。
            semaphore.release();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("acquired", acquired);
        result.put("availablePermits", semaphore.availablePermits());
        return result;
    }

    /**
     * RPermitExpirableSemaphore 示例。
     *
     * <p>它和 RSemaphore 类似，但每个 permit 都有独立 ID，并且可以设置租约时间。
     * 适合“拿到资源后必须在指定时间内归还”的场景。</p>
     */
    public Map<String, Object> expirableSemaphore() throws InterruptedException {
        // 可过期信号量的 permit 带租约，避免进程异常退出后长期占坑。
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(PREFIX + "expirable-semaphore");
        semaphore.delete();
        semaphore.trySetPermits(1);
        // tryAcquire(waitTime, leaseTime, unit)：最多等 1 秒，拿到后 permit 3 秒后自动过期。
        String permitId = semaphore.tryAcquire(1, 3, TimeUnit.SECONDS);
        if (permitId != null) {
            // 可过期信号量释放时必须传 permitId，因为每个许可都有独立身份。
            semaphore.release(permitId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("permitId", permitId);
        result.put("availablePermits", semaphore.availablePermits());
        return result;
    }

    /**
     * RRateLimiter 示例。
     *
     * <p>限流器适合保护接口、外部服务调用、昂贵资源访问。
     * 当前示例配置为整体每秒最多 3 次。</p>
     */
    public Map<String, Object> rateLimiter() {
        // 限流器控制单位时间内的通过数量，适合接口限流、资源保护。
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(PREFIX + "rate-limiter");
        rateLimiter.delete();
        // OVERALL 表示所有客户端共享同一个速率限制。
        // 如果使用 PER_CLIENT，则每个 Redisson 客户端单独限流。
        rateLimiter.trySetRate(RateType.OVERALL, 3, 1, RateIntervalUnit.SECONDS);

        Map<String, Object> result = new LinkedHashMap<>();
        // 前三次通常为 true，第四次通常为 false，用来展示令牌耗尽效果。
        result.put("firstAcquire", rateLimiter.tryAcquire());
        result.put("secondAcquire", rateLimiter.tryAcquire());
        result.put("thirdAcquire", rateLimiter.tryAcquire());
        result.put("fourthAcquire", rateLimiter.tryAcquire());
        return result;
    }

    /**
     * RTimeSeries 示例。
     *
     * <p>时间序列按时间戳存储值，并可以附加标签 label。
     * 适合监控指标、IoT 采集数据、价格走势等按时间查询的数据。</p>
     */
    public Map<String, Object> timeSeries() {
        // 时间序列适合按时间点追加数值，例如温度、心率、监控指标等。
        RTimeSeries<Integer, String> timeSeries = redissonClient.getTimeSeries(PREFIX + "time-series");
        timeSeries.delete();
        // 当前时间作为最新采样点，前两个点模拟历史采样。
        long now = System.currentTimeMillis();
        // add(timestamp, value, label)：这里 value 是温度数值，label 是指标名。
        timeSeries.add(now - 2000, 20, "temperature");
        timeSeries.add(now - 1000, 30, "temperature");
        timeSeries.add(now, 25, "temperature");

        Map<String, Object> result = new LinkedHashMap<>();
        // get(timestamp) 精确读取某个时间戳的值。
        result.put("latestValue", timeSeries.get(now));
        // entryRange 返回时间范围内的完整条目，包含 timestamp、value、label。
        result.put("range", timeSeries.entryRange(now - 3000, now));
        return result;
    }
}
