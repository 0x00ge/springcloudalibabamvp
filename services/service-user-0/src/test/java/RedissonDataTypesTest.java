import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 基本数据类型完整测试（最终修正版）
 * 修正：addScore 参数顺序，移除 RSortedSet，使用 RScoredSortedSet
 */
public class RedissonDataTypesTest {

    private RedissonClient redissonClient;

    @BeforeEach
    public void setUp() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5);

        // 设置为 JSON 序列化（重要！）
        config.setCodec(new StringCodec());

        redissonClient = Redisson.create(config);
        System.out.println("RedissonClient 初始化成功（使用 JsonJacksonCodec）\n");
    }

    @AfterEach
    public void tearDown() {
        if (redissonClient != null) {
            redissonClient.shutdown();
            System.out.println("\nRedissonClient 已关闭");
        }
    }

    // ==================== 1. RBucket ====================
    @Test
    public void testBucket() {
        System.out.println("========== RBucket ==========");
        RBucket<String> bucket = redissonClient.getBucket("test:str");
        bucket.set("张三");
        System.out.println("值: " + bucket.get());
        bucket.set("李四", 10, TimeUnit.SECONDS);
        String old = bucket.getAndSet("王五");
        System.out.println("旧值: " + old + ", 新值: " + bucket.get());
        bucket.delete();
    }

    // ==================== 2. RMap ====================
    @Test
    public void testMap() {
        System.out.println("========== RMap ==========");
        RMap<String, String> map = redissonClient.getMap("test:map");
        map.put("name", "张三");
        map.put("age", "25");
        map.putAll(Map.of("city", "北京", "phone", "13800000000"));
        System.out.println("全部数据: " + map.readAllMap());
        map.remove("phone");
        System.out.println("删除phone后: " + map.readAllMap());
        map.delete();
    }

    // ==================== 3. RList ====================
    @Test
    public void testList() {
        System.out.println("========== RList ==========");
        RList<String> list = redissonClient.getList("test:list");
        list.addAll(Arrays.asList("任务1", "任务2", "任务3"));
        System.out.println("全部: " + list.readAll());
        list.remove(0);
        System.out.println("移除第一个后: " + list.readAll());
        list.delete();
    }

    // ==================== 4. RSet ====================
    @Test
    public void testSet() {
        System.out.println("========== RSet ==========");
        RSet<String> set = redissonClient.getSet("test:set");
        set.addAll(Set.of("Java", "Spring", "Redis", "Java"));
        System.out.println("去重后: " + set.readAll());
        set.remove("Redis");
        System.out.println("包含Java? " + set.contains("Java"));
        set.delete();
    }

    // ==================== 5. RScoredSortedSet（正确使用 addScore） ====================
    @Test
    public void testScoredSortedSet() {
        System.out.println("========== RScoredSortedSet ==========");
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet("test:rank");

        // 正确用法：addScore(元素, 分数)
        scoredSet.addScore("玩家A", 100);
        scoredSet.addScore("玩家B", 95);
        scoredSet.addScore("玩家C", 110);
        scoredSet.addScore("玩家D", 88);
        System.out.println("初始添加完成");

        // 增加分数（同样，元素在前，分数增量在后）
        scoredSet.addScore("玩家A", 20);   // 玩家A 变成120分
        System.out.println("玩家A 当前分数: " + scoredSet.getScore("玩家A"));

        // 获取所有玩家（正序，分数从低到高）
        Collection<String> asc = scoredSet.valueRange(0, -1);
        System.out.println("正序排列: " + asc);   // [玩家D(88), 玩家B(95), 玩家C(110), 玩家A(120)]

        // 获取倒序排行榜（分数从高到低）
        Collection<String> desc = scoredSet.valueRangeReversed(0, -1);
        System.out.println("倒序排行榜: " + desc); // [玩家A(120), 玩家C(110), 玩家B(95), 玩家D(88)]

        // 获取分数在 90~120 之间的玩家
        Collection<String> between = scoredSet.valueRange(90, true, 120, true);
        System.out.println("分数90~120: " + between); // [玩家B(95), 玩家C(110), 玩家A(120)]

        // 获取玩家C的正序排名（从0开始）
        Integer rank = scoredSet.rank("玩家C");
        System.out.println("玩家C 正序排名: " + rank); // 2

        scoredSet.delete();
    }

    // ==================== 6. RLexSortedSet ====================
    @Test
    public void testLexSortedSet() {
        System.out.println("========== RLexSortedSet ==========");
        RLexSortedSet lexSet = redissonClient.getLexSortedSet("test:lex");
        lexSet.addAll(Set.of("apple", "banana", "cat", "dog", "ant"));
        System.out.println("字典序: " + lexSet.readAll()); // [ant, apple, banana, cat, dog]
        Collection<String> range = lexSet.range("b", true, "d", false);
        System.out.println("b 到 d（不含d）: " + range); // [banana, cat]
        lexSet.delete();
    }

    // ==================== 7. 分布式锁 ====================
    @Test
    public void testLock() throws InterruptedException {
        System.out.println("========== 分布式锁 ==========");
        RLock lock = redissonClient.getLock("test:lock");
        if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
            try {
                System.out.println("获取锁成功，执行临界区代码");
                Thread.sleep(1000);
            } finally {
                lock.unlock();
                System.out.println("锁已释放");
            }
        } else {
            System.out.println("获取锁失败");
        }
    }

    // ==================== 8. 综合演示 ====================
    @Test
    public void testAll() {
        System.out.println("\n========== 快速综合演示 ==========\n");
        RBucket<String> bucket = redissonClient.getBucket("demo:str");
        bucket.set("Redisson");
        System.out.println("RBucket: " + bucket.get());

        RMap<String, Integer> map = redissonClient.getMap("demo:map");
        map.put("score", 100);
        System.out.println("RMap: " + map.get("score"));

        RScoredSortedSet<String> rank = redissonClient.getScoredSortedSet("demo:rank");
        rank.addScore("Alice", 95);
        rank.addScore("Bob", 88);
        System.out.println("排行榜: " + rank.valueRangeReversed(0, -1));

        // 清理
        bucket.delete();
        map.delete();
        rank.delete();
        System.out.println("清理完成");
    }
}