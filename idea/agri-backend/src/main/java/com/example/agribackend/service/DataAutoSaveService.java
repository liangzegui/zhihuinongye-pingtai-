package com.example.agribackend.service;

import com.example.agribackend.dto.EnvDataDTO;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.mapper.EnvDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据自动保存服务 - 定期从实时数据获取并保存到数据库
 */
@Service
public class DataAutoSaveService {
    private static final Logger logger = LoggerFactory.getLogger(DataAutoSaveService.class);
    // IoTDA数据新鲜度窗口：5分钟
    private static final long IOTDA_FRESH_WINDOW_MILLIS = 5 * 60 * 1000L;

    @Autowired
    private RealTimeService realTimeService;

    @Autowired
    private EnvDataMapper envDataMapper;

    @Autowired
    private Esp32BridgeService esp32BridgeService;

    @Autowired
    private IoTDAService iotDAService;

    // 是否启用自动保存（默认关闭，需要用户在管理员中心手动开启）
    private AtomicBoolean enabled = new AtomicBoolean(false);

    // 保存间隔（秒），默认60秒
    private AtomicLong saveIntervalSeconds = new AtomicLong(60);

    // 上次保存时间
    private volatile long lastSaveTime = 0;

    // 最后配置自动保存的用户名（定时保存时使用此用户名作为保存人，由实际登录用户设置）
    private volatile String configuredBy = "";

    // 默认传感器ID
    private static final int DEFAULT_SENSOR_ID = 1;

