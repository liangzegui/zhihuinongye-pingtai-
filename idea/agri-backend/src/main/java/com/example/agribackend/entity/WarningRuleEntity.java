package com.example.agribackend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_warning_rule")
public class WarningRuleEntity {
    @TableId
    private Integer id;
    private String sensorType;
    private Double minValue;
    private Double maxValue;
    private Integer enabled;
}