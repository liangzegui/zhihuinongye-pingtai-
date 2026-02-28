package com.example.agribackend.service;

import java.util.Map;

/**
 * 异常配置服务接口
 * 提供异常检测配置的查询能力，供 WarningServiceImpl 等调用
 */
public interface ExceptionConfigService {

    /** 获取所有配置（key → value） */
    Map<String, String> getAllConfigMap();

    /** 获取某个分组的配置 */
    Map<String, String> getGroupConfig(String group);

    /** 获取单个配置值，若不存在则返回 defaultValue */
    String getConfigValue(String key, String defaultValue);

    /** 获取 boolean 类型配置 */
    boolean getBooleanConfig(String key, boolean defaultValue);

    /** 获取 int 类型配置 */
    int getIntConfig(String key, int defaultValue);

    /** 获取 double 类型配置 */
    double getDoubleConfig(String key, double defaultValue);
}
