package com.example.agribackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备控制历史记录实体
 * 对应数据库表 t_control_history
 */
@Data
@TableName("t_control_history")
public class ControlHistoryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备ID */
    private String deviceId;

    /** 控制类型: pump/fan/light/mode/threshold */
    private String controlType;

    /** 控制值: on/off 或具体阈值 */
    private String controlValue;

    /** 控制来源: manual(手动) / auto(自动) */
    private String controlSource;

    /** 操作者用户名 */
    private String operator;

    /** 执行结果: success / fail */
    private String result;

    /** 操作时间 */
    private LocalDateTime createTime;
}
