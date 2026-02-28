package com.example.agribackend.service.impl;

import com.example.agribackend.service.Esp32BridgeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class Esp32BridgeServiceImpl implements Esp32BridgeService {
    private static final Logger logger = LoggerFactory.getLogger(Esp32BridgeServiceImpl.class);

    @Value("${esp32.base-url:http://192.168.2.92}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Esp32BridgeServiceImpl(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 获取当前ESP32地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 动态设置ESP32地址
     */
    public void setBaseUrl(String newBaseUrl) {
        this.baseUrl = newBaseUrl;
        logger.info("[ESP32] 地址已更新为: {}", newBaseUrl);
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
