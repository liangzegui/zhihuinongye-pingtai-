package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.entity.ControlHistoryEntity;
import com.example.agribackend.mapper.ControlHistoryMapper;
import com.example.agribackend.service.Esp32BridgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * 设备控制接口
 * 前端网页通过此接口向ESP32发送控制命令
 */
@RestController
@RequestMapping("/api/device")
public class DeviceControlController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceControlController.class);

    @Autowired
    private Esp32BridgeService esp32BridgeService;

    @Autowired
    private ControlHistoryMapper controlHistoryMapper;

    /**
     * 统一设备控制接口
     * 支持同时控制多个设备和设置阈值
     */
    @PostMapping("/control")
    public Result<String> controlDevice(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        boolean success = true;
        StringBuilder message = new StringBuilder();

        // 控制水泵
        if (request.containsKey("pump")) {
            boolean state = Boolean.TRUE.equals(request.get("pump"));
            success &= esp32BridgeService.controlPump(state);
            message.append("水泵").append(state ? "开启" : "关闭").append("; ");
        }

        // 控制风扇
        if (request.containsKey("fan")) {
            boolean state = Boolean.TRUE.equals(request.get("fan"));
            success &= esp32BridgeService.controlFan(state);
            message.append("风扇").append(state ? "开启" : "关闭").append("; ");
        }

        // 控制照明灯
        if (request.containsKey("light")) {
            boolean state = Boolean.TRUE.equals(request.get("light"));
            success &= esp32BridgeService.controlLight(state);
            message.append("照明").append(state ? "开启" : "关闭").append("; ");
        }

        // 切换手动/自动模式
        if (request.containsKey("manual")) {
            boolean manual = Boolean.TRUE.equals(request.get("manual"));
            success &= esp32BridgeService.setMode(manual);
            message.append("模式切换为").append(manual ? "手动" : "自动").append("; ");
        }

        // 设置阈值
        Map<String, Object> thresholds = new HashMap<>();
        if (request.containsKey("fanTempThreshold")) {
            thresholds.put("fanTemp", request.get("fanTempThreshold"));
        }
        if (request.containsKey("fanTemp")) {
            thresholds.put("fanTemp", request.get("fanTemp"));
        }
        if (request.containsKey("fanCO2Threshold")) {
            thresholds.put("fanCO2", request.get("fanCO2Threshold"));
        }
        if (request.containsKey("fanCO2")) {
            thresholds.put("fanCO2", request.get("fanCO2"));
        }
        if (request.containsKey("pumpDroughtThreshold")) {
            thresholds.put("pumpDrought", request.get("pumpDroughtThreshold"));
        }
        if (request.containsKey("pumpDrought")) {
            thresholds.put("pumpDrought", request.get("pumpDrought"));
        }
        if (request.containsKey("lightLuxThreshold")) {
            thresholds.put("lightLux", request.get("lightLuxThreshold"));
        }
        if (request.containsKey("lightLux")) {
            thresholds.put("lightLux", request.get("lightLux"));
        }

        if (!thresholds.isEmpty()) {
            success &= esp32BridgeService.setThresholds(thresholds);
            message.append("阈值已下发; ");
        }

        if (message.length() == 0) {
            return Result.error(400, "未提供有效的控制参数");
        }

        // 记录控制操作历史
        saveControlHistory(request, message.toString(), httpRequest, success);

        return success ? Result.success(message.toString()) : Result.error(500, "部分命令发送失败");
    }

    /** 保存设备控制操作到历史记录表 */
    private void saveControlHistory(Map<String, Object> request, String message,
                                     HttpServletRequest httpRequest, boolean success) {
        try {
            logger.info("准备保存控制历史, request={}, message={}, success={}", request, message, success);
            String controlType = "unknown";
            if (request.containsKey("pump")) controlType = "pump";
            else if (request.containsKey("fan")) controlType = "fan";
            else if (request.containsKey("light")) controlType = "light";
            else if (request.containsKey("manual")) controlType = "mode";
            else controlType = "threshold";

            String operator = httpRequest.getAttribute("loginUsername") != null
                    ? String.valueOf(httpRequest.getAttribute("loginUsername")) : "unknown";

            ControlHistoryEntity history = new ControlHistoryEntity();
            history.setDeviceId("esp32-001");
            history.setControlType(controlType);
            history.setControlValue(message.trim());
            history.setControlSource("manual");
            history.setOperator(operator);
            history.setResult(success ? "success" : "fail");
            history.setCreateTime(java.time.LocalDateTime.now());
            controlHistoryMapper.insert(history);
        } catch (Exception e) {
            logger.warn("保存控制历史记录失败: {}", e.getMessage());
        }
    }

    /**
     * 控制水泵
     * 
     * @param state true=开启, false=关闭
     */
    @PostMapping("/pump")
    public Result<String> controlPump(@RequestBody Map<String, Boolean> request, HttpServletRequest httpRequest) {
        boolean state = request.getOrDefault("state", false);
        boolean success = esp32BridgeService.controlPump(state);
        // 记录控制历史（兼容单设备接口）
        Map<String, Object> saveReq = new HashMap<>();
        saveReq.put("pump", state);
        saveControlHistory(saveReq, "水泵" + (state ? "开启" : "关闭"), httpRequest, success);
        return success ? Result.success("水泵控制命令已发送") : Result.error(500, "命令发送失败");
    }

    /**
     * 控制风扇
     * 
     * @param state true=开启, false=关闭
     */
    @PostMapping("/fan")
    public Result<String> controlFan(@RequestBody Map<String, Boolean> request, HttpServletRequest httpRequest) {
        boolean state = request.getOrDefault("state", false);
        boolean success = esp32BridgeService.controlFan(state);
        // 记录控制历史（兼容单设备接口）
        Map<String, Object> saveReq = new HashMap<>();
        saveReq.put("fan", state);
        saveControlHistory(saveReq, "风扇" + (state ? "开启" : "关闭"), httpRequest, success);
        return success ? Result.success("风扇控制命令已发送") : Result.error(500, "命令发送失败");
    }

    /**
     * 控制照明灯
     * 
     * @param state true=开启, false=关闭
     */
    @PostMapping("/light")
    public Result<String> controlLight(@RequestBody Map<String, Boolean> request, HttpServletRequest httpRequest) {
        boolean state = request.getOrDefault("state", false);
        boolean success = esp32BridgeService.controlLight(state);
        // 记录控制历史（兼容单设备接口）
        Map<String, Object> saveReq = new HashMap<>();
        saveReq.put("light", state);
        saveControlHistory(saveReq, "照明" + (state ? "开启" : "关闭"), httpRequest, success);
        return success ? Result.success("照明灯控制命令已发送") : Result.error(500, "命令发送失败");
    }

    /**
     * 切换手动/自动模式
     * 
     * @param manual true=手动模式, false=自动模式
     */
    @PostMapping("/mode")
    public Result<String> setMode(@RequestBody Map<String, Boolean> request, HttpServletRequest httpRequest) {
        boolean manual = request.getOrDefault("manual", false);
        boolean success = esp32BridgeService.setMode(manual);
        Map<String, Object> saveReq = new HashMap<>();
        saveReq.put("manual", manual);
        saveControlHistory(saveReq, "模式切换为" + (manual ? "手动" : "自动"), httpRequest, success);
        return success ? Result.success("模式切换命令已发送") : Result.error(500, "命令发送失败");
    }

    /**
     * 设置阈值
     */
    @PostMapping("/threshold")
    public Result<String> setThreshold(@RequestBody Map<String, Integer> request, HttpServletRequest httpRequest) {
        Map<String, Object> thresholds = new HashMap<>();
        if (request.containsKey("fanTemp")) {
            thresholds.put("fanTemp", request.get("fanTemp"));
        }
        if (request.containsKey("fanCO2")) {
            thresholds.put("fanCO2", request.get("fanCO2"));
        }
        if (request.containsKey("pumpDrought")) {
            thresholds.put("pumpDrought", request.get("pumpDrought"));
        }
        if (request.containsKey("lightLux")) {
            thresholds.put("lightLux", request.get("lightLux"));
        }

        boolean success = !thresholds.isEmpty() && esp32BridgeService.setThresholds(thresholds);
        if (!thresholds.isEmpty()) {
            saveControlHistory(thresholds, "阈值下发: " + thresholds.toString(), httpRequest, success);
        }
        return success ? Result.success("阈值设置命令已发送") : Result.error(500, "命令发送失败");
    }

    /**
     * 获取设备当前状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getDeviceStatus() {
        Map<String, Object> status = esp32BridgeService.buildStatusSnapshot();
        if (status.isEmpty()) {
            return Result.error(502, "无法连接到ESP32设备");
        }
        return Result.success(status);
    }
}
