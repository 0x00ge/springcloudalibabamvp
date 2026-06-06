package com.mvp.iot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MQTT 连接和订阅配置。
 *
 * <p>对应 application.yml 中的 mqtt 配置段。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    /** MQTT Broker 地址，开发环境可以使用本机 EMQX 或 Mosquitto。 */
    private String brokerUrl = "tcp://127.0.0.1:1883";

    /** MQTT clientId，同一个 Broker 下必须唯一。 */
    private String clientId = "service-iot-0";

    /** MQTT 用户名，没有开启认证时可以为空。 */
    private String username;

    /** MQTT 密码，没有开启认证时可以为空。 */
    private String password;

    /** 订阅 QoS。设备上报通常使用 QoS 1，保证至少送达一次。 */
    private int qos = 1;

    /** MQTT 操作完成超时时间，单位毫秒。 */
    private long completionTimeout = 5000;

    /** 设备温度上报 topic，+ 表示单层通配符。 */
    private List<String> temperatureTopics = new ArrayList<>(List.of("mvp/iot/device/+/temperature"));

    /** 设备湿度上报 topic，+ 表示单层通配符。 */
    private List<String> humidityTopics = new ArrayList<>(List.of("mvp/iot/device/+/humidity"));
}
