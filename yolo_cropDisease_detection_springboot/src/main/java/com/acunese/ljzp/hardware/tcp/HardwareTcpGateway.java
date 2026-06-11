package com.acunese.ljzp.hardware.tcp;

import com.acunese.ljzp.dto.DeviceConnectionStatus;

import java.util.List;

public interface HardwareTcpGateway {

    boolean sendCommand(String deviceId, String payload, Long logId);

    boolean isOnline(String deviceId);

    List<DeviceConnectionStatus> listConnectionStatus();
}
