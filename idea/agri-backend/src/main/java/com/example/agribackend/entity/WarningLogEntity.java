package com.example.agribackend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_warning_log")
public class WarningLogEntity {
    @TableId
    private Integer id;
    private Integer sensorId;
    private Integer ruleId;
    private String warningType;
    private Double triggerValue;
    private LocalDateTime triggerTime;
    private Integer status;
    private Integer userId;
}