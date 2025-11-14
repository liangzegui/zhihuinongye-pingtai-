package com.example.agribackend.dto;

import lombok.Data;

@Data
public class EnvDataDTO {
    private Integer id;
    private Integer sensorId;
    private Double temperature;
    private Double humidity;
    private Double soilMoisture;
    private Integer lightIntensity;
    private Integer co2;
}