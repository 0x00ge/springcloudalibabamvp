package com.mvp.iot.consumer;

import com.alibaba.fastjson2.JSON;
import com.mvp.iot.config.MqttConfig;
import com.mvp.iot.dto.TemperatureReport;
import com.mvp.iot.service.DeviceTemperatureService;
import com.mvp.iot.util.MqttMessageSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 设备温度 MQTT 消息消费者。
 *
 * <p>订阅 topic：{@code mvp/iot/device/+/temperature}。
 * 设备发布 topic 示例：{@code mvp/iot/device/device-001/temperature}。</p>
 */
@Slf4j
@Component
public class TemperatureReportHandler {

    private final DeviceTemperatureService deviceTemperatureService;

    public TemperatureReportHandler(DeviceTemperatureService deviceTemperatureService) {
        this.deviceTemperatureService = deviceTemperatureService;
    }

    /**
     * 处理温度上报入站消息。
     */
    @ServiceActivator(inputChannel = MqttConfig.MQTT_TEMPERATURE_INBOUND_CHANNEL)
    public void handle(Message<?> message) {
        // 1. 读取 MQTT topic 和 payload。
        String topic = String.valueOf(message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
        String payload = MqttMessageSupport.toPayloadText(message.getPayload());

        try {
            // 2. 把 JSON payload 转换成温度上报 DTO。
            TemperatureReport report = JSON.parseObject(payload, TemperatureReport.class);
            if (report == null) {
                throw new IllegalArgumentException("温度上报 payload 不能为空");
            }

            // 3. 补齐 topic、原始 payload、deviceId、默认单位和默认上报时间。
            report.setTopic(topic);
            report.setRawPayload(payload);
            fillDefaults(report, topic);

            // 4. 交给业务 service 处理。
            deviceTemperatureService.handle(report);
        } catch (Exception e) {
            log.warn("处理设备温度上报失败：topic={}, payload={}", topic, payload, e);
        }
    }

    private void fillDefaults(TemperatureReport report, String topic) {
        if (!StringUtils.hasText(report.getDeviceId())) {
            report.setDeviceId(MqttMessageSupport.resolveDeviceId(topic));
        }
        if (!StringUtils.hasText(report.getUnit())) {
            report.setUnit("C");
        }
        if (report.getReportedAt() == null) {
            report.setReportedAt(System.currentTimeMillis());
        }
    }
}
