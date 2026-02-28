package com.example.agribackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_env_data")
public class EnvDataEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer sensorId;// 关联传感器ID
    private Double temperature;// 温度
    private Double humidity;// 湿度
    private Double soilMoisture;// 土壤湿度百分比
    private Integer soilAdc;// 土壤湿度ADC原始值(0-4095)
    private Integer lightIntensity;// 光照强度
    private Integer co2;// 二氧化碳的浓度
    private String saveUsername;// 保存人用户名
    private LocalDateTime collectTime; // 采集时间
}