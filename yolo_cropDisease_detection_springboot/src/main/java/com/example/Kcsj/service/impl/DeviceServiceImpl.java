package com.example.Kcsj.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.Kcsj.config.MqttConfig;
import com.example.Kcsj.dto.DeviceControlRequest;
import com.example.Kcsj.dto.DeviceControlResponse;
import com.example.Kcsj.entity.DeviceControlLog;
import com.example.Kcsj.hardware.tcp.HardwareCommandFormatter;
import com.example.Kcsj.hardware.tcp.HardwareTcpGateway;
import com.example.Kcsj.mapper.DeviceControlLogMapper;
import com.example.Kcsj.service.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceControlLogMapper deviceControlLogMapper;
    private final ObjectMapper objectMapper;
    private final Optional<MqttConfig.MqttGateway> mqttGateway;
    private final Optional<HardwareTcpGateway> hardwareTcpGateway;

    @Value("${mqtt.topic.control-prefix:device/control/}")
    private String controlTopicPrefix;

    @Value("${mqtt.enabled:true}")
    private boolean mqttEnabled;

    @Override
    public DeviceControlResponse executeControl(DeviceControlRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("设备控制请求不能为空");
        }

        List<String> deviceIds = request.getResolvedDeviceIds();
        if (deviceIds.isEmpty()) {
            throw new IllegalArgumentException("请选择至少一个目标设备");
        }

        String resolvedAction = request.getResolvedAction();
        if (StrUtil.isBlank(resolvedAction)) {
            throw new IllegalArgumentException("缺少控制指令标识 action");
        }

        HardwareTcpGateway tcpGateway = hardwareTcpGateway.orElse(null);
        MqttConfig.MqttGateway mqtt = mqttGateway.orElse(null);
        boolean tcpAvailable = tcpGateway != null;
        boolean mqttAvailable = mqttEnabled && mqtt != null;

        List<DeviceControlResponse.DeviceResult> results = new ArrayList<>();
        for (String deviceId : deviceIds) {
            DeviceControlResponse.DeviceResult.DeviceResultBuilder resultBuilder = DeviceControlResponse.DeviceResult.builder()
                    .deviceId(deviceId)
                    .timestamp(new Date());

            if (!tcpAvailable && !mqttAvailable) {
                persistControlLog(request, resolvedAction, null, "SKIPPED", deviceId);
                resultBuilder.status("SKIPPED")
                        .message("MQTT/TCP 通道均未启用，指令已记录");
                results.add(resultBuilder.build());
                continue;
            }

            if (tcpAvailable) {
                handleTcpDispatch(request, resolvedAction, deviceId, resultBuilder, tcpGateway);
            } else {
                handleMqttDispatch(request, resolvedAction, deviceId, resultBuilder, mqtt);
            }
            results.add(resultBuilder.build());
        }

        String overallStatus = results.stream().allMatch(r -> "SUCCESS".equalsIgnoreCase(r.getStatus())
                || "PENDING".equalsIgnoreCase(r.getStatus())) ? "SUCCESS" : "PARTIAL";

        return DeviceControlResponse.builder()
                .status(overallStatus)
                .message(String.format("指令 [%s] 已处理，共 %d 台设备", resolvedAction, results.size()))
                .deviceId(deviceIds.get(0))
                .timestamp(new Date())
                .deviceResults(results)
                .build();
    }

    private void handleTcpDispatch(DeviceControlRequest request,
                                   String resolvedAction,
                                   String deviceId,
                                   DeviceControlResponse.DeviceResult.DeviceResultBuilder resultBuilder,
                                   HardwareTcpGateway tcpGateway) {
        String payload = HardwareCommandFormatter.formatCommand(resolvedAction, request.getParameters(), request.getValue());
        if (StrUtil.isBlank(payload)) {
            persistControlLog(request, resolvedAction, null, "FAILED", deviceId);
            resultBuilder.status("FAILED").message("无法构建硬件控制指令");
            return;
        }

        DeviceControlLog logEntry = persistControlLog(request, resolvedAction, payload, "PENDING", deviceId);
        boolean sendResult = tcpGateway.sendCommand(deviceId, payload, logEntry.getId());
        if (sendResult) {
            resultBuilder.status("PENDING").message("指令已写入 TCP，等待硬件回执");
        } else {
            deviceControlLogMapper.updateStatusAndResponse(logEntry.getId(), "FAILED", "TCP_SEND_FAILED");
            resultBuilder.status("FAILED").message("TCP 通道不可用");
        }
    }

    private void handleMqttDispatch(DeviceControlRequest request,
                                    String resolvedAction,
                                    String deviceId,
                                    DeviceControlResponse.DeviceResult.DeviceResultBuilder resultBuilder,
                                    MqttConfig.MqttGateway mqtt) {
        Map<String, Object> payload = buildPayload(request, resolvedAction, deviceId);
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.error("序列化控制指令失败", ex);
            throw new IllegalStateException("序列化失败: " + ex.getMessage(), ex);
        }

        DeviceControlLog logEntry = persistControlLog(request, resolvedAction, jsonPayload, "QUEUED", deviceId);
        try {
            mqtt.sendToMqtt(buildControlTopic(deviceId), jsonPayload);
            log.info("MQTT 指令已下发，deviceId={}, payload={}", deviceId, jsonPayload);
            deviceControlLogMapper.updateStatusAndResponse(logEntry.getId(), "SUCCESS", jsonPayload);
            resultBuilder.status("SUCCESS").message("MQTT 指令已发送");
        } catch (Exception ex) {
            log.error("发送 MQTT 控制指令失败", ex);
            deviceControlLogMapper.updateStatusAndResponse(logEntry.getId(), "FAILED", ex.getMessage());
            throw new IllegalStateException("发送 MQTT 指令失败: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> buildPayload(DeviceControlRequest request, String resolvedAction, String deviceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", deviceId);
        payload.put("action", resolvedAction);
        if (request.getValue() != null) {
            payload.put("value", request.getValue());
        } else if (request.getParameters() != null && request.getParameters().containsKey("value")) {
            payload.put("value", request.getParameters().get("value"));
        }
        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            payload.put("parameters", request.getParameters());
        }
        return payload;
    }

    private String buildControlTopic(String deviceId) {
        return controlTopicPrefix + deviceId;
    }

    private DeviceControlLog persistControlLog(DeviceControlRequest request,
                                              String resolvedAction,
                                              String payload,
                                              String status,
                                              String deviceId) {
        DeviceControlLog logEntry = DeviceControlLog.builder()
                .taskId(request.getTaskId())
                .deviceId(deviceId)
                .command(resolvedAction)
                .response(payload)
                .status(status)
                .createdAt(new Date())
                .build();
        deviceControlLogMapper.insert(logEntry);
        return logEntry;
    }
}
