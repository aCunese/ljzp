package com.example.Kcsj.controller;

import com.example.Kcsj.common.Result;
import com.example.Kcsj.dto.DeviceControlRequest;
import com.example.Kcsj.dto.DeviceControlResponse;
import com.example.Kcsj.hardware.tcp.HardwareTcpGateway;
import com.example.Kcsj.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final DeviceService deviceService;
    private final java.util.Optional<HardwareTcpGateway> hardwareTcpGateway;

    @PostMapping("/control")
    public Result<?> controlDevice(@RequestBody DeviceControlRequest request) {
        try {
            DeviceControlResponse response = deviceService.executeControl(request);
            return Result.success(response);
        } catch (IllegalArgumentException ex) {
            log.warn("控制指令参数错误", ex);
            return Result.error(-1, ex.getMessage());
        } catch (Exception ex) {
            log.error("发送控制指令失败", ex);
            return Result.error(-1, "发送控制指令失败: " + ex.getMessage());
        }
    }

    @PostMapping("/execute")
    public Result<?> execute(@RequestBody DeviceControlRequest request) {
        return controlDevice(request);
    }

    @GetMapping("/connections")
    public Result<?> listConnections() {
        return hardwareTcpGateway
                .map(HardwareTcpGateway::listConnectionStatus)
                .map(Result::success)
                .orElse(Result.error(-1, "TCP 硬件网关未启用"));
    }
}
