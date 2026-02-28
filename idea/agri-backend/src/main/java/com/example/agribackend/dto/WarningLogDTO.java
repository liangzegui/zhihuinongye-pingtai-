package com.example.agribackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarningLogDTO {
    private Integer id;
    private Integer sensorId;
    private Integer ruleId;
    private String warningType;
    private Double triggerValue;
    private Double threshold;        // 触发时的阈值
    private String description;      // 预警描述
    private LocalDateTime triggerTime;
    private Integer status;
    private Integer userId;
}