package com.demo.redisson;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Redisson demo 启动类。
 *
 * <p>启动后 Spring Boot 会完成三件事：</p>
 * <ul>
 *     <li>扫描 com.demo.redisson 包下的 Controller、Service、DTO 等组件；</li>
 *     <li>读取 application.yml 中的 spring.data.redis 配置；</li>
 *     <li>通过 redisson-spring-boot-starter 自动创建 RedissonClient Bean。</li>
 * </ul>
 *
 * <p>业务代码不要自己 new RedissonClient，直接通过构造器注入即可。</p>
 */
@SpringBootApplication
public class RedissonDemoApplication {

    public static void main(String[] args) {
        // 标准 Spring Boot 启动入口，返回 ConfigurableApplicationContext。
        // 本 demo 不需要手动保存上下文对象，因此直接调用即可。
        SpringApplication.run(RedissonDemoApplication.class, args);

        // 控制台提示仅用于本地学习，方便启动后马上知道访问入口。
        System.out.println();
        System.out.println("=================================");
        System.out.println("Redisson Demo started");
        System.out.println("Base URL: http://localhost:8091/redisson");
        System.out.println("=================================");
        System.out.println();
    }
}
