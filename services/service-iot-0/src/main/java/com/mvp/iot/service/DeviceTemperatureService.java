package com.mvp.iot.service;

import com.mvp.iot.dto.TemperatureReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 设备温度业务处理。
 *
 * <p>当前先打印日志，后续可以在这里扩展：写入 MySQL、写入 Redis 时序缓存、触发温度告警等。</p>
 */
@Slf4j
@Service
public class DeviceTemperatureService {

    /** 高温告警阈值，单位摄氏度。 */
    private static final BigDecimal HIGH_TEMPERATURE_THRESHOLD = BigDecimal.valueOf(80);

    /**
     * 处理设备温度上报。
     */
    public void handle(TemperatureReport report) {
        // 1. 校验核心字段。
        if (!StringUtils.hasText(report.getDeviceId())) {
            throw new IllegalArgumentException("deviceId 不能为空");
        }
        if (report.getTemperature() == null) {
            throw new IllegalArgumentException("temperature 不能为空");
        }

        // 2. 打印正常温度上报日志。
        log.info("收到设备温度上报：deviceId={}, temperature={} {}, reportedAt={}, topic={}",
                report.getDeviceId(),
                report.getTemperature(),
                report.getUnit(),
                report.getReportedAt(),
                report.getTopic());

        // 3. 简单高温告警示例。
        if (report.getTemperature().compareTo(HIGH_TEMPERATURE_THRESHOLD) >= 0) {
            log.warn("设备温度过高：deviceId={}, temperature={} {}",
                    report.getDeviceId(),
                    report.getTemperature(),
                    report.getUnit());
        }
    }
}
