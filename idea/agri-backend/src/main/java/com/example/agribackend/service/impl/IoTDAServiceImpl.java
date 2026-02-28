package com.example.agribackend.service.impl;

import com.example.agribackend.config.HuaweiCloudSigner;
import com.example.agribackend.service.IoTDAService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 华为云IoTDA服务实现
 * 通过IoTDA北向API发送命令和查询设备
 * 
 * 功能：
 * 1. 调用IoTDA API控制设备（水泵、风扇、照明灯）
 * 2. 设置设备阈值
 * 3. 查询设备状态
 * 4. 通过WebSocket推送实时数据
 * 
 * 使用前需要在application.properties中配置：
 * iotda.endpoint=https://iotda.cn-south-1.myhuaweicloud.com
 * iotda.project-id=您的项目ID
 * iotda.access-key=您的AK
 * iotda.secret-key=您的SK
 * iotda.device-id=69568516c00ccb6d4b302187_esp32-001
 */
@Service
public class IoTDAServiceImpl implements IoTDAService {

    // IoTDA配置 - 从application.properties读取
    @Value("${iotda.endpoint:https://iotda.cn-south-1.myhuaweicloud.com}")
    private String endpoint;

    @Value("${iotda.project-id:}")
    private String projectId;

    @Value("${iotda.access-key:}")
    private String accessKey;

    @Value("${iotda.secret-key:}")
    private String secretKey;

    @Value("${iotda.device-id:69568516c00ccb6d4b302187_esp32-001}")
    private String deviceId;

    @Value("${iotda.instance-id:}")
    private String instanceId;

    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;

    // WebSocket消息模板（用于推送实时数据）
    private final SimpMessagingTemplate messagingTemplate;

    // 缓存最新的设备状态（从IoTDA数据推送更新）
    private Map<String, Object> cachedDeviceStatus = new HashMap<>();

    // 缓存最新的传感器数据
    private Map<String, Object> cachedSensorData = new HashMap<>();

    public IoTDAServiceImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void init() {
        // 配置HTTP客户端
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        objectMapper = new ObjectMapper();

        // 初始化默认状态
        cachedDeviceStatus.put("pump", false);
        cachedDeviceStatus.put("fan", false);
        cachedDeviceStatus.put("light", false);
        cachedDeviceStatus.put("manual", false);
        cachedDeviceStatus.put("online", false);

        // 初始化默认传感器数据
        cachedSensorData.put("temperature", 0.0);
        cachedSensorData.put("humidity", 0.0);
        cachedSensorData.put("soilMoisture", 0.0);
        cachedSensorData.put("soilAdc", 0);
        cachedSensorData.put("lightIntensity", 0);
        cachedSensorData.put("co2", 400);
        cachedSensorData.put("tvoc", 0);

        System.out.println("====== IoTDA服务初始化 ======");
        System.out.println("Endpoint: " + endpoint);
        System.out.println("Project ID: "
                + (projectId.isEmpty() ? "未配置" : projectId.substring(0, Math.min(8, projectId.length())) + "..."));
        System.out.println("Device ID: " + deviceId);
        System.out.println("AK配置: " + (accessKey.isEmpty() ? "未配置" : "已配置"));
    }

