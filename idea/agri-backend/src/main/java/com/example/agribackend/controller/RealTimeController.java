package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.dto.EnvDataDTO;
import com.example.agribackend.service.RealTimeService;
import com.example.agribackend.service.Esp32BridgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/api/realtime")
public class RealTimeController {
    private static final Logger logger = LoggerFactory.getLogger(RealTimeController.class);

    @Autowired
    private RealTimeService realTimeService;

    @Autowired
    private Esp32BridgeService esp32BridgeService;

    @GetMapping
    public Result<List<EnvDataDTO>> getData() {
        try {
            // 首选直接读取ESP32设备数据
            Map<String, Object> data = esp32BridgeService.fetchData();
            if (data != null && !data.isEmpty()) {
                EnvDataDTO dto = mapToDto(data);
                dto.setDataSource("device"); // 标记数据来源为设备
                dto.setIsRealTime(true); // 标记为实时数据
                logger.debug("成功获取ESP32数据: {}", dto);
                return Result.success(Collections.singletonList(dto));
            }

            // 兜底：使用数据库中的最新数据
            logger.info("ESP32数据获取失败或为空，使用数据库数据");
            List<EnvDataDTO> dbData = realTimeService.getLatestData();
            if (dbData != null && !dbData.isEmpty()) {
                // 标记数据来源为数据库，非实时数据
                for (EnvDataDTO dto : dbData) {
                    dto.setDataSource("database");
                    dto.setIsRealTime(false);
                }
                return Result.success(dbData);
            }

            logger.warn("数据库也无有效数据");
            return Result.success(Collections.emptyList());
        } catch (Exception e) {
            logger.error("获取实时数据异常", e);
            return Result.error(500, "获取实时数据失败: " + e.getMessage());
        }
    }

    private EnvDataDTO mapToDto(Map<String, Object> data) {
        try {
            EnvDataDTO dto = new EnvDataDTO();
            dto.setTemperature(readDouble(data, "dhtTemp", "temperature", "temp"));
            dto.setHumidity(readDouble(data, "dhtHumi", "humidity", "humi"));

            // soilAO 为原始ADC值(0-4095)，越大越干
            Double soilAdcValue = readDouble(data, "soilAO", "soil", "soilMoisture");
            if (soilAdcValue != null) {
                // 数据范围验证
                if (soilAdcValue < 0)
                    soilAdcValue = 0.0;
                if (soilAdcValue > 4095)
                    soilAdcValue = 4095.0;

                // 返回原始ADC值
                dto.setSoilAdc(soilAdcValue.intValue());
                // 同时计算百分比湿度作为参考
                double soilMoisture = Math.max(0, Math.min(100, (4095 - soilAdcValue) / 40.95));
                dto.setSoilMoisture(soilMoisture);
            }

            Double light = readDouble(data, "lightIntensity", "lightLux");
            if (light != null) {
                int lightValue = light.intValue();
                dto.setLightIntensity(Math.max(0, lightValue)); // 确保非负
            }

            Double co2 = readDouble(data, "eco2", "co2");
            if (co2 != null) {
                int co2Value = co2.intValue();
                dto.setCo2(Math.max(0, co2Value)); // 确保非负
            }

            return dto;
        } catch (Exception e) {
            logger.error("数据映射异常", e);
            throw new RuntimeException("数据映射失败", e);
        }
    }

    private Double readDouble(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (data == null || !data.containsKey(key))
                continue;

            Object value = data.get(key);
            if (value == null)
                continue;

            try {
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                if (value instanceof String) {
                    String str = ((String) value).trim();
                    if (!str.isEmpty()) {
                        return Double.parseDouble(str);
                    }
                }
            } catch (NumberFormatException e) {
                logger.debug("无法解析数据字段 {} 的值: {}", key, value);
                // 继续下一个key
            }
        }
        return null;
    }
}