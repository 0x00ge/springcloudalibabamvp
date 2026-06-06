package com.mvp.iot.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 设备温度上报消息。
 *
 * <p>推荐 payload：
 * {"temperature":26.5,"unit":"C","reportedAt":1710000000000}</p>
 */
@Data
public class TemperatureReport {

    /** 设备 id。可以由 payload 传入，也可以从 topic 中解析。 */
    private String deviceId;

    /** 温度值。 */
    private BigDecimal temperature;

    /** 温度单位，默认摄氏度 C。 */
    private String unit = "C";

    /** 设备上报时间戳，单位毫秒。 */
    private Long reportedAt;

    /** 实际收到的 MQTT topic。 */
    private String topic;

    /** 原始 MQTT payload，方便排查问题。 */
    private String rawPayload;
}
