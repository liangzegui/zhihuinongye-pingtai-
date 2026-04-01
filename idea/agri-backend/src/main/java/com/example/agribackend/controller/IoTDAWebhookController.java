package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.mapper.EnvDataMapper;
import com.example.agribackend.service.IoTDAService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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

    private static final Logger logger = LoggerFactory.getLogger(IoTDAWebhookController.class);

    @Value("${iotda.webhook-token:}")
    private String webhookToken;

    @Autowired
    private EnvDataMapper envDataMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IoTDAService ioTDAService;

    /**
     * 接收华为云IoTDA数据转发的Webhook
     * IoTDA会以POST方式推送设备上报的数据
     */
    @PostMapping("/webhook")
    public Result<String> receiveIoTDAData(@RequestBody String payload,
            @RequestHeader(value = "X-Webhook-Token", required = false) String token) {
        if (!webhookToken.isEmpty() && !webhookToken.equals(token)) {
            logger.warn("Webhook token验证失败");
            return Result.error(401, "认证失败");
        }
        try {
            logger.info("====== 收到IoTDA数据推送 ======");
            logger.info("{}", payload);

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

                envData.setSaveUsername("IoTDA设备上报");
                envData.setCollectTime(LocalDateTime.now());

                // 异常数据检测：传感器离线或故障时返回全零，不应入库
                if (isAbnormalData(envData)) {
                    logger.warn("[数据检测] IoTDA上报数据异常（多项关键指标为0），跳过保存");
                    return Result.success("数据异常，已跳过保存");
                }

                // 保存到数据库
                envDataMapper.insert(envData);

                // 通过WebSocket广播实时数据
                Map<String, Object> sensorData = new HashMap<>();
                sensorData.put("temp", envData.getTemperature());
                sensorData.put("humi", envData.getHumidity());
                sensorData.put("soilMoisture", envData.getSoilMoisture());
                sensorData.put("lightLux", envData.getLightIntensity());
                sensorData.put("eco2", envData.getCo2());

                // 提取设备状态
                if (properties.has("pump")) {
                    sensorData.put("pump", properties.get("pump").asBoolean());
                }
                if (properties.has("fan")) {
                    sensorData.put("fan", properties.get("fan").asBoolean());
                }
                if (properties.has("light")) {
                    sensorData.put("light", properties.get("light").asBoolean());
                }
                if (properties.has("manual")) {
                    sensorData.put("manual", properties.get("manual").asBoolean());
                }

                // 更新IoTDA服务缓存并广播
                ioTDAService.updateDeviceStatus(sensorData);

                logger.info("====== 数据已保存并广播 ======");
                logger.info("温度: {}℃", envData.getTemperature());
                logger.info("湿度: {}%", envData.getHumidity());
                logger.info("土壤湿度: {}%", envData.getSoilMoisture());
                logger.info("光照: {} lux", envData.getLightIntensity());
                logger.info("CO2: {} ppm", envData.getCo2());

                return Result.success("数据接收成功");
            }

            return Result.success("数据格式无效，但已接收");

        } catch (Exception e) {
            logger.error("处理IoTDA数据失败: {}", e.getMessage());
            return Result.error(500, "处理失败: " + e.getMessage());
        }
    }

    /**
     * 直接接收ESP32上报的数据（备用接口）
     * 如果不使用IoTDA数据转发，ESP32可以直接调用此接口
     */
    @PostMapping("/direct")
    public Result<String> receiveDirectData(@RequestBody String payload,
            @RequestHeader(value = "X-Webhook-Token", required = false) String token) {
        if (!webhookToken.isEmpty() && !webhookToken.equals(token)) {
            logger.warn("Webhook token验证失败");
            return Result.error(401, "认证失败");
        }
        try {
            logger.info("====== 收到ESP32直接上报数据 ======");
            logger.info("{}", payload);

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

            envData.setSaveUsername("ESP32直传");
            envData.setCollectTime(LocalDateTime.now());

            // 异常数据检测
            if (isAbnormalData(envData)) {
                logger.warn("[数据检测] ESP32直传数据异常（多项关键指标为0），跳过保存");
                return Result.success("数据异常，已跳过保存");
            }

            envDataMapper.insert(envData);

            return Result.success("数据保存成功");

        } catch (Exception e) {
            logger.error("处理直接上报数据失败: {}", e.getMessage());
            return Result.error(500, "处理失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口，用于验证服务是否可用
     */
    @GetMapping("/health")
    public Result<String> healthCheck() {
        return Result.success("IoTDA Webhook服务正常运行");
    }

    /**
     * 检测异常数据：温度、湿度、土壤湿度、光照中3项及以上为零/null时视为传感器异常
     */
    private boolean isAbnormalData(EnvDataEntity entity) {
        int zeroCount = 0;
        if (entity.getTemperature() == null || entity.getTemperature() == 0.0)
            zeroCount++;
        if (entity.getHumidity() == null || entity.getHumidity() == 0.0)
            zeroCount++;
        if (entity.getSoilMoisture() == null || entity.getSoilMoisture() == 0.0)
            zeroCount++;
        if (entity.getLightIntensity() == null || entity.getLightIntensity() == 0)
            zeroCount++;
        return zeroCount >= 3;
    }
}