    /**
     * 定时任务 - 每秒检查是否需要保存数据
     */
    @Scheduled(fixedRate = 1000)
    public void checkAndSaveData() {
        if (!enabled.get()) {
            return;
        }

        // 必须有配置人（即有用户通过管理员中心开启过自动保存）才执行
        if (configuredBy == null || configuredBy.trim().isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long intervalMillis = saveIntervalSeconds.get() * 1000;

        if (currentTime - lastSaveTime >= intervalMillis) {
            String autoSaveUser = configuredBy + "(自动保存)";
            boolean success = saveDataFromRealTime(autoSaveUser);
            if (success) {
                lastSaveTime = currentTime;
            }
        }
    }

    /**
     * 从实时数据获取并保存到数据库
     */
    public boolean saveDataFromRealTime() {
        return saveDataFromRealTime("手动保存");
    }

    /**
     * 从实时数据获取并保存到数据库（可指定保存人）
     * 优先从ESP32直连获取实时数据，兜底从IoTDA缓存读取
     */
    private boolean saveDataFromRealTime(String saveUsername) {
        try {
            // 设备离线时禁止自动保存
            if (!isDeviceOnline()) {
                logger.warn("[数据保存] 设备离线，跳过自动保存");
                return false;
            }

            // 优先通道1：直接从ESP32获取实时数据（与"立即保存"使用相同数据源）
            Map<String, Object> esp32Data = null;
            try {
                esp32Data = esp32BridgeService.fetchData();
            } catch (Exception e) {
                logger.warn("[数据保存] ESP32直连获取数据失败: {}", e.getMessage());
            }

            EnvDataEntity entity = new EnvDataEntity();
            entity.setSensorId(DEFAULT_SENSOR_ID);

            if (esp32Data != null && !esp32Data.isEmpty()) {
                // ESP32原始字段名: dhtTemp, dhtHumi, soilAO, lightIntensity, eco2
                entity.setTemperature(getDoubleFromKeys(esp32Data, "dhtTemp", "temperature", "temp"));
                entity.setHumidity(getDoubleFromKeys(esp32Data, "dhtHumi", "humidity", "humi"));

                Double soilAdc = getDoubleFromKeys(esp32Data, "soilAO", "soil", "soilAdc", "soilMoisture");
                if (soilAdc != null) {
                    if (soilAdc < 0) soilAdc = 0.0;
                    if (soilAdc > 4095) soilAdc = 4095.0;
                    entity.setSoilAdc(soilAdc.intValue());
                    entity.setSoilMoisture(Math.max(0, Math.min(100, (4095 - soilAdc) / 40.95)));
                }

                Integer light = getIntegerFromKeys(esp32Data, "lightIntensity", "lightLux");
                entity.setLightIntensity(light != null ? Math.max(0, light) : null);

                Integer co2 = getIntegerFromKeys(esp32Data, "eco2", "co2");
                entity.setCo2(co2 != null ? Math.max(0, co2) : null);

                logger.info("[数据保存] 使用ESP32直连数据");
            } else {
                // 兜底通道2：从IoTDA缓存获取（缓存key已标准化）
                Map<String, Object> cachedData = iotDAService.getCachedSensorData();
                if (cachedData == null || cachedData.isEmpty()) {
                    logger.warn("[数据保存] ESP32和IoTDA均无有效数据，跳过保存");
                    return false;
                }

                entity.setTemperature(getDoubleValue(cachedData, "temperature"));
                entity.setHumidity(getDoubleValue(cachedData, "humidity"));
                entity.setSoilMoisture(getDoubleValue(cachedData, "soilMoisture"));
                entity.setSoilAdc(getIntegerValue(cachedData, "soilAdc"));
                entity.setLightIntensity(getIntegerValue(cachedData, "lightIntensity"));
                entity.setCo2(getIntegerValue(cachedData, "co2"));

                logger.info("[数据保存] 使用IoTDA缓存数据");
            }

            entity.setSaveUsername(saveUsername);
            entity.setCollectTime(LocalDateTime.now());

            envDataMapper.insert(entity);
            logger.info("[数据保存] 成功保存数据: 温度={}, 湿度={}, 土壤={}%, 土壤ADC={}, 光照={}, CO2={}",
                    entity.getTemperature(), entity.getHumidity(),
                    entity.getSoilMoisture(), entity.getSoilAdc(), entity.getLightIntensity(), entity.getCo2());
            return true;
        } catch (Exception e) {
            logger.error("[数据保存] 保存失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 手动触发保存
     */
    public boolean manualSave() {
        return saveDataFromRealTime("手动保存");
    }

    /**
     * 手动触发保存（记录触发用户名）
     */
    public boolean manualSave(String saveUsername) {
        return saveDataFromRealTime(normalizeSaveUsername(saveUsername, "手动保存"));
    }

    /**
     * 使用传入的数据保存到数据库
     */
    public boolean saveWithData(Map<String, Object> data) {
        return saveWithData(data, "手动保存");
    }

    /**
     * 使用传入的数据保存到数据库（记录保存人）
     */
    public boolean saveWithData(Map<String, Object> data, String saveUsername) {
        try {
            // 设备离线时禁止手动保存
            if (!isDeviceOnline()) {
                logger.warn("[数据保存] 设备离线，禁止手动保存");
                return false;
            }

            if (data == null || data.isEmpty()) {
                logger.warn("[数据保存] 传入数据为空，跳过保存");
                return false;
            }

            EnvDataEntity entity = new EnvDataEntity();
            entity.setSensorId(DEFAULT_SENSOR_ID);
            entity.setTemperature(parseDouble(data.get("temperature")));
            entity.setHumidity(parseDouble(data.get("humidity")));
            // soilMoisture 是百分比值(0-100)，soilAdc 是原始ADC值(0-4095)
            Double soilValue = parseDouble(data.get("soilMoisture"));
            Integer soilAdcValue = parseInteger(data.get("soilAdc"));
            if (soilValue == null && soilAdcValue != null) {
                // 如果没有soilMoisture，尝试从soilAdc转换
                soilValue = Math.max(0, Math.min(100, (4095 - soilAdcValue) / 40.95));
            }
            entity.setSoilMoisture(soilValue != null ? soilValue : 0.0);
            entity.setSoilAdc(soilAdcValue);  // 保存土壤ADC原始值
            entity.setLightIntensity(parseInteger(data.get("lightIntensity")));
            entity.setCo2(parseInteger(data.get("co2")));
            entity.setSaveUsername(normalizeSaveUsername(saveUsername, "手动保存"));
            entity.setCollectTime(LocalDateTime.now());

            envDataMapper.insert(entity);
            lastSaveTime = System.currentTimeMillis();
            logger.info("[数据保存] 成功保存传入数据: 温度={}, 湿度={}, 土壤={}%, 土壤ADC={}, 光照={}, CO2={}",
                    entity.getTemperature(), entity.getHumidity(),
                    entity.getSoilMoisture(), entity.getSoilAdc(), entity.getLightIntensity(), entity.getCo2());
            return true;
        } catch (Exception e) {
            logger.error("[数据保存] 保存传入数据失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取是否启用自动保存
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * 设置是否启用自动保存
     */
    public void setEnabled(boolean enable) {
        this.enabled.set(enable);
        logger.info("[数据保存] 自动保存已{}", enable ? "启用" : "禁用");
    }

    /**
     * 设置是否启用自动保存，并记录配置人
     */
    public void setEnabled(boolean enable, String username) {
        this.enabled.set(enable);
        if (username != null && !username.trim().isEmpty() && !"null".equalsIgnoreCase(username.trim())) {
            this.configuredBy = username.trim();
        }
        logger.info("[数据保存] 自动保存已{}，配置人: {}", enable ? "启用" : "禁用", this.configuredBy);
    }

    /**
     * 设置自动保存配置人（不改变启用状态）
     */
    public void setConfiguredBy(String username) {
        if (username != null && !username.trim().isEmpty() && !"null".equalsIgnoreCase(username.trim())) {
            this.configuredBy = username.trim();
            logger.info("[数据保存] 自动保存配置人已更新为: {}", this.configuredBy);
        }
    }

    /**
     * 获取当前自动保存配置人
     */
    public String getConfiguredBy() {
        return configuredBy;
    }

    /**
     * 获取保存间隔（秒）
     */
    public long getSaveIntervalSeconds() {
        return saveIntervalSeconds.get();
    }

    /**
     * 设置保存间隔（秒）
     */
    public void setSaveIntervalSeconds(long seconds) {
        if (seconds < 5) {
            seconds = 5; // 最小5秒
        }
        if (seconds > 3600) {
            seconds = 3600; // 最大1小时
        }
        this.saveIntervalSeconds.set(seconds);
        logger.info("[数据保存] 保存间隔已设置为 {} 秒", seconds);
    }

    /**
     * 获取上次保存时间
     */
    public long getLastSaveTime() {
        return lastSaveTime;
    }

    private Double parseDouble(Object value) {
        if (value == null)
            return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null)
            return 0;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 从Map中安全获取Double值
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从Map中安全获取Integer值
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeSaveUsername(String saveUsername, String defaultValue) {
        if (saveUsername == null || saveUsername.trim().isEmpty() || "null".equalsIgnoreCase(saveUsername.trim())) {
            return defaultValue;
        }
        return saveUsername.trim();
    }

    /**
     * 从Map中尝试多个key获取Double值（兼容ESP32不同字段名）
     */
    private Double getDoubleFromKeys(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Double val = getDoubleValue(map, key);
            if (val != null) return val;
        }
        return null;
    }

    /**
     * 从Map中尝试多个key获取Integer值（兼容ESP32不同字段名）
     */
    private Integer getIntegerFromKeys(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Integer val = getIntegerValue(map, key);
            if (val != null) return val;
        }
        return null;
    }

    /**
     * 检查设备是否在线
     */
    private boolean isDeviceOnline() {
        try {
            // 通道1：本地ESP32直连
            if (esp32BridgeService.testConnection()) {
                return true;
            }

            // 通道2：IoTDA在线且数据为近期上报
            Map<String, Object> deviceStatus = iotDAService.getDeviceStatus();
            boolean iotOnline = parseBoolean(deviceStatus != null ? deviceStatus.get("online") : null);
            long lastUpdate = parseLong(deviceStatus != null ? deviceStatus.get("lastUpdate") : null, 0L);
            boolean isFresh = lastUpdate > 0 && (System.currentTimeMillis() - lastUpdate) <= IOTDA_FRESH_WINDOW_MILLIS;

            if (iotOnline && isFresh) {
                return true;
            }

            logger.warn("[数据保存] 在线检查未通过：本地离线，IoTDA在线={}, 数据新鲜={}", iotOnline, isFresh);
            return false;
        } catch (Exception e) {
            logger.warn("[数据保存] 设备在线状态检查失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean parseBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private long parseLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
