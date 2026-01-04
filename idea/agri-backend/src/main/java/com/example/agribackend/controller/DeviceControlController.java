package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.service.IoTDAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 设备控制接口
 * 前端网页通过此接口向ESP32发送控制命令
 */
@RestController
@RequestMapping("/api/device")
public class DeviceControlController {

    @Autowired
    private IoTDAService ioTDAService;

    /**
     * 控制水泵
     * 
     * @param state true=开启, false=关闭
     */
    @PostMapping("/pump")
    public Result<String> controlPump(@RequestBody Map<String, Boolean> request) {
        boolean state = request.getOrDefault("state", false);
        boolean success = ioTDAService.sendCommand("pump", state);
        return success ? Result.success("水泵控制命令已发送") : Result.error("命令发送失败");
    }

    /**
     * 控制风扇
     * 
     * @param state true=开启, false=关闭
     */
    @PostMapping("/fan")
    public Result<String> controlFan(@RequestBody Map<String, Boolean> request) {
        boolean state = request.getOrDefault("state", false);
        boolean success = ioTDAService.sendCommand("fan", state);
        return success ? Result.success("风扇控制命令已发送") : Result.error("命令发送失败");
    }

    /**
     * 控制照明灯
     * 
     * @param state true=开启, false=关闭
     */
    @PostMapping("/light")
    public Result<String> controlLight(@RequestBody Map<String, Boolean> request) {
        boolean state = request.getOrDefault("state", false);
        boolean success = ioTDAService.sendCommand("light", state);
        return success ? Result.success("照明灯控制命令已发送") : Result.error("命令发送失败");
    }

    /**
     * 切换手动/自动模式
     * 
     * @param manual true=手动模式, false=自动模式
     */
    @PostMapping("/mode")
    public Result<String> setMode(@RequestBody Map<String, Boolean> request) {
        boolean manual = request.getOrDefault("manual", false);
        boolean success = ioTDAService.sendCommand("manual", manual);
        return success ? Result.success("模式切换命令已发送") : Result.error("命令发送失败");
    }

    /**
     * 设置阈值
     */
    @PostMapping("/threshold")
    public Result<String> setThreshold(@RequestBody Map<String, Integer> request) {
        boolean success = true;

        if (request.containsKey("fanTemp")) {
            success &= ioTDAService.sendProperty("fanTempThreshold", request.get("fanTemp"));
        }
        if (request.containsKey("fanCO2")) {
            success &= ioTDAService.sendProperty("fanCO2Threshold", request.get("fanCO2"));
        }
        if (request.containsKey("pumpDrought")) {
            success &= ioTDAService.sendProperty("pumpDroughtThreshold", request.get("pumpDrought"));
        }
        if (request.containsKey("lightLux")) {
            success &= ioTDAService.sendProperty("lightLuxThreshold", request.get("lightLux"));
        }

        return success ? Result.success("阈值设置命令已发送") : Result.error("命令发送失败");
    }

    /**
     * 获取设备当前状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getDeviceStatus() {
        Map<String, Object> status = ioTDAService.getDeviceStatus();
        return Result.success(status);
    }
}
