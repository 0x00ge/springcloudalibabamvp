package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RocketMQDemoApplication {

    public static void main(String[] args) {
        System.setProperty("com.rocketmq.sendMessageWithVIPChannel", "false");
        SpringApplication.run(RocketMQDemoApplication.class, args);
        System.out.println("\n=================================");
        System.out.println("RocketMQ Demo 启动成功！");
        System.out.println("访问: http://localhost:8080");
        System.out.println("=================================\n");
    }
}
