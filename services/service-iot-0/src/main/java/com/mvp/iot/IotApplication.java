package com.mvp.iot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * IoT 服务启动入口。
 *
 * <p>当前服务负责接入 MQTT Broker，订阅设备温度上报消息。</p>
 */
@SpringBootApplication
public class IotApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotApplication.class, args);
    }
}
