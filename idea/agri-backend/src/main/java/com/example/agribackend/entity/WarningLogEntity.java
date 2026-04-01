package com.example.agribackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_warning_log")
public class WarningLogEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer sensorId;
    private Integer ruleId;
    private String warningType; // temperature / humidity / soil / light / co2
    private Double triggerValue; // 触发预警的实际值
    private Double threshold; // 触发时的阈值
    private String description; // 预警描述文本
    private LocalDateTime triggerTime;
    private Integer status; // 0=未处理 1=已处理
    private Integer userId;
}