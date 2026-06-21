package com.demo.redisson.controller;

import com.demo.redisson.dto.DemoResponse;
import com.demo.redisson.service.RedissonLockDemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Redisson 锁和同步器示例接口。
 *
 * <p>所有接口只做两件事：接收 HTTP 请求、调用 Service 返回结果。
 * 锁的 tryLock、unlock、await 等细节全部放在 RedissonLockDemoService 中。</p>
 *
 * <p>接口统一以 /redisson/lock 开头。</p>
 */
@RestController
@RequestMapping("/redisson/lock")
public class RedissonLockDemoController {

    private final RedissonLockDemoService lockDemoService;

    /**
     * 构造器注入锁示例服务。
     */
    public RedissonLockDemoController(RedissonLockDemoService lockDemoService) {
        this.lockDemoService = lockDemoService;
    }

    /**
     * 一次性执行常见锁和同步器 demo。
     *
     * <p>适合学习接口返回字段，也适合启动后检查 Redis 连接。</p>
     */
    @GetMapping("/all")
    public DemoResponse<Map<String, Object>> all() throws Exception {
        return DemoResponse.ok("Redisson lock demos finished", lockDemoService.runAllLockDemos());
    }

    /**
     * RLock：普通可重入锁。
     */
    @GetMapping("/lock")
    public DemoResponse<Map<String, Object>> lock() throws InterruptedException {
        return DemoResponse.ok("RLock demo finished", lockDemoService.lock());
    }

    /**
     * RFairLock：公平锁，按请求顺序排队。
     */
    @GetMapping("/fair-lock")
    public DemoResponse<Map<String, Object>> fairLock() throws InterruptedException {
        return DemoResponse.ok("RFairLock demo finished", lockDemoService.fairLock());
    }

    /**
     * RReadWriteLock：读写锁，读并发、写排他。
     */
    @GetMapping("/read-write-lock")
    public DemoResponse<Map<String, Object>> readWriteLock() throws InterruptedException {
        return DemoResponse.ok("RReadWriteLock demo finished", lockDemoService.readWriteLock());
    }

    /**
     * RMultiLock：多个锁同时成功才算成功。
     */
    @GetMapping("/multi-lock")
    public DemoResponse<Map<String, Object>> multiLock() throws InterruptedException {
        return DemoResponse.ok("RMultiLock demo finished", lockDemoService.multiLock());
    }

    /**
     * RRedLock：红锁 API 示例。
     */
    @GetMapping("/red-lock")
    public DemoResponse<Map<String, Object>> redLock() throws InterruptedException {
        return DemoResponse.ok("RRedLock demo finished", lockDemoService.redLock());
    }

    /**
     * RSpinLock：自旋锁。
     */
    @GetMapping("/spin-lock")
    public DemoResponse<Map<String, Object>> spinLock() throws InterruptedException {
        return DemoResponse.ok("RSpinLock demo finished", lockDemoService.spinLock());
    }

    /**
     * RFencedLock：带 fencing token 的锁。
     */
    @GetMapping("/fenced-lock")
    public DemoResponse<Map<String, Object>> fencedLock() throws InterruptedException {
        return DemoResponse.ok("RFencedLock demo finished", lockDemoService.fencedLock());
    }

    /**
     * RSemaphore：信号量，控制最大并发数。
     */
    @GetMapping("/semaphore")
    public DemoResponse<Map<String, Object>> semaphore() {
        return DemoResponse.ok("RSemaphore demo finished", lockDemoService.semaphore());
    }

    /**
     * RCountDownLatch：闭锁，等待多个任务完成。
     */
    @GetMapping("/count-down-latch")
    public DemoResponse<Map<String, Object>> countDownLatch() throws InterruptedException {
        return DemoResponse.ok("RCountDownLatch demo finished", lockDemoService.countDownLatch());
    }

    /**
     * tryLockAsync：异步加锁。
     */
    @GetMapping("/async-lock")
    public DemoResponse<Map<String, Object>> asyncLock() throws Exception {
        return DemoResponse.ok("Async RLock demo finished", lockDemoService.asyncLock());
    }
}
