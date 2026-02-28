package com.example.agribackend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异常检测配置实体
 * 存储异常检测、通知、处理、严重程度等系统级配置
 */
@Data
@TableName("t_exception_config")
public class ExceptionConfigEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 配置键（唯一） */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置分组：detection / notification / handling / severity */
    private String configGroup;

    /** 配置说明 */
    private String description;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
