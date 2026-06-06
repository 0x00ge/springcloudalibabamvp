package com.mvp.iot.consumer;

import com.alibaba.fastjson2.JSON;
import com.mvp.iot.config.MqttConfig;
import com.mvp.iot.dto.HumidityReport;
import com.mvp.iot.service.DeviceHumidityService;
import com.mvp.iot.util.MqttMessageSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 设备湿度 MQTT 消息消费者。
 *
 * <p>订阅 topic：{@code mvp/iot/device/+/humidity}。
 * 设备发布 topic 示例：{@code mvp/iot/device/device-001/humidity}。</p>
 */
@Component
public class HumidityReportHandler {

    private static final Logger log = LoggerFactory.getLogger(HumidityReportHandler.class);

    private final DeviceHumidityService deviceHumidityService;

    public HumidityReportHandler(DeviceHumidityService deviceHumidityService) {
        this.deviceHumidityService = deviceHumidityService;
    }

    /**
     * 处理湿度上报入站消息。
     */
    @ServiceActivator(inputChannel = MqttConfig.MQTT_HUMIDITY_INBOUND_CHANNEL)
    public void handle(Message<?> message) {
        // 1. 读取 MQTT topic 和 payload。
        String topic = String.valueOf(message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
        String payload = MqttMessageSupport.toPayloadText(message.getPayload());

        try {
            // 2. 把 JSON payload 转换成湿度上报 DTO。
            HumidityReport report = JSON.parseObject(payload, HumidityReport.class);
            if (report == null) {
                throw new IllegalArgumentException("湿度上报 payload 不能为空");
            }

            // 3. 补齐 topic、原始 payload、deviceId、默认单位和默认上报时间。
            report.setTopic(topic);
            report.setRawPayload(payload);
            fillDefaults(report, topic);

            // 4. 交给业务 service 处理。
            deviceHumidityService.handle(report);
        } catch (Exception e) {
            log.warn("处理设备湿度上报失败：topic={}, payload={}", topic, payload, e);
        }
    }

    private void fillDefaults(HumidityReport report, String topic) {
        if (!StringUtils.hasText(report.getDeviceId())) {
            report.setDeviceId(MqttMessageSupport.resolveDeviceId(topic));
        }
        if (!StringUtils.hasText(report.getUnit())) {
            report.setUnit("%");
        }
        if (report.getReportedAt() == null) {
            report.setReportedAt(System.currentTimeMillis());
        }
    }
}
