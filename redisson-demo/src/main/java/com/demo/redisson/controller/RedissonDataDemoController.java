package com.demo.redisson.controller;

import com.demo.redisson.dto.DemoResponse;
import com.demo.redisson.service.RedissonDataDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Redisson 基础数据结构示例接口。
 *
 * <p>这一层只负责暴露 HTTP 路由，不直接写 Redisson 操作；
 * 具体逻辑放在 RedissonDataDemoService 中，便于把“接口入口”和“API 用法”分开阅读。</p>
 *
 * <p>接口统一以 /redisson/data 开头，例如：
 * http://localhost:8091/redisson/data/bucket</p>
 */
@RestController
@RequestMapping("/redisson/data")
public class RedissonDataDemoController {

    private final RedissonDataDemoService dataDemoService;

    /**
     * 使用构造器注入 Service。
     *
     * <p>相比字段注入，构造器注入更容易测试，也能让依赖关系在对象创建时就明确。</p>
     */
    public RedissonDataDemoController(RedissonDataDemoService dataDemoService) {
        this.dataDemoService = dataDemoService;
    }

    /**
     * 一次性执行所有数据结构 demo。
     *
     * <p>适合第一次启动后快速检查 Redis 连接是否可用。</p>
     */
    @GetMapping("/all")
    public DemoResponse<Map<String, Object>> all() throws InterruptedException {
        return DemoResponse.ok("Redisson basic data demos finished", dataDemoService.runAllDataDemos());
    }

    /**
     * RBucket：演示单值缓存、TTL、getAndSet。
     */
    @GetMapping("/bucket")
    public DemoResponse<Map<String, Object>> bucket() {
        return DemoResponse.ok("RBucket demo finished", dataDemoService.bucket());
    }

    /**
     * RAtomicLong：演示分布式原子计数。
     */
    @GetMapping("/atomic-long")
    public DemoResponse<Map<String, Object>> atomicLong() {
        return DemoResponse.ok("RAtomicLong demo finished", dataDemoService.atomicLong());
    }

    /**
     * RMap：演示 Redis Hash 风格的字段读写。
     */
    @GetMapping("/map")
    public DemoResponse<Map<String, Object>> map() {
        return DemoResponse.ok("RMap demo finished", dataDemoService.map());
    }

    /**
     * RList：演示有序列表。
     */
    @GetMapping("/list")
    public DemoResponse<Map<String, Object>> list() {
        return DemoResponse.ok("RList demo finished", dataDemoService.list());
    }

    /**
     * RSet：演示去重集合。
     */
    @GetMapping("/set")
    public DemoResponse<Map<String, Object>> set() {
        return DemoResponse.ok("RSet demo finished", dataDemoService.set());
    }

    /**
     * RQueue：演示普通 FIFO 队列。
     */
    @GetMapping("/queue")
    public DemoResponse<Map<String, Object>> queue() {
        return DemoResponse.ok("RQueue demo finished", dataDemoService.queue());
    }

    /**
     * RDeque：演示双端队列。
     */
    @GetMapping("/deque")
    public DemoResponse<Map<String, Object>> deque() {
        return DemoResponse.ok("RDeque demo finished", dataDemoService.deque());
    }

    /**
     * RBlockingQueue：演示带超时等待的阻塞队列。
     */
    @GetMapping("/blocking-queue")
    public DemoResponse<Map<String, Object>> blockingQueue() throws InterruptedException {
        return DemoResponse.ok("RBlockingQueue demo finished", dataDemoService.blockingQueue());
    }

    /**
     * RScoredSortedSet：演示排行榜、分数和排名。
     */
    @GetMapping("/scored-sorted-set")
    public DemoResponse<Map<String, Object>> scoredSortedSet() {
        return DemoResponse.ok("RScoredSortedSet demo finished", dataDemoService.scoredSortedSet());
    }

    /**
     * RBitSet：演示签到、布尔位标记。
     */
    @GetMapping("/bit-set")
    public DemoResponse<Map<String, Object>> bitSet() {
        return DemoResponse.ok("RBitSet demo finished", dataDemoService.bitSet());
    }

    /**
     * RBloomFilter：演示布隆过滤器的可能存在判断。
     */
    @GetMapping("/bloom-filter")
    public DemoResponse<Map<String, Object>> bloomFilter() {
        return DemoResponse.ok("RBloomFilter demo finished", dataDemoService.bloomFilter());
    }

    /**
     * RHyperLogLog：演示 UV 去重估算。
     */
    @GetMapping("/hyper-log-log")
    public DemoResponse<Map<String, Object>> hyperLogLog() {
        return DemoResponse.ok("RHyperLogLog demo finished", dataDemoService.hyperLogLog());
    }

    /**
     * RTopic：演示发布订阅。
     */
    @GetMapping("/topic")
    public DemoResponse<Map<String, Object>> topic() {
        return DemoResponse.ok("RTopic demo finished", dataDemoService.topic());
    }

    /**
     * RSemaphore：演示信号量 permit 获取与释放。
     */
    @GetMapping("/semaphore")
    public DemoResponse<Map<String, Object>> semaphore() {
        return DemoResponse.ok("RSemaphore demo finished", dataDemoService.semaphore());
    }

    /**
     * RPermitExpirableSemaphore：演示带租约的 permit。
     */
    @GetMapping("/expirable-semaphore")
    public DemoResponse<Map<String, Object>> expirableSemaphore() throws InterruptedException {
        return DemoResponse.ok("RPermitExpirableSemaphore demo finished", dataDemoService.expirableSemaphore());
    }

    /**
     * RRateLimiter：演示单位时间限流。
     */
    @GetMapping("/rate-limiter")
    public DemoResponse<Map<String, Object>> rateLimiter() {
        return DemoResponse.ok("RRateLimiter demo finished", dataDemoService.rateLimiter());
    }

    /**
     * RTimeSeries：演示按时间戳记录指标值。
     */
    @GetMapping("/time-series")
    public DemoResponse<Map<String, Object>> timeSeries() {
        return DemoResponse.ok("RTimeSeries demo finished", dataDemoService.timeSeries());
    }
}
