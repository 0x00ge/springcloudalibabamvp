package com.mvp.iot.util;

import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * MQTT 消息处理公共方法。
 */
public final class MqttMessageSupport {

    private MqttMessageSupport() {
    }

    /**
     * 把 MQTT payload 转成 UTF-8 字符串。
     */
    public static String toPayloadText(Object payload) {
        if (payload instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(payload);
    }

    /**
     * 从 topic 中解析 deviceId。
     *
     * <p>示例 topic：{@code mvp/iot/device/device-001/temperature}，
     * 解析结果为 {@code device-001}。</p>
     */
    public static String resolveDeviceId(String topic) {
        if (!StringUtils.hasText(topic)) {
            return null;
        }

        String[] parts = topic.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("device".equals(parts[i])) {
                return parts[i + 1];
            }
        }
        return null;
    }
}
