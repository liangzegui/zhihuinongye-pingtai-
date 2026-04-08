package com.example.agribackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.entity.ExceptionConfigEntity;
import com.example.agribackend.mapper.EnvDataMapper;
import com.example.agribackend.mapper.ExceptionConfigMapper;
import com.example.agribackend.service.Esp32BridgeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class Esp32BridgeServiceImpl implements Esp32BridgeService {
    private static final Logger logger = LoggerFactory.getLogger(Esp32BridgeServiceImpl.class);
    private static final String CONFIG_KEY = "esp32_base_url";
    private static final String CONFIG_GROUP = "system";

    @Value("${esp32.base-url:http://192.168.2.4}")
    private String baseUrl;

    @Autowired
    private ExceptionConfigMapper configMapper;

    @Autowired
    private EnvDataMapper envDataMapper;

    // 上次请求ESP32 /data的时间戳（毫秒）
    private volatile long lastFetchDataTime = 0;
    // 心跳间隔：确保ESP32每15秒至少被访问一次，避免30秒超时误判离线
    private static final long HEARTBEAT_INTERVAL_MS = 15_000;

    // 防止并发拉取缓存数据
    private final AtomicBoolean fetchingCachedData = new AtomicBoolean(false);
    // 待通知前端的缓存数据条数
    private final AtomicInteger pendingCachedCount = new AtomicInteger(0);
    // 上次成功拉取缓存数据的时间（防止短时间内重复拉取）
    private volatile long lastCachedFetchTime = 0;
    private static final long CACHED_FETCH_COOLDOWN_MS = 60_000; // 60秒冷却

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

    /**
     * 心跳定时任务：每15秒检查是否有其他调用方已经访问ESP32，
     * 如果超过15秒没有请求，主动发起一次 /data 请求，避免ESP32误判离线。
     */
    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        long elapsed = System.currentTimeMillis() - lastFetchDataTime;
        if (elapsed >= HEARTBEAT_INTERVAL_MS) {
            try {
                doGetJson("/data");
                logger.debug("[ESP32] 心跳请求已发送");
            } catch (Exception e) {
                logger.debug("[ESP32] 心跳请求失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public Map<String, Object> fetchData() {
        lastFetchDataTime = System.currentTimeMillis();
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

    @SuppressWarnings("unchecked")
    @Override
    public int fetchCachedData() {
        // 冷却期内不再重复拉取
        if (System.currentTimeMillis() - lastCachedFetchTime < CACHED_FETCH_COOLDOWN_MS) {
            logger.debug("[ESP32] 缓存数据拉取冷却中，跳过");
            return -1;
        }
        if (!fetchingCachedData.compareAndSet(false, true)) {
            logger.info("[ESP32] 缓存数据正在拉取中，跳过重复请求");
            return -1;
        }
        try {
            Map<String, Object> response = doGetJson("/cachedData");
            if (response == null || response.isEmpty() || !response.containsKey("cached")) {
                logger.info("[ESP32] 无缓存数据");
                return 0;
            }

            List<Map<String, Object>> cachedList = (List<Map<String, Object>>) response.get("cached");
            if (cachedList == null || cachedList.isEmpty()) {
                logger.info("[ESP32] 缓存数据列表为空");
                return 0;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int savedCount = 0;

            for (Map<String, Object> item : cachedList) {
                try {
                    EnvDataEntity entity = new EnvDataEntity();
                    entity.setSensorId(1);
                    entity.setTemperature(readDouble(item, "temp"));
                    entity.setHumidity(readDouble(item, "humi"));

                    Double soilAdc = readDouble(item, "soil");
                    if (soilAdc != null) {
                        if (soilAdc < 0)
                            soilAdc = 0.0;
                        if (soilAdc > 4095)
                            soilAdc = 4095.0;
                        entity.setSoilAdc(soilAdc.intValue());
                        entity.setSoilMoisture(Math.max(0, Math.min(100, (4095 - soilAdc) / 40.95)));
                    }

                    Double lightLux = readDouble(item, "lightLux");
                    entity.setLightIntensity(lightLux != null ? Math.max(0, lightLux.intValue()) : null);

                    Double eco2 = readDouble(item, "eco2");
                    entity.setCo2(eco2 != null ? Math.max(0, eco2.intValue()) : null);

                    entity.setSaveUsername("离线保存");

                    // 解析ESP32端的实际采集时间
                    Object ts = item.get("ts");
                    if (ts != null) {
                        entity.setCollectTime(LocalDateTime.parse(ts.toString(), formatter));
                    } else {
                        entity.setCollectTime(LocalDateTime.now());
                    }

                    envDataMapper.insert(entity);
                    savedCount++;
                } catch (Exception e) {
                    logger.warn("[ESP32] 缓存数据记录解析失败: {}", e.getMessage());
                }
            }

            if (savedCount > 0) {
                pendingCachedCount.addAndGet(savedCount);
            }
            lastCachedFetchTime = System.currentTimeMillis();
            logger.info("[ESP32] 成功保存 {} 条缓存数据（共 {} 条）", savedCount, cachedList.size());
            return savedCount;
        } catch (Exception e) {
            logger.error("[ESP32] 获取缓存数据失败", e);
            return -1;
        } finally {
            fetchingCachedData.set(false);
        }
    }

    @Override
    public int consumePendingCachedCount() {
        return pendingCachedCount.getAndSet(0);
    }

    @Override
    public Map<String, Object> getCacheInterval() {
        return doGetJson("/getCacheInterval");
    }

    @Override
    public boolean setCacheInterval(int interval) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/setCacheInterval")
                    .queryParam("interval", interval)
                    .build(true)
                    .toUri();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                logger.info("[ESP32] 缓存间隔设置成功: {}秒", interval);
            } else {
                logger.warn("[ESP32] 缓存间隔设置失败，状态码: {}", response.getStatusCode());
            }
            return success;
        } catch (RestClientException e) {
            logger.error("[ESP32] 网络连接失败，无法设置缓存间隔: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("[ESP32] 缓存间隔设置异常", e);
            return false;
        }
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
