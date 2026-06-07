package com.mvp.iot.service;

import com.mvp.iot.dto.HumidityReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 设备湿度业务处理。
 *
 * <p>当前先打印日志，后续可以在这里扩展：写入 MySQL、写入 Redis 时序缓存、触发湿度告警等。</p>
 */
@Slf4j
@Service
public class DeviceHumidityService {

    /** 高湿度告警阈值。 */
    private static final BigDecimal HIGH_HUMIDITY_THRESHOLD = BigDecimal.valueOf(90);

    /**
     * 处理设备湿度上报。
     */
    public void handle(HumidityReport report) {
        // 1. 校验核心字段。
        if (!StringUtils.hasText(report.getDeviceId())) {
            throw new IllegalArgumentException("deviceId 不能为空");
        }
        if (report.getHumidity() == null) {
            throw new IllegalArgumentException("humidity 不能为空");
        }

        // 2. 打印正常湿度上报日志。
        log.info("收到设备湿度上报：deviceId={}, humidity={} {}, reportedAt={}, topic={}",
                report.getDeviceId(),
                report.getHumidity(),
                report.getUnit(),
                report.getReportedAt(),
                report.getTopic());

        // 3. 简单高湿度告警示例。
        if (report.getHumidity().compareTo(HIGH_HUMIDITY_THRESHOLD) >= 0) {
            log.warn("设备湿度过高：deviceId={}, humidity={} {}",
                    report.getDeviceId(),
                    report.getHumidity(),
                    report.getUnit());
        }
    }
}