    @Override
    public boolean sendCommand(String commandName, Object value) {
        // 先更新本地缓存
        cachedDeviceStatus.put(commandName, value);

        // 如果没有配置IoTDA凭证，仅更新本地状态（演示模式）
        if (accessKey.isEmpty() || secretKey.isEmpty() || projectId.isEmpty()) {
            System.out.println("[IoTDA-演示模式] 命令已缓存: " + commandName + "=" + value);
            broadcastDeviceStatus();
            return true;
        }

        // 本地演示模式：设置 IOTDA_DEMO_MODE=true 时跳过真实IoTDA调用
        boolean localDemoMode = Boolean.parseBoolean(System.getenv().getOrDefault("IOTDA_DEMO_MODE", "false"));
        if (localDemoMode) {
            System.out.println("[IoTDA-本地模式] 命令已缓存（跳过API调用）: " + commandName + "=" + value);
            broadcastDeviceStatus();
            return true;
        }

        try {
            // 构建IoTDA属性下发请求
            String path = String.format("/v5/iot/%s/devices/%s/properties", projectId, deviceId);
            String url = endpoint + path;

            // 构建请求体
            Map<String, Object> properties = new HashMap<>();
            properties.put(commandName, value);

            Map<String, Object> service = new HashMap<>();
            service.put("service_id", "default");
            service.put("properties", properties);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("services", new Object[] { service });

            String bodyJson = objectMapper.writeValueAsString(requestBody);

            // 获取主机名和区域
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            String region = extractRegion(host);

            // 使用AK/SK签名
            Map<String, String> headers = HuaweiCloudSigner.sign(
                    "PUT", host, path, null, null, bodyJson,
                    accessKey, secretKey, region, "iotda");

            // 添加实例ID（如果有）
            if (instanceId != null && !instanceId.isEmpty()) {
                headers.put("X-Instance-Id", instanceId);
            }

            // 构建请求
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create(bodyJson, MediaType.parse("application/json")));

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            Request request = requestBuilder.build();

            System.out.println("[IoTDA] 发送命令: " + commandName + "=" + value);
            System.out.println("[IoTDA] URL: " + url);

            // 发送请求
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                System.out.println("[IoTDA] 响应: " + response.code() + " - " + responseBody);

                if (response.isSuccessful()) {
                    broadcastDeviceStatus();
                    return true;
                } else {
                    System.err.println("[IoTDA] 命令发送失败: " + responseBody);
                    // 即使失败也广播状态（用于前端演示）
                    broadcastDeviceStatus();
                    return false;
                }
            }

        } catch (Exception e) {
            System.err.println("[IoTDA] 发送命令异常: " + e.getMessage());
            e.printStackTrace();
            // 即使失败也广播状态（用于前端演示）
            broadcastDeviceStatus();
            return true; // 暂时返回true用于测试
        }
    }

    @Override
    public boolean sendProperty(String propertyName, Object value) {
        return sendCommand(propertyName, value);
    }

    @Override
    public Map<String, Object> getDeviceStatus() {
        // 有真实凭证时查询云端状态，否则沿用本地演示状态
        if (!accessKey.isEmpty() && !secretKey.isEmpty() && !projectId.isEmpty()) {
            queryDeviceOnlineStatus();
        } else {
            cachedDeviceStatus.put("online", true);
            // 演示模式下保证有更新时间，避免上层判定为陈旧数据
            if (!cachedDeviceStatus.containsKey("lastUpdate")) {
                cachedDeviceStatus.put("lastUpdate", System.currentTimeMillis());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.putAll(cachedDeviceStatus);
        result.putAll(cachedSensorData);
        result.putIfAbsent("lastUpdate", System.currentTimeMillis());
        return result;
    }

    /**
     * 从IoTDA查询设备在线状态
     */
    private void queryDeviceOnlineStatus() {
        // 如果没有配置IoTDA凭证，使用演示模式（设备在线）
        if (accessKey.isEmpty() || secretKey.isEmpty() || projectId.isEmpty()) {
            cachedDeviceStatus.put("online", true);
            return;
        }

        try {
            // 构建IoTDA设备查询请求
            String path = String.format("/v5/iot/%s/devices/%s", projectId, deviceId);
            String url = endpoint + path;

            // 获取主机名和区域
            URI uri = URI.create(endpoint);
            String host = uri.getHost();
            String region = extractRegion(host);

            // 使用AK/SK签名
            Map<String, String> headers = HuaweiCloudSigner.sign(
                    "GET", host, path, null, null, null,
                    accessKey, secretKey, region, "iotda");

            // 添加实例ID（如果有）
            if (instanceId != null && !instanceId.isEmpty()) {
                headers.put("X-Instance-Id", instanceId);
            }

            // 构建请求
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .get();

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            Request request = requestBuilder.build();

            // 发送请求
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    // 解析响应，获取设备状态
                    @SuppressWarnings("unchecked")
                    Map<String, Object> deviceInfo = objectMapper.readValue(responseBody, Map.class);
                    String status = (String) deviceInfo.get("status");
                    boolean isOnline = "ONLINE".equalsIgnoreCase(status);
                    cachedDeviceStatus.put("online", isOnline);
                    System.out.println("[IoTDA] 设备状态查询: " + (isOnline ? "在线" : "离线"));
                } else {
                    System.err.println("[IoTDA] 查询设备状态失败: " + response.code() + " - " + responseBody);
                    // 查询失败时保持现有状态
                }
            }

        } catch (Exception e) {
            System.err.println("[IoTDA] 查询设备状态异常: " + e.getMessage());
            // 异常时保持现有状态
        }
    }

    /**
     * 更新缓存的设备状态（由Webhook调用）
     */
    public void updateDeviceStatus(Map<String, Object> newStatus) {
        // 分类更新状态和传感器数据
        for (Map.Entry<String, Object> entry : newStatus.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.equals("pump") || key.equals("fan") || key.equals("light") || key.equals("manual")) {
                cachedDeviceStatus.put(key, value);
            } else if (key.equals("temp") || key.equals("temperature")) {
                cachedSensorData.put("temperature", value);
            } else if (key.equals("humi") || key.equals("humidity")) {
                cachedSensorData.put("humidity", value);
            } else if (key.equals("soil") || key.equals("soilMoisture")) {
                cachedSensorData.put("soilMoisture", value);
            } else if (key.equals("soilAdc")) {
                cachedSensorData.put("soilAdc", value);
            } else if (key.equals("lightLux") || key.equals("lightIntensity")) {
                cachedSensorData.put("lightIntensity", value);
            } else if (key.equals("eco2") || key.equals("co2")) {
                cachedSensorData.put("co2", value);
            } else if (key.equals("tvoc")) {
                cachedSensorData.put("tvoc", value);
            }
        }

        cachedDeviceStatus.put("online", true);
        cachedDeviceStatus.put("lastUpdate", System.currentTimeMillis());

        // 广播更新到所有WebSocket客户端
        broadcastSensorData();
        broadcastDeviceStatus();
    }

    /**
     * 通过WebSocket广播传感器数据
     */
    public void broadcastSensorData() {
        try {
            Map<String, Object> data = new HashMap<>(cachedSensorData);
            data.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/sensor-data", data);
            System.out.println("[WebSocket] 广播传感器数据: " + data);
        } catch (Exception e) {
            System.err.println("[WebSocket] 广播失败: " + e.getMessage());
        }
    }

    /**
     * 通过WebSocket广播设备状态
     */
    public void broadcastDeviceStatus() {
        try {
            Map<String, Object> status = new HashMap<>(cachedDeviceStatus);
            status.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/device-status", status);
            System.out.println("[WebSocket] 广播设备状态: " + status);
        } catch (Exception e) {
            System.err.println("[WebSocket] 广播失败: " + e.getMessage());
        }
    }

    /**
     * 从主机名提取区域
     */
    private String extractRegion(String host) {
        // 格式: iotda.cn-south-1.myhuaweicloud.com
        String[] parts = host.split("\\.");
        if (parts.length >= 2) {
            return parts[1]; // cn-south-1
        }
        return "cn-south-1";
    }

    /**
     * 获取缓存的传感器数据
     */
    public Map<String, Object> getCachedSensorData() {
        return new HashMap<>(cachedSensorData);
    }
}
