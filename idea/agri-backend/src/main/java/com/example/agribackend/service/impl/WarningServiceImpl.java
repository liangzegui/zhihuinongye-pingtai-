package com.example.agribackend.service.impl;

import com.example.agribackend.dto.WarningLogDTO;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.entity.WarningLogEntity;
import com.example.agribackend.entity.WarningRuleEntity;
import com.example.agribackend.mapper.EnvDataMapper;
import com.example.agribackend.mapper.WarningLogMapper;
import com.example.agribackend.mapper.WarningRuleMapper;
import com.example.agribackend.service.WarningService;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WarningServiceImpl implements WarningService {
    @Autowired
    private EnvDataMapper envDataMapper;
    @Autowired
    private WarningRuleMapper warningRuleMapper;
    @Autowired
    private WarningLogMapper warningLogMapper;

    // 每分钟执行一次预警检查
    @Scheduled(fixedRate = 60000)
    @Override
    public void checkWarnings() {
        try {
            // 1. 查询最新的环境数据（按采集时间降序，取所有传感器的最新一条数据）
            QueryWrapper<EnvDataEntity> envWrapper = new QueryWrapper<>();
            envWrapper.orderByDesc("collect_time"); // 最新数据排在前面
            List<EnvDataEntity> latestEnvDataList = envDataMapper.selectList(envWrapper);
            if (latestEnvDataList.isEmpty()) {
                // 若没有环境数据，直接返回（避免空指针）
                return;
            }

            // 2. 查询所有启用的预警规则（假设 enabled=1 表示启用）
            QueryWrapper<WarningRuleEntity> ruleWrapper = new QueryWrapper<>();
            ruleWrapper.eq("enabled", 1); // 只查启用的规则
            List<WarningRuleEntity> warningRules = warningRuleMapper.selectList(ruleWrapper);
            if (warningRules.isEmpty()) {
                // 若没有启用的规则，直接返回
                return;
            }

            // 3. 循环最新环境数据，匹配规则并检查是否超标
            for (EnvDataEntity envData : latestEnvDataList) {
                Integer sensorId = envData.getSensorId(); // 当前数据的传感器ID
                Double temperature = envData.getTemperature(); // 温度值（可扩展湿度、CO2等）
                Double humidity = envData.getHumidity();
                Double soilMoisture = envData.getSoilMoisture();

                // 匹配该传感器类型对应的预警规则（假设传感器类型从EnvData或Sensor表获取，此处简化为按规则的sensorType匹配）
                for (WarningRuleEntity rule : warningRules) {
                    String ruleSensorType = rule.getSensorType(); // 规则对应的传感器类型（如"temp_hum"）
                    Double minValue = rule.getMinValue(); // 规则最小值
                    Double maxValue = rule.getMaxValue(); // 规则最大值
                    Integer ruleId = rule.getId(); // 规则ID

                    // --------------------------
                    // 示例1：检查温度是否超标
                    // --------------------------
                    if ("temp_hum".equals(ruleSensorType) && temperature != null) {
                        boolean isOverLimit = false;
                        String warningType = "";
                        Double triggerValue = temperature;

                        // 判断温度是否低于最小值或高于最大值
                        if (temperature < minValue) {
                            isOverLimit = true;
                            warningType = "温度低于阈值";
                        } else if (temperature > maxValue) {
                            isOverLimit = true;
                            warningType = "温度高于阈值";
                        }

                        // 若超标，创建预警日志并插入
                        if (isOverLimit) {
                            WarningLogEntity warningLog = new WarningLogEntity();
                            // 给所有必填字段赋值（关键！避免字段缺失）
                            warningLog.setSensorId(sensorId); // 传感器ID（从环境数据中获取）
                            warningLog.setRuleId(ruleId); // 触发的规则ID
                            warningLog.setWarningType(warningType); // 预警类型（如"温度高于阈值"）
                            warningLog.setTriggerValue(triggerValue); // 触发预警的具体值
                            warningLog.setTriggerTime(LocalDateTime.now()); // 预警触发时间（当前时间）
                            warningLog.setStatus(0); // 预警状态（0=未处理，1=已处理，可自定义）
                            warningLog.setUserId(1); // 暂设默认用户ID（后续可关联登录用户，此处用1占位）

                            // 插入预警日志到数据库
                            warningLogMapper.insert(warningLog);
                        }
                    }

                    // --------------------------
                    // 示例2：可扩展检查湿度、土壤湿度等（逻辑同上）
                    // --------------------------
                    if ("temp_hum".equals(ruleSensorType) && humidity != null) {
                        // 湿度超标判断逻辑...（参考温度逻辑）
                    }
                }
            }
        } catch (Exception e) {
            // 捕获异常并打印日志（避免定时任务因异常中断）
            e.printStackTrace();
        }
    }

    @Override
    public List<WarningLogDTO> getWarningLogs() {
        List<WarningLogEntity> list = warningLogMapper.selectList(null);
        return BeanUtil.copyToList(list, WarningLogDTO.class);
    }
}