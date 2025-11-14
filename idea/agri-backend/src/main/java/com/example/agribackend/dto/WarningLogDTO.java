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
    private LocalDateTime triggerTime;
    private Integer status;
    private Integer userId;
}