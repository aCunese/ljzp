package com.acunese.ljzp.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
public class DeviceConnectionStatus implements Serializable {
    private String deviceId;
    private boolean connected;
    private Instant lastConnectedAt;
    private Instant lastSeenAt;
}
