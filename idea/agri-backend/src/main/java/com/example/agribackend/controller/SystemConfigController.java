package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.service.DataAutoSaveService;
import com.example.agribackend.service.impl.Esp32BridgeServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置控制器 - 管理ESP32等设备的连接配置
 */
@RestController
@RequestMapping("/api/config")
public class SystemConfigController {
    private static final Logger logger = LoggerFactory.getLogger(SystemConfigController.class);

    @Autowired
    private Esp32BridgeServiceImpl esp32BridgeService;

    @Autowired
    private DataAutoSaveService dataAutoSaveService;

    /**
     * 获取当前ESP32配置
     */
    @GetMapping("/esp32")
    public Result<Map<String, Object>> getEsp32Config() {
        Map<String, Object> config = new HashMap<>();
        config.put("baseUrl", esp32BridgeService.getBaseUrl());
        config.put("connected", esp32BridgeService.testConnection());
        return Result.success(config);
    }

    /**
     * 更新ESP32 IP地址
     */
    @PostMapping("/esp32")
    public Result<Map<String, Object>> updateEsp32Config(@RequestBody Map<String, String> configData) {
        String newBaseUrl = configData.get("baseUrl");

        if (newBaseUrl == null || newBaseUrl.trim().isEmpty()) {
            return Result.error(400, "IP地址不能为空");
        }

        // 格式化URL
        newBaseUrl = newBaseUrl.trim();
        if (!newBaseUrl.startsWith("http://") && !newBaseUrl.startsWith("https://")) {
            newBaseUrl = "http://" + newBaseUrl;
        }

        // 移除末尾斜杠
        if (newBaseUrl.endsWith("/")) {
            newBaseUrl = newBaseUrl.substring(0, newBaseUrl.length() - 1);
        }

        logger.info("更新ESP32地址: {}", newBaseUrl);
        esp32BridgeService.setBaseUrl(newBaseUrl);

        // 测试连接
        boolean connected = esp32BridgeService.testConnection();

        Map<String, Object> result = new HashMap<>();
        result.put("baseUrl", newBaseUrl);
        result.put("connected", connected);
        result.put("message", connected ? "连接成功" : "地址已保存，但连接失败，请检查设备是否在线");

        return Result.success(result);
    }

    /**
     * 测试ESP32连接
     */
    @GetMapping("/esp32/test")
    public Result<Map<String, Object>> testEsp32Connection() {
        boolean connected = esp32BridgeService.testConnection();
        Map<String, Object> result = new HashMap<>();
        result.put("connected", connected);
        result.put("baseUrl", esp32BridgeService.getBaseUrl());
        result.put("message", connected ? "连接正常" : "连接失败，请检查设备是否在线");
        return Result.success(result);
    }

    // ==================== 数据自动保存配置 ====================

    /**
     * 获取数据自动保存配置
     */
    @GetMapping("/autosave")
    public Result<Map<String, Object>> getAutoSaveConfig(HttpServletRequest request) {

        Map<String, Object> config = new HashMap<>();
        config.put("enabled", dataAutoSaveService.isEnabled());
        config.put("intervalSeconds", dataAutoSaveService.getSaveIntervalSeconds());

        long lastSaveTime = dataAutoSaveService.getLastSaveTime();
        if (lastSaveTime > 0) {
            LocalDateTime lastSave = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(lastSaveTime), ZoneId.systemDefault());
            config.put("lastSaveTime", lastSave.toString());
        } else {
            config.put("lastSaveTime", null);
        }

        return Result.success(config);
    }

    /**
     * 更新数据自动保存配置
     */
    @PostMapping("/autosave")
    public Result<Map<String, Object>> updateAutoSaveConfig(@RequestBody Map<String, Object> configData,
            HttpServletRequest request) {

        String loginUsername = String.valueOf(request.getAttribute("loginUsername"));

        if (configData.containsKey("enabled")) {
            boolean enabled = Boolean.parseBoolean(configData.get("enabled").toString());
            dataAutoSaveService.setEnabled(enabled, loginUsername);
        } else {
            dataAutoSaveService.setConfiguredBy(loginUsername);
        }

        if (configData.containsKey("intervalSeconds")) {
            long interval = Long.parseLong(configData.get("intervalSeconds").toString());
            dataAutoSaveService.setSaveIntervalSeconds(interval);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("enabled", dataAutoSaveService.isEnabled());
        result.put("intervalSeconds", dataAutoSaveService.getSaveIntervalSeconds());
        result.put("configuredBy", dataAutoSaveService.getConfiguredBy());
        result.put("message", "配置已更新");

        return Result.success(result);
    }

    /**
     * 手动触发保存数据（从ESP32获取）
     */
    @PostMapping("/autosave/trigger")
    public Result<Map<String, Object>> triggerSaveData(HttpServletRequest request) {

        String loginUsername = String.valueOf(request.getAttribute("loginUsername"));
        boolean success = dataAutoSaveService.manualSave(loginUsername);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "数据保存成功" : "数据保存失败：设备离线或无有效数据");

        return Result.success(result);
    }

    /**
     * 使用前端传入的数据保存到数据库
     */
    @PostMapping("/autosave/save")
    public Result<Map<String, Object>> saveWithData(@RequestBody Map<String, Object> data,
            HttpServletRequest request) {

        String loginUsername = String.valueOf(request.getAttribute("loginUsername"));
        boolean success = dataAutoSaveService.saveWithData(data, loginUsername);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "数据保存成功" : "保存失败：设备可能离线或传感器数据异常（多项指标为0）");

        return Result.success(result);
    }

    // ==================== 离线缓存间隔配置 ====================

    /**
     * 获取ESP32离线缓存间隔配置及SD卡状态
     */
    @GetMapping("/cacheInterval")
    public Result<Map<String, Object>> getCacheInterval() {
        try {
            Map<String, Object> data = esp32BridgeService.getCacheInterval();
            if (data == null || data.isEmpty()) {
                return Result.error(503, "ESP32未连接，无法获取缓存配置");
            }
            return Result.success(data);
        } catch (Exception e) {
            logger.error("获取缓存间隔配置失败", e);
            return Result.error(503, "ESP32未连接：" + e.getMessage());
        }
    }

    /**
     * 设置ESP32离线缓存间隔
     */
    @PostMapping("/cacheInterval")
    public Result<Map<String, Object>> setCacheInterval(@RequestBody Map<String, Object> body) {
        Object intervalObj = body.get("interval");
        if (intervalObj == null) {
            return Result.error(400, "缺少interval参数");
        }

        int interval;
        try {
            interval = Integer.parseInt(intervalObj.toString());
        } catch (NumberFormatException e) {
            return Result.error(400, "interval必须为整数");
        }

        if (interval < 5 || interval > 3600) {
            return Result.error(400, "interval范围为5-3600秒");
        }

        boolean success = esp32BridgeService.setCacheInterval(interval);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("interval", interval);
        result.put("message", success ? "缓存间隔已设置为" + interval + "秒" : "设置失败，请检查ESP32连接");
        return Result.success(result);
    }
}
