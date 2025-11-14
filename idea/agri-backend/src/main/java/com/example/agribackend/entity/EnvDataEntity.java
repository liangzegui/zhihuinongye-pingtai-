package com.example.agribackend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_env_data")
public class EnvDataEntity {
    @TableId
    private Integer id;
    private Integer sensorId;// 关联传感器ID
    private Double temperature;// 温度
    private Double humidity;// 湿度
    private Double soilMoisture;// 光照
    private Integer lightIntensity;// 土壤湿度
    private Integer co2;//二氧化碳的浓度
    private LocalDateTime collectTime; // 采集时间
}