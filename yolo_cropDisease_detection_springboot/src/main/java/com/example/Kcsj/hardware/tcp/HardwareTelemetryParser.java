package com.example.Kcsj.hardware.tcp;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.example.Kcsj.entity.SensorData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HardwareTelemetryParser {

    private static final Pattern TEMP_PATTERN = Pattern.compile("\\$temp:([^#]+)#");
    private static final Pattern SOIL_PATTERN = Pattern.compile("\\$soil_hmd:([^#]+)#");
    private static final Pattern LIGHT_PATTERN = Pattern.compile("\\$light:([^#]+)#");

    private HardwareTelemetryParser() {
    }

    public static SensorData parse(String payload, String deviceId) {
        if (StrUtil.isBlank(payload)) {
            return null;
        }
        BigDecimal temperature = toDecimal(extract(TEMP_PATTERN, payload));
        BigDecimal soil = toDecimal(extract(SOIL_PATTERN, payload));
        BigDecimal light = toDecimal(extract(LIGHT_PATTERN, payload));

        if (temperature == null && soil == null && light == null) {
            return null;
        }
        return SensorData.builder()
                .deviceId(deviceId)
                .temperature(temperature)
                .soilMoisture(soil)
                .lightIntensity(light)
                .timestamp(LocalDateTime.now())
                .createdAt(new Date())
                .build();
    }

    private static String extract(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static BigDecimal toDecimal(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            double num = NumberUtil.parseDouble(value.trim());
            return BigDecimal.valueOf(num);
        } catch (Exception ex) {
            return null;
        }
    }
}
