package com.example.agribackend.service.impl;

import com.example.agribackend.dto.WarningLogDTO;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.entity.WarningLogEntity;
import com.example.agribackend.entity.WarningRuleEntity;
import com.example.agribackend.mapper.EnvDataMapper;
import com.example.agribackend.mapper.WarningLogMapper;
import com.example.agribackend.mapper.WarningRuleMapper;
import com.example.agribackend.service.ExceptionConfigService;
import com.example.agribackend.service.WarningService;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class WarningServiceImpl implements WarningService {

    private static final Logger logger = LoggerFactory.getLogger(WarningServiceImpl.class);

    @Autowired
    private EnvDataMapper envDataMapper;
    @Autowired
    private WarningRuleMapper warningRuleMapper;
    @Autowired
    private WarningLogMapper warningLogMapper;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ExceptionConfigService exceptionConfigService;

    // ==================== 定时预警检查（每分钟） ====================

    @Scheduled(fixedRate = 60000)
    @Override
    public void checkWarnings() {
        try {
            // 0. 读取异常配置——检测总开关
            if (!exceptionConfigService.getBooleanConfig("detection_enabled", true)) {
                logger.debug("异常检测已关闭，跳过本次检查");
                return;
            }

            // 1. 取最新一条环境数据
            QueryWrapper<EnvDataEntity> envWrapper = new QueryWrapper<>();
            envWrapper.orderByDesc("collect_time").last("LIMIT 1");
            EnvDataEntity latest = envDataMapper.selectOne(envWrapper);
            if (latest == null)
                return;

            // 2. 查询启用的规则
            QueryWrapper<WarningRuleEntity> ruleWrapper = new QueryWrapper<>();
            ruleWrapper.eq("enabled", 1);
            List<WarningRuleEntity> rules = warningRuleMapper.selectList(ruleWrapper);
            if (rules.isEmpty())
                return;

            // 3. 读取各传感器检测开关
            boolean tempEnabled = exceptionConfigService.getBooleanConfig("detection_temp_enabled", true);
            boolean humidityEnabled = exceptionConfigService.getBooleanConfig("detection_humidity_enabled", true);
            boolean soilEnabled = exceptionConfigService.getBooleanConfig("detection_soil_enabled", true);
            boolean lightEnabled = exceptionConfigService.getBooleanConfig("detection_light_enabled", true);
            boolean co2Enabled = exceptionConfigService.getBooleanConfig("detection_co2_enabled", true);

            // 4. 读取冷却时间（分钟）
            int cooldownMinutes = exceptionConfigService.getIntConfig("handling_cooldown", 5);

            // 5. 读取严重程度比例
            double warningRatio = exceptionConfigService.getDoubleConfig("severity_warning_ratio", 1.0);
            double dangerRatio = exceptionConfigService.getDoubleConfig("severity_danger_ratio", 1.5);
            double criticalRatio = exceptionConfigService.getDoubleConfig("severity_critical_ratio", 2.0);

            // 6. 读取是否自动处理
            boolean autoHandle = exceptionConfigService.getBooleanConfig("handling_auto_handle", false);

            Integer sensorId = latest.getSensorId();

            for (WarningRuleEntity rule : rules) {
                String type = rule.getSensorType();
                Double min = rule.getMinValue();
                Double max = rule.getMaxValue();
                Integer ruleId = rule.getId();

                // ---------- 温度 ----------
                if (tempEnabled && "temperature".equals(type) && latest.getTemperature() != null) {
                    checkAndInsert(sensorId, ruleId, "temperature", "温度",
                            latest.getTemperature(), min, max, "°C",
                            cooldownMinutes, warningRatio, dangerRatio, criticalRatio, autoHandle);
                }

                // ---------- 湿度 ----------
                if (humidityEnabled && "humidity".equals(type) && latest.getHumidity() != null) {
                    checkAndInsert(sensorId, ruleId, "humidity", "湿度",
                            latest.getHumidity(), min, max, "%",
                            cooldownMinutes, warningRatio, dangerRatio, criticalRatio, autoHandle);
                }

                // ---------- 兼容旧规则：temp_hum类型同时检查温度和湿度（使用相同阈值） ----------
                if ("temp_hum".equals(type)) {
                    if (tempEnabled && latest.getTemperature() != null) {
                        checkAndInsert(sensorId, ruleId, "temperature", "温度",
                                latest.getTemperature(), min, max, "°C",
                                cooldownMinutes, warningRatio, dangerRatio, criticalRatio, autoHandle);
                    }
                    if (humidityEnabled && latest.getHumidity() != null) {
                        checkAndInsert(sensorId, ruleId, "humidity", "湿度",
                                latest.getHumidity(), min, max, "%",
                                cooldownMinutes, warningRatio, dangerRatio, criticalRatio, autoHandle);
                    }
                }

                // ---------- 土壤湿度(ADC) ----------
                if (soilEnabled && "soil".equals(type) && latest.getSoilAdc() != null) {
                    checkAndInsert(sensorId, ruleId, "soil", "土壤湿度",
                            latest.getSoilAdc().doubleValue(), min, max, "ADC",
                            cooldownMinutes, warningRatio, dangerRatio, criticalRatio, autoHandle);
                }

                // ---------- 光照强度 ----------
                if (lightEnabled && "light".equals(type) && latest.getLightIntensity() != null) {
                    checkAndInsert(sensorId, ruleId, "light", "光照强度",
                            latest.getLightIntensity().doubleValue(), min, max, "lux",
                            cooldownMinutes, warningRatio, dangerRatio, criticalRatio, autoHandle);
                }

                // ---------- CO₂ ----------
                if (co2Enabled && "co2".equals(type) && latest.getCo2() != null) {
                    checkAndInsert(sensorId, ruleId, "co2", "CO₂浓度",
                            latest.getCo2().doubleValue(), min, max, "ppm",
                            cooldownMinutes, warningRatio, dangerRatio, criticalRatio, autoHandle);
                }
            }
        } catch (Exception e) {
            logger.error("预警检查异常", e);
        }
    }

    /** 比较单个维度并插入预警日志（支持冷却、严重程度、自动处理） */
    private void checkAndInsert(Integer sensorId, Integer ruleId,
            String warningType, String label,
            double value, Double min, Double max, String unit,
            int cooldownMinutes,
            double warningRatio, double dangerRatio, double criticalRatio,
            boolean autoHandle) {
        String desc = null;
        Double threshold = null;
        double exceedAmount = 0;

        if (min != null && value < min) {
            desc = label + "低于阈值（当前 " + value + unit + " < " + min + unit + "）";
            threshold = min;
            exceedAmount = min - value;
        } else if (max != null && value > max) {
            desc = label + "高于阈值（当前 " + value + unit + " > " + max + unit + "）";
            threshold = max;
            exceedAmount = value - max;
        }

        if (desc != null) {
            // ===== 冷却检查：同类型异常在冷却期内不重复记录 =====
            if (cooldownMinutes > 0) {
                LocalDateTime cooldownStart = LocalDateTime.now().minusMinutes(cooldownMinutes);
                QueryWrapper<WarningLogEntity> cooldownQw = new QueryWrapper<>();
                cooldownQw.eq("warning_type", warningType)
                        .ge("trigger_time", cooldownStart)
                        .orderByDesc("trigger_time")
                        .last("LIMIT 1");
                WarningLogEntity recent = warningLogMapper.selectOne(cooldownQw);
                if (recent != null) {
                    logger.debug("{}类型异常在冷却期内，跳过（上次: {}）", warningType, recent.getTriggerTime());
                    return;
                }
            }

            // ===== 严重程度分级 =====
            double ratio = (threshold != null && threshold != 0) ? exceedAmount / Math.abs(threshold) : 0;
            String severity;
            if (ratio > criticalRatio) {
                severity = "严重";
            } else if (ratio > dangerRatio) {
                severity = "危险";
            } else {
                severity = "警告";
            }
            desc = "【" + severity + "】" + desc;

            WarningLogEntity log = new WarningLogEntity();
            log.setSensorId(sensorId);
            log.setRuleId(ruleId);
            log.setWarningType(warningType);
            log.setTriggerValue(value);
            log.setThreshold(threshold);
            log.setDescription(desc);
            log.setTriggerTime(LocalDateTime.now());
            // 自动处理：低级别（警告）自动标记为已处理
            log.setStatus(autoHandle && "警告".equals(severity) ? 1 : 0);
            log.setUserId(1);
            warningLogMapper.insert(log);
            logger.info("生成预警：{}", desc);
            // WebSocket 实时推送预警通知到前端
            pushWarningNotification(log, desc);
        }
    }

    /** 通过WebSocket推送预警通知到前端 */
    private void pushWarningNotification(WarningLogEntity log, String desc) {
        if (messagingTemplate == null)
            return;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", log.getId());
            payload.put("warningType", log.getWarningType());
            payload.put("triggerValue", log.getTriggerValue());
            payload.put("threshold", log.getThreshold());
            payload.put("description", desc);
            payload.put("triggerTime", log.getTriggerTime() != null ? log.getTriggerTime().toString() : null);
            payload.put("status", log.getStatus());
            messagingTemplate.convertAndSend("/topic/warnings", payload);
            logger.debug("预警已推送到WebSocket: {}", desc);
        } catch (Exception e) {
            logger.warn("WebSocket推送预警失败: {}", e.getMessage());
        }
    }

    // ==================== 全量查询（兼容旧接口） ====================

    @Override
    public List<WarningLogDTO> getWarningLogs() {
        List<WarningLogEntity> list = warningLogMapper.selectList(null);
        return BeanUtil.copyToList(list, WarningLogDTO.class);
    }

    // ==================== 分页 + 筛选 ====================

    @Override
    public Map<String, Object> getWarningLogsPaged(int page, int pageSize,
            String warningType, Integer status,
            String timeRange) {
        Page<WarningLogEntity> pageObj = new Page<>(page, pageSize);

        QueryWrapper<WarningLogEntity> qw = new QueryWrapper<>();
        // 类型筛选
        if (warningType != null && !warningType.isEmpty()) {
            qw.eq("warning_type", warningType);
        }
        // 状态筛选
        if (status != null) {
            qw.eq("status", status);
        }
        // 时间范围筛选
        if (timeRange != null && !timeRange.isEmpty()) {
            LocalDateTime start = resolveStartTime(timeRange);
            if (start != null) {
                qw.ge("trigger_time", start);
            }
        }
        qw.orderByDesc("trigger_time");

        Page<WarningLogEntity> resultPage = warningLogMapper.selectPage(pageObj, qw);

        List<WarningLogDTO> dtoList = BeanUtil.copyToList(resultPage.getRecords(), WarningLogDTO.class);

        Map<String, Object> result = new HashMap<>();
        result.put("list", dtoList);
        result.put("total", resultPage.getTotal());
        return result;
    }

    // ==================== 标记已处理 ====================

    @Override
    public boolean markAsHandled(Integer id) {
        UpdateWrapper<WarningLogEntity> uw = new UpdateWrapper<>();
        uw.eq("id", id).set("status", 1);
        return warningLogMapper.update(null, uw) > 0;
    }

    @Override
    public int batchMarkAsHandled(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            return 0;
        UpdateWrapper<WarningLogEntity> uw = new UpdateWrapper<>();
        uw.in("id", ids).set("status", 1);
        return warningLogMapper.update(null, uw);
    }

    @Override
    public int batchDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            return 0;
        return warningLogMapper.deleteByIds(ids);
    }

    @Override
    public int clearHandled() {
        QueryWrapper<WarningLogEntity> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        return warningLogMapper.delete(qw);
    }

    // ==================== 工具方法 ====================

    private LocalDateTime resolveStartTime(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange) {
            case "today" -> LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            case "week" -> now.minusDays(7);
            case "month" -> now.minusDays(30);
            default -> null;
        };
    }
}