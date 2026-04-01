package com.example.agribackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.agribackend.common.Result;
import com.example.agribackend.entity.ExceptionConfigEntity;
import com.example.agribackend.mapper.ExceptionConfigMapper;
import com.example.agribackend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 异常检测配置控制器
 * 提供异常检测、通知方式、处理策略、严重程度分级的配置管理
 */
@RestController
@RequestMapping("/api/exception-config")
public class ExceptionConfigController {

    @Autowired
    private ExceptionConfigMapper configMapper;

    @Autowired
    private UserService userService;

    /**
     * 获取所有异常配置（按分组返回）
     */
    @GetMapping
    public Result<Map<String, List<Map<String, Object>>>> getAllConfig() {
        List<ExceptionConfigEntity> list = configMapper.selectList(
                new QueryWrapper<ExceptionConfigEntity>().orderByAsc("id"));

        // 按 config_group 分组，每项返回 key/value/description
        Map<String, List<Map<String, Object>>> grouped = list.stream()
                .collect(Collectors.groupingBy(
                        ExceptionConfigEntity::getConfigGroup,
                        LinkedHashMap::new,
                        Collectors.mapping(entity -> {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("key", entity.getConfigKey());
                            item.put("value", entity.getConfigValue());
                            item.put("description", entity.getDescription());
                            return item;
                        }, Collectors.toList())));

        return Result.success(grouped);
    }

    /**
     * 获取指定分组的配置
     * 
     * @param group 分组名：detection / notification / handling / severity
     */
    @GetMapping("/group/{group}")
    public Result<Map<String, String>> getByGroup(@PathVariable String group) {
        List<ExceptionConfigEntity> list = configMapper.selectList(
                new QueryWrapper<ExceptionConfigEntity>().eq("config_group", group));

        Map<String, String> configMap = new LinkedHashMap<>();
        for (ExceptionConfigEntity entity : list) {
            configMap.put(entity.getConfigKey(), entity.getConfigValue());
        }
        return Result.success(configMap);
    }

    /**
     * 批量更新配置
     * 请求体格式：{ "detection_enabled": "true", "detection_interval": "30", ... }
     */
    @PutMapping
    public Result<String> updateConfig(@RequestBody Map<String, String> configs,
            HttpServletRequest httpRequest) {
        if (!isAdminRequest(httpRequest)) {
            return Result.error(403, "仅管理员可执行此操作");
        }
        if (configs == null || configs.isEmpty()) {
            return Result.error(400, "配置不能为空");
        }

        int updated = 0;
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 参数校验
            String error = validateConfig(key, value);
            if (error != null) {
                return Result.error(400, key + ": " + error);
            }

            UpdateWrapper<ExceptionConfigEntity> uw = new UpdateWrapper<>();
            uw.eq("config_key", key).set("config_value", value);
            int rows = configMapper.update(null, uw);
            if (rows > 0)
                updated++;
        }

        return Result.success("已更新 " + updated + " 项配置");
    }

    /**
     * 更新单个配置项
     */
    @PutMapping("/{key}")
    public Result<String> updateSingleConfig(@PathVariable String key,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        if (!isAdminRequest(httpRequest)) {
            return Result.error(403, "仅管理员可执行此操作");
        }
        String value = body.get("value");
        if (value == null) {
            return Result.error(400, "缺少 value 字段");
        }

        String error = validateConfig(key, value);
        if (error != null) {
            return Result.error(400, error);
        }

        UpdateWrapper<ExceptionConfigEntity> uw = new UpdateWrapper<>();
        uw.eq("config_key", key).set("config_value", value);
        int rows = configMapper.update(null, uw);
        return rows > 0 ? Result.success("配置已更新") : Result.error(404, "配置项不存在: " + key);
    }

    /**
     * 重置所有配置为默认值
     */
    @PostMapping("/reset")
    public Result<String> resetToDefaults(HttpServletRequest httpRequest) {
        if (!isAdminRequest(httpRequest)) {
            return Result.error(403, "仅管理员可执行此操作");
        }
        Map<String, String> defaults = getDefaultValues();
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            UpdateWrapper<ExceptionConfigEntity> uw = new UpdateWrapper<>();
            uw.eq("config_key", entry.getKey()).set("config_value", entry.getValue());
            configMapper.update(null, uw);
        }
        return Result.success("已恢复默认设置");
    }

    private boolean isAdminRequest(HttpServletRequest request) {
        Object loginUsername = request.getAttribute("loginUsername");
        if (loginUsername == null) {
            return false;
        }
        return userService.isAdmin(String.valueOf(loginUsername));
    }

    /** 配置项校验 */
    private String validateConfig(String key, String value) {
        if (key.contains("enabled") || key.equals("notify_websocket")
                || key.equals("notify_sound") || key.equals("notify_repeat")
                || key.equals("handling_auto_handle")) {
            if (!"true".equals(value) && !"false".equals(value)) {
                return "开关值只能为 true 或 false";
            }
        }
        if ("detection_interval".equals(key)) {
            try {
                int interval = Integer.parseInt(value);
                if (interval < 10 || interval > 3600)
                    return "检测间隔需在 10~3600 秒之间";
            } catch (NumberFormatException e) {
                return "检测间隔必须为整数";
            }
        }
        if ("handling_cooldown".equals(key)) {
            try {
                int cd = Integer.parseInt(value);
                if (cd < 1 || cd > 60)
                    return "冷却时间需在 1~60 分钟之间";
            } catch (NumberFormatException e) {
                return "冷却时间必须为整数";
            }
        }
        if ("notify_popup_duration".equals(key)) {
            try {
                int d = Integer.parseInt(value);
                if (d < 1 || d > 60)
                    return "弹窗时长需在 1~60 秒之间";
            } catch (NumberFormatException e) {
                return "弹窗时长必须为整数";
            }
        }
        if (key.contains("ratio")) {
            try {
                double r = Double.parseDouble(value);
                if (r < 0.1 || r > 10.0)
                    return "比例系数需在 0.1~10.0 之间";
            } catch (NumberFormatException e) {
                return "比例系数必须为数字";
            }
        }
        return null;
    }

    /** 默认值映射 */
    private Map<String, String> getDefaultValues() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("detection_enabled", "true");
        defaults.put("detection_interval", "60");
        defaults.put("detection_temp_enabled", "true");
        defaults.put("detection_humidity_enabled", "true");
        defaults.put("detection_soil_enabled", "true");
        defaults.put("detection_light_enabled", "true");
        defaults.put("detection_co2_enabled", "true");
        defaults.put("notify_websocket", "true");
        defaults.put("notify_sound", "true");
        defaults.put("notify_popup_duration", "8");
        defaults.put("notify_repeat", "false");
        defaults.put("handling_auto_handle", "false");
        defaults.put("handling_cooldown", "5");
        defaults.put("handling_max_logs", "1000");
        defaults.put("severity_warning_ratio", "1.0");
        defaults.put("severity_danger_ratio", "1.5");
        defaults.put("severity_critical_ratio", "2.0");
        return defaults;
    }
}
