package com.example.Kcsj.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class DeviceControlResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String status;
    private String message;
    private String deviceId;
    private Date timestamp;
    private List<DeviceResult> deviceResults;

    @Data
    @Builder
    public static class DeviceResult implements Serializable {
        private String deviceId;
        private String status;
        private String message;
        private Date timestamp;
    }
}
