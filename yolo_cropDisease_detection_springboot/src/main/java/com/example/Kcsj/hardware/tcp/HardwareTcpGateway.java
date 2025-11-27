package com.example.Kcsj.hardware.tcp;

import com.example.Kcsj.dto.DeviceConnectionStatus;

import java.util.List;

public interface HardwareTcpGateway {

    boolean sendCommand(String deviceId, String payload, Long logId);

    boolean isOnline(String deviceId);

    List<DeviceConnectionStatus> listConnectionStatus();
}
