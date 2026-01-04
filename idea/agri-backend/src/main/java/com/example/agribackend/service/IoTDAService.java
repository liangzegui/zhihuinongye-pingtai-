package com.example.agribackend.service;

import java.util.Map;

/**
 * 华为云IoTDA服务接口
 * 用于与IoTDA平台交互，发送命令和查询设备状态
 */
public interface IoTDAService {

    /**
     * 向设备发送控制命令
     * 
     * @param commandName 命令名称 (pump, fan, light, manual)
     * @param value       命令值
     * @return 是否发送成功
     */
    boolean sendCommand(String commandName, Object value);

    /**
     * 向设备发送属性设置
     * 
     * @param propertyName 属性名称
     * @param value        属性值
     * @return 是否发送成功
     */
    boolean sendProperty(String propertyName, Object value);

    /**
     * 获取设备当前状态
     * 
     * @return 设备状态信息
     */
    Map<String, Object> getDeviceStatus();
}
