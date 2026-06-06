package com.mvp.iot.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 设备湿度上报消息。
 *
 * <p>推荐 payload：
 * {"humidity":63.5,"unit":"%","reportedAt":1710000000000}</p>
 */
@Data
public class HumidityReport {

    /** 设备 id。可以由 payload 传入，也可以从 topic 中解析。 */
    private String deviceId;

    /** 湿度值。 */
    private BigDecimal humidity;

    /** 湿度单位，默认百分比 %。 */
    private String unit = "%";

    /** 设备上报时间戳，单位毫秒。 */
    private Long reportedAt;

    /** 实际收到的 MQTT topic。 */
    private String topic;

    /** 原始 MQTT payload，方便排查问题。 */
    private String rawPayload;
}
