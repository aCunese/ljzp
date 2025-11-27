package com.example.Kcsj.hardware.tcp;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public final class HardwareCommandFormatter {

    private HardwareCommandFormatter() {
    }

    public static String formatCommand(String action, Map<String, Object> parameters, Object value) {
        if (StrUtil.isBlank(action)) {
            return null;
        }
        String normalized = action.trim().toLowerCase();
        if (Arrays.asList("mode", "led", "pump", "feng").contains(normalized)) {
            return normalized + "\r\n";
        }
        if (normalized.startsWith("temp") || normalized.startsWith("set") || normalized.contains("target")) {
            String temp = resolveParam(parameters, value, "temp", "temperature", "targetTemp");
            String light = resolveParam(parameters, null, "light", "light_intensity", "targetLight");
            String soil = resolveParam(parameters, null, "soil", "soil_hmd", "soil_moisture", "targetSoil");
            if (StrUtil.isAllNotBlank(temp, light, soil)) {
                return String.format("temp:%s,light:%s,soil_hmd:%s\r\n", temp, light, soil);
            }
        }
        return normalized + "\r\n";
    }

    private static String resolveParam(Map<String, Object> parameters, Object fallback, String... keys) {
        if (parameters != null) {
            for (String key : keys) {
                if (parameters.containsKey(key)) {
                    Object value = parameters.get(key);
                    if (value != null && StrUtil.isNotBlank(String.valueOf(value))) {
                        return String.valueOf(value).trim();
                    }
                }
            }
        }
        return Optional.ofNullable(fallback)
                .map(String::valueOf)
                .map(String::trim)
                .orElse(null);
    }
}
