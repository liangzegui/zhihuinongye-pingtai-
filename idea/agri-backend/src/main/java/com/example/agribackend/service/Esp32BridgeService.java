package com.example.agribackend.service;

import java.util.Map;

/**
 * 简单的 ESP32 HTTP 网关服务，用于直接访问局域网设备。
 */
public interface Esp32BridgeService {

    /**
     * 获取 /data 返回的最新传感器与状态数据。
     */
    Map<String, Object> fetchData();

    /**
     * 获取当前阈值。
     */
    Map<String, Object> fetchThresholds();

    /**
     * 测试设备连接是否可用。
     */
    boolean testConnection();

    boolean setMode(boolean manual);

    boolean controlPump(boolean state);

    boolean controlFan(boolean state);

    boolean controlLight(boolean state);

    boolean setThresholds(Map<String, Object> thresholds);

    /**
     * 聚合 /data 与阈值，形成前端所需的状态快照。
     */
    Map<String, Object> buildStatusSnapshot();
}
