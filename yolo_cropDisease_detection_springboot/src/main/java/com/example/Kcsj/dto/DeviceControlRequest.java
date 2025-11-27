package com.example.Kcsj.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class DeviceControlRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private String deviceId;

    /**
     * 预留多设备下发能力，当同时指定 deviceId 与 deviceIds 时优先使用 deviceIds
     */
    private List<String> deviceIds;

    /**
     * 硬件执行动作标识，兼容旧版字段 command
     */
    @JsonAlias("command")
    private String action;

    /**
     * 控制指令的值，例如 true/false 或持续时间
     */
    private Object value;

    private Map<String, Object> parameters;

    /**
     * 获取最终用于下发的指令标识
     */
    public String getResolvedAction() {
        if (action != null && !action.trim().isEmpty()) {
            return action;
        }
        if (parameters != null && parameters.containsKey("command")) {
            Object commandValue = parameters.get("command");
            return commandValue != null ? String.valueOf(commandValue) : null;
        }
        return null;
    }

    /**
     * 解析需要下发的设备 ID 列表
     */
    public List<String> getResolvedDeviceIds() {
        if (deviceIds != null && !deviceIds.isEmpty()) {
            return deviceIds.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
        if (StringUtils.hasText(deviceId)) {
            return Collections.singletonList(deviceId.trim());
        }
        return Collections.emptyList();
    }
}
