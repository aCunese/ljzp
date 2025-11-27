package com.example.Kcsj.hardware.tcp;

import cn.hutool.core.util.StrUtil;
import com.example.Kcsj.dto.DeviceConnectionStatus;
import com.example.Kcsj.entity.SensorData;
import com.example.Kcsj.mapper.DeviceControlLogMapper;
import com.example.Kcsj.service.SensorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(HardwareTcpProperties.class)
@ConditionalOnProperty(prefix = "hardware.tcp", name = "enabled", havingValue = "true")
public class HardwareTcpService implements HardwareTcpGateway {

    private final HardwareTcpProperties properties;
    private final SensorService sensorService;
    private final DeviceControlLogMapper deviceControlLogMapper;

    private final Map<String, TcpSession> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void startSessions() {
        for (HardwareTcpProperties.DeviceEndpoint endpoint : properties.getResolvedDevices()) {
            TcpSession session = new TcpSession(endpoint.getDeviceId(), endpoint.getHost(), endpoint.getPort());
            sessions.put(endpoint.getDeviceId(), session);
            session.start();
        }
    }

    @PreDestroy
    public void shutdown() {
        sessions.values().forEach(TcpSession::stop);
        sessions.clear();
    }

    @Override
    public boolean sendCommand(String deviceId, String payload, Long logId) {
        if (StrUtil.isBlank(deviceId) || StrUtil.isBlank(payload)) {
            return false;
        }
        TcpSession session = sessions.get(deviceId);
        if (session == null) {
            log.warn("未找到设备 {} 的 TCP session", deviceId);
            return false;
        }
        return session.send(payload, logId);
    }

    @Override
    public boolean isOnline(String deviceId) {
        TcpSession session = sessions.get(deviceId);
        return session != null && session.isConnected();
    }

    @Override
    public List<DeviceConnectionStatus> listConnectionStatus() {
        List<DeviceConnectionStatus> results = new ArrayList<>();
        sessions.values().forEach(session -> results.add(session.toStatus()));
        return results;
    }

    private class TcpSession {
        private final String deviceId;
        private final String host;
        private final int port;

        private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "hardware-tcp-" + deviceId);
            t.setDaemon(true);
            return t;
        });

        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicReference<Long> pendingLogId = new AtomicReference<>();

        private volatile Socket socket;
        private volatile InputStreamReader reader;
        private volatile PrintWriter writer;
        private volatile Instant lastConnectedAt;
        private volatile Instant lastSeenAt;

        TcpSession(String deviceId, String host, int port) {
            this.deviceId = deviceId;
            this.host = host;
            this.port = port;
        }

        void start() {
            if (running.compareAndSet(false, true)) {
                executor.submit(this::runLoop);
            }
        }

        void stop() {
            running.set(false);
            closeResources();
            executor.shutdownNow();
        }

        boolean send(String payload, Long logId) {
            PrintWriter writerRef = this.writer;
            if (writerRef == null) {
                log.warn("设备 {} 未连接，发送失败", deviceId);
                return false;
            }
            synchronized (writerRef) {
                writerRef.print(payload);
                writerRef.flush();
            }
            if (logId != null) {
                pendingLogId.set(logId);
            }
            return true;
        }

        boolean isConnected() {
            Socket socketRef = this.socket;
            return socketRef != null && socketRef.isConnected() && !socketRef.isClosed();
        }

        DeviceConnectionStatus toStatus() {
            return DeviceConnectionStatus.builder()
                    .deviceId(deviceId)
                    .connected(isConnected())
                    .lastConnectedAt(lastConnectedAt)
                    .lastSeenAt(lastSeenAt)
                    .build();
        }

        private void runLoop() {
            while (running.get()) {
                try {
                    connect();
                    readLoop();
                } catch (Exception ex) {
                    log.warn("设备 {} TCP 连接异常: {}", deviceId, ex.getMessage());
                } finally {
                    closeResources();
                }
                sleep(properties.getReconnectDelay().toMillis());
            }
        }

        private void connect() throws IOException {
            Socket sock = new Socket();
            sock.connect(new InetSocketAddress(host, port), (int) properties.getConnectTimeout().toMillis());
            sock.setSoTimeout((int) properties.getReadTimeout().toMillis());
            this.socket = sock;
            this.reader = new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8);
            this.writer = new PrintWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8), true);
            this.lastConnectedAt = Instant.now();
            log.info("设备 {} 已连接 {}:{}, 等待数据", deviceId, host, port);
        }

        private void readLoop() throws IOException {
            char[] buffer = new char[1024];
            StringBuilder chunk = new StringBuilder();
            int len;
            while (running.get() && (len = reader.read(buffer)) != -1) {
                lastSeenAt = Instant.now();
                chunk.append(buffer, 0, len);
                handleIncoming(chunk.toString());
                chunk.setLength(0);
            }
        }

        private void handleIncoming(String message) {
            if (StrUtil.isBlank(message)) {
                return;
            }
            String trimmed = message.trim();
            if (isAck(trimmed)) {
                handleAck(trimmed);
                return;
            }
            SensorData sensorData = HardwareTelemetryParser.parse(message, deviceId);
            if (sensorData != null) {
                sensorService.saveSensorData(sensorData);
            }
        }

        private boolean isAck(String message) {
            String low = message.toLowerCase();
            return "ok".equals(low) || "err".equals(low) || "error".equals(low);
        }

        private void handleAck(String ack) {
            Long logId = pendingLogId.getAndSet(null);
            if (logId == null) {
                return;
            }
            String status = ack.equalsIgnoreCase("ok") ? "OK" : "ERR";
            deviceControlLogMapper.updateStatusAndResponse(logId, status, ack);
        }

        private void closeResources() {
            tryClose(reader);
            tryClose(writer);
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
            reader = null;
            writer = null;
            socket = null;
        }

        private void tryClose(AutoCloseable closeable) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                }
            }
        }

        private void sleep(long millis) {
            if (millis <= 0) {
                return;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(millis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
