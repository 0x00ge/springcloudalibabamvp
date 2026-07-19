package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KafkaDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaDemoApplication.class, args);
        System.out.println("\n=================================");
        System.out.println("Kafka Demo 启动成功！");
        System.out.println("访问: http://localhost:8091/order/");
        System.out.println("=================================\n");
    }
}
