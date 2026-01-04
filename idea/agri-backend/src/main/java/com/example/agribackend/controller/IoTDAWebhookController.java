package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.mapper.EnvDataMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 华为云IoTDA数据转发Webhook接收器
 * 用于接收华为云IoTDA平台转发的设备上报数据
 * 
 * 配置步骤：
 * 1. 在华为云IoTDA控制台 -> 规则 -> 数据转发 -> 创建规则
 * 2. 触发源选择：设备属性上报
 * 3. 转发目标选择：HTTP推送
 * 4. 推送地址填写：http://您的服务器IP:8080/api/iotda/webhook
 * 5. 如果是本地开发，需要使用内网穿透工具(如ngrok)获取公网地址
 */
@RestController
@RequestMapping("/api/iotda")
public class IoTDAWebhookController {

    @Autowired
    private EnvDataMapper envDataMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 接收华为云IoTDA数据转发的Webhook
     * IoTDA会以POST方式推送设备上报的数据
     */
    @PostMapping("/webhook")
    public Result<String> receiveIoTDAData(@RequestBody String payload) {
        try {
            System.out.println("====== 收到IoTDA数据推送 ======");
            System.out.println(payload);

            // 解析IoTDA推送的JSON数据
            JsonNode root = objectMapper.readTree(payload);

            // IoTDA数据转发格式通常为：
            // {
            // "resource": "device.property",
            // "event": "report",
            // "notify_data": {
            // "header": {...},
            // "body": {
            // "services": [{
            // "service_id": "default",
            // "properties": {
            // "temp": 25,
            // "humi": 60,
            // ...
            // }
            // }]
            // }
            // }
            // }

            JsonNode notifyData = root.path("notify_data");
            JsonNode body = notifyData.path("body");
            JsonNode services = body.path("services");

            if (services.isArray() && services.size() > 0) {
                JsonNode properties = services.get(0).path("properties");

                // 创建环境数据实体
                EnvDataEntity envData = new EnvDataEntity();
                envData.setSensorId(1); // 默认传感器ID，可根据设备ID映射

                // 从IoTDA数据中提取各项传感器值
                if (properties.has("temp")) {
                    envData.setTemperature(properties.get("temp").asDouble());
                }
                if (properties.has("humi")) {
                    envData.setHumidity(properties.get("humi").asDouble());
                }
                if (properties.has("soil")) {
                    // ESP32上报的是ADC值，需要转换为百分比（可选）
                    int soilAdc = properties.get("soil").asInt();
                    // 转换公式：ADC值越大越干，转换为湿度百分比
                    double soilMoisture = Math.max(0, Math.min(100, (4095 - soilAdc) / 40.95));
                    envData.setSoilMoisture(soilMoisture);
                }
                if (properties.has("lightLux")) {
                    envData.setLightIntensity(properties.get("lightLux").asInt());
                }
                if (properties.has("eco2")) {
                    envData.setCo2(properties.get("eco2").asInt());
                }

                envData.setCollectTime(LocalDateTime.now());

                // 保存到数据库
                envDataMapper.insert(envData);

                System.out.println("====== 数据已保存到数据库 ======");
                System.out.println("温度: " + envData.getTemperature() + "℃");
                System.out.println("湿度: " + envData.getHumidity() + "%");
                System.out.println("土壤湿度: " + envData.getSoilMoisture() + "%");
                System.out.println("光照: " + envData.getLightIntensity() + " lux");
                System.out.println("CO2: " + envData.getCo2() + " ppm");

                return Result.success("数据接收成功");
            }

            return Result.success("数据格式无效，但已接收");

        } catch (Exception e) {
            System.err.println("处理IoTDA数据失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 直接接收ESP32上报的数据（备用接口）
     * 如果不使用IoTDA数据转发，ESP32可以直接调用此接口
     */
    @PostMapping("/direct")
    public Result<String> receiveDirectData(@RequestBody String payload) {
        try {
            System.out.println("====== 收到ESP32直接上报数据 ======");
            System.out.println(payload);

            JsonNode root = objectMapper.readTree(payload);

            EnvDataEntity envData = new EnvDataEntity();
            envData.setSensorId(1);

            if (root.has("temp")) {
                envData.setTemperature(root.get("temp").asDouble());
            }
            if (root.has("humi")) {
                envData.setHumidity(root.get("humi").asDouble());
            }
            if (root.has("soil")) {
                int soilAdc = root.get("soil").asInt();
                double soilMoisture = Math.max(0, Math.min(100, (4095 - soilAdc) / 40.95));
                envData.setSoilMoisture(soilMoisture);
            }
            if (root.has("lightLux")) {
                envData.setLightIntensity(root.get("lightLux").asInt());
            }
            if (root.has("eco2")) {
                envData.setCo2(root.get("eco2").asInt());
            }

            envData.setCollectTime(LocalDateTime.now());
            envDataMapper.insert(envData);

            return Result.success("数据保存成功");

        } catch (Exception e) {
            System.err.println("处理直接上报数据失败: " + e.getMessage());
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口，用于验证服务是否可用
     */
    @GetMapping("/health")
    public Result<String> healthCheck() {
        return Result.success("IoTDA Webhook服务正常运行");
    }
}
