package com.example.agribackend.dto;

import lombok.Data;

@Data
public class EnvDataDTO {
    private Integer id;
    private Integer sensorId;
    private Double temperature;
    private Double humidity;
    private Double soilMoisture;
    private Integer soilAdc; // 原始ADC值(0-4095)
    private Integer lightIntensity;
    private Integer co2;

    // 数据来源标记
    private String dataSource; // "device" = ESP32设备实时数据, "database" = 数据库历史数据
    private Boolean isRealTime; // true = 实时数据, false = 历史数据
    private String collectTime; // 数据采集时间

    // 断连缓存数据通知
    private Boolean hasCachedData; // ESP32是否刚恢复了缓存数据
    private Integer cachedDataCount; // 恢复的缓存数据条数
}