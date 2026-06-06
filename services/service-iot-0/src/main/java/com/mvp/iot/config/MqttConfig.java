package com.mvp.iot.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

/**
 * MQTT 接入配置。
 *
 * <p>这里负责建立 MQTT 连接、订阅设备温度/湿度 topic，并把收到的消息投递到 Spring Integration channel。</p>
 */
@Configuration
@EnableIntegration
public class MqttConfig {

    /** 温度上报入站消息通道名称。 */
    public static final String MQTT_TEMPERATURE_INBOUND_CHANNEL = "mqttTemperatureInboundChannel";

    /** 湿度上报入站消息通道名称。 */
    public static final String MQTT_HUMIDITY_INBOUND_CHANNEL = "mqttHumidityInboundChannel";

    /**
     * 创建 MQTT ClientFactory。
     *
     * <p>Spring Integration MQTT 的入站 adapter 会使用这个 factory 连接 Broker。</p>
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttProperties properties) {
        // 1. 设置 Broker 地址和连接参数。
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{properties.getBrokerUrl()});
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        // 2. Broker 开启账号密码认证时写入用户名和密码。
        if (StringUtils.hasText(properties.getUsername())) {
            options.setUserName(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            options.setPassword(properties.getPassword().toCharArray());
        }

        // 3. 交给 Paho ClientFactory 统一创建 MQTT client。
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    /**
     * 温度上报入站通道。
     *
     * <p>MQTT adapter 收到消息后会把消息发送到这个 channel，再由消费方法处理。</p>
     */
    @Bean(name = MQTT_TEMPERATURE_INBOUND_CHANNEL)
    public MessageChannel mqttTemperatureInboundChannel() {
        return new DirectChannel();
    }

    /**
     * 湿度上报入站通道。
     *
     * <p>MQTT adapter 收到消息后会把消息发送到这个 channel，再由消费方法处理。</p>
     */
    @Bean(name = MQTT_HUMIDITY_INBOUND_CHANNEL)
    public MessageChannel mqttHumidityInboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT 温度上报订阅 adapter。
     *
     * <p>订阅 topic 示例：{@code mvp/iot/device/+/temperature}。
     * 设备实际发布 topic 可以是 {@code mvp/iot/device/device-001/temperature}。</p>
     */
    @Bean
    public MessageProducer mqttTemperatureInboundAdapter(MqttPahoClientFactory mqttClientFactory,
                                                         MessageChannel mqttTemperatureInboundChannel,
                                                         MqttProperties properties) {
        // 1. 创建入站 adapter，并指定 clientId、ClientFactory 和订阅 topic。
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                properties.getClientId() + "-temperature-in",
                mqttClientFactory,
                properties.getTemperatureTopics().toArray(String[]::new)
        );

        // 2. 设置消息转换器、QoS 和超时时间。
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(properties.getQos());
        adapter.setCompletionTimeout(properties.getCompletionTimeout());

        // 3. 把 MQTT 消息投递到温度上报 channel。
        adapter.setOutputChannel(mqttTemperatureInboundChannel);
        return adapter;
    }

    /**
     * MQTT 湿度上报订阅 adapter。
     *
     * <p>订阅 topic 示例：{@code mvp/iot/device/+/humidity}。
     * 设备实际发布 topic 可以是 {@code mvp/iot/device/device-001/humidity}。</p>
     */
    @Bean
    public MessageProducer mqttHumidityInboundAdapter(MqttPahoClientFactory mqttClientFactory,
                                                      MessageChannel mqttHumidityInboundChannel,
                                                      MqttProperties properties) {
        // 1. 创建入站 adapter，并指定 clientId、ClientFactory 和订阅 topic。
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                properties.getClientId() + "-humidity-in",
                mqttClientFactory,
                properties.getHumidityTopics().toArray(String[]::new)
        );

        // 2. 设置消息转换器、QoS 和超时时间。
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(properties.getQos());
        adapter.setCompletionTimeout(properties.getCompletionTimeout());

        // 3. 把 MQTT 消息投递到湿度上报 channel。
        adapter.setOutputChannel(mqttHumidityInboundChannel);
        return adapter;
    }
}
