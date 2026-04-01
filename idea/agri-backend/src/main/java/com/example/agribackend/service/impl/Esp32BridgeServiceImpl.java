package com.example.agribackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.agribackend.entity.ExceptionConfigEntity;
import com.example.agribackend.mapper.ExceptionConfigMapper;
import com.example.agribackend.service.Esp32BridgeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class Esp32BridgeServiceImpl implements Esp32BridgeService {
    private static final Logger logger = LoggerFactory.getLogger(Esp32BridgeServiceImpl.class);
    private static final String CONFIG_KEY = "esp32_base_url";
    private static final String CONFIG_GROUP = "system";

    @Value("${esp32.base-url:http://192.168.2.4}")
    private String baseUrl;

    @Autowired
    private ExceptionConfigMapper configMapper;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Esp32BridgeServiceImpl(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 启动时从数据库加载ESP32地址，如果数据库没有则用配置文件的默认值
     */
    @PostConstruct
    public void init() {
        try {
            ExceptionConfigEntity config = configMapper.selectOne(
                    new LambdaQueryWrapper<ExceptionConfigEntity>()
                            .eq(ExceptionConfigEntity::getConfigKey, CONFIG_KEY));
            if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
                this.baseUrl = config.getConfigValue();
                logger.info("[ESP32] 从数据库加载地址: {}", this.baseUrl);
            } else {
                // 数据库没有记录，将默认值存入数据库
                saveToDb(this.baseUrl);
                logger.info("[ESP32] 使用默认地址并存入数据库: {}", this.baseUrl);
            }
        } catch (Exception e) {
            logger.warn("[ESP32] 从数据库加载地址失败，使用默认值: {}", this.baseUrl, e);
        }
    }

    /**
     * 获取当前ESP32地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 动态设置ESP32地址，同时持久化到数据库
     */
    public void setBaseUrl(String newBaseUrl) {
        this.baseUrl = newBaseUrl;
        saveToDb(newBaseUrl);
        logger.info("[ESP32] 地址已更新并持久化: {}", newBaseUrl);
    }

    /**
     * 将ESP32地址保存到数据库
     */
    private void saveToDb(String url) {
        try {
            ExceptionConfigEntity config = configMapper.selectOne(
                    new LambdaQueryWrapper<ExceptionConfigEntity>()
                            .eq(ExceptionConfigEntity::getConfigKey, CONFIG_KEY));
            if (config != null) {
                config.setConfigValue(url);
                configMapper.updateById(config);
            } else {
                config = new ExceptionConfigEntity();
                config.setConfigKey(CONFIG_KEY);
                config.setConfigValue(url);
                config.setConfigGroup(CONFIG_GROUP);
                config.setDescription("ESP32单片机本地HTTP网关地址");
                configMapper.insert(config);
            }
        } catch (Exception e) {
            logger.error("[ESP32] 地址持久化到数据库失败", e);
        }
    }

    /**
     * 测试ESP32连接
     */
    public boolean testConnection() {
        try {
            Map<String, Object> data = fetchData();
            return data != null && !data.isEmpty();
        } catch (Exception e) {
            logger.warn("[ESP32] 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> fetchData() {
        return doGetJson("/data");
    }

    @Override
    public Map<String, Object> fetchThresholds() {
        return doGetJson("/getThresholds");
    }

    @Override
    public boolean setMode(boolean manual) {
        return sendStateCommand("/mode", manual);
    }

    @Override
    public boolean controlPump(boolean state) {
        return sendStateCommand("/pump", state);
    }

    @Override
    public boolean controlFan(boolean state) {
        return sendStateCommand("/fan", state);
    }

    @Override
    public boolean controlLight(boolean state) {
        return sendStateCommand("/light", state);
    }

    @Override
    public boolean setThresholds(Map<String, Object> thresholds) {
        try {
            if (thresholds == null || thresholds.isEmpty()) {
                logger.warn("[ESP32] 阈值参数为空");
                return false;
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/setThresholds");
            for (Map.Entry<String, Object> entry : thresholds.entrySet()) {
                if (entry.getValue() != null) {
                    builder.queryParam(entry.getKey(), entry.getValue());
                }
            }
            URI uri = builder.build(true).toUri();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                logger.info("[ESP32] 阈值设置成功");
            } else {
                logger.warn("[ESP32] 阈值设置失败，状态码: {}", response.getStatusCode());
            }
            return success;
        } catch (RestClientException e) {
            logger.error("[ESP32] 网络连接失败，无法设置阈值: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("[ESP32] 阈值设置异常", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> buildStatusSnapshot() {
        Map<String, Object> data = fetchData();
        Map<String, Object> thresholds = fetchThresholds();

        // 计算土壤湿度百分比（ADC越大越干）
        Double soilAdc = readDouble(data, "soilAO", "soil", "soilAdc");
        if (soilAdc != null) {
            // 数据范围验证
            if (soilAdc < 0)
                soilAdc = 0.0;
            if (soilAdc > 4095)
                soilAdc = 4095.0;

            double soilMoisture = Math.max(0, Math.min(100, (4095 - soilAdc) / 40.95));
            data.put("soilMoisture", soilMoisture);
            data.put("soilAdc", soilAdc);
        }

        Map<String, Object> status = new HashMap<>();
        status.put("online", !data.isEmpty());
        status.put("manualMode", readBoolean(data, "manualControl", "manual"));
        status.put("pumpState", readBoolean(data, "pumpState", "pump"));
        status.put("fanState", readBoolean(data, "fanState", "fan"));
        status.put("lightState", readBoolean(data, "lightState", "light"));
        status.put("thresholds", thresholds);
        status.put("lastUpdate", data.getOrDefault("lastUpdate", System.currentTimeMillis()));
        status.put("rawData", data);
        return status;
    }

    private Double readDouble(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble(((String) value).trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private Map<String, Object> doGetJson(String path) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(path)
                    .build(true)
                    .toUri();

            logger.debug("[ESP32] 请求: {}", uri);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(response.getBody(),
                        new TypeReference<Map<String, Object>>() {
                        });
                logger.debug("[ESP32] 响应数据: {}", result);
                return result;
            } else {
                logger.warn("[ESP32] 请求失败({})，状态码: {}", path, response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (RestClientException e) {
            logger.error("[ESP32] 网络连接失败({}): {}", path, e.getMessage());
            return Collections.emptyMap();
        } catch (Exception e) {
            logger.error("[ESP32] 请求异常({})", path, e);
            return Collections.emptyMap();
        }
    }

    private boolean sendStateCommand(String path, boolean state) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(path)
                    .queryParam("state", state ? 1 : 0)
                    .build(true)
                    .toUri();

            logger.debug("[ESP32] 发送命令: {} (state={})", uri, state);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            boolean success = response.getStatusCode().is2xxSuccessful();

            if (success) {
                logger.info("[ESP32] 命令执行成功: {}", path);
            } else {
                logger.warn("[ESP32] 命令执行失败: {}，状态码: {}", path, response.getStatusCode());
            }
            return success;
        } catch (RestClientException e) {
            logger.error("[ESP32] 网络连接失败({}): {}", path, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("[ESP32] 命令发送异常({})", path, e);
            return false;
        }
    }

    private boolean readBoolean(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
        }
        return false;
    }
}
