package com.example.Kcsj.hardware.tcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "hardware.tcp")
public class HardwareTcpProperties {

    /**
     * 是否启用 TCP 硬件通道
     */
    private boolean enabled = false;

    /**
     * 默认单设备配置
     */
    private String host = "192.168.4.1";
    private int port = 8080;
    private String defaultDeviceId = "DEVICE_001";

    /**
     * 连接/读取参数
     */
    private Duration reconnectDelay = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(6);
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * 多设备扩展配置
     */
    private List<DeviceEndpoint> devices = new ArrayList<>();

    public List<DeviceEndpoint> getResolvedDevices() {
        if (devices != null && !devices.isEmpty()) {
            return devices;
        }
        DeviceEndpoint endpoint = new DeviceEndpoint();
        endpoint.setDeviceId(defaultDeviceId);
        endpoint.setHost(host);
        endpoint.setPort(port);
        return Collections.singletonList(endpoint);
    }

    @Data
    public static class DeviceEndpoint {
        private String deviceId;
        private String host;
        private int port = 8080;
    }
}
