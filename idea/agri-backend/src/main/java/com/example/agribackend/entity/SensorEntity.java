package com.example.agribackend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 传感器实体类，对应数据库表 t_sensor
 * 用于存储传感器基本信息，如名称、类型、位置和状态
 */
@Data
@TableName("t_sensor")
public class SensorEntity {
    /**
     * 主键ID，自增
     */
    @TableId
    private Integer id;

    /**
     * 传感器名称，必填
     */
    private String sensorName;

    /**
     * 传感器类型，必填，例如 'temp_hum' 表示温湿度
     */
    private String type;

    /**
     * 部署位置，可选
     */
    private String location;

    /**
     * 状态，1 表示启用，0 表示禁用，默认1
     */
    private Integer status;
}