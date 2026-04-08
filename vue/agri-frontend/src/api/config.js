// api/config.js - 系统配置API
import request from '@/utils/request';

/**
 * 获取ESP32配置
 */
export const getEsp32Config = () => {
  return request.get('/api/config/esp32');
};

/**
 * 更新ESP32 IP地址
 * @param {string} baseUrl - ESP32的IP地址或URL
 */
export const updateEsp32Config = (baseUrl) => {
  return request.post('/api/config/esp32', { baseUrl });
};

/**
 * 测试ESP32连接
 */
export const testEsp32Connection = () => {
  return request.get('/api/config/esp32/test');
};

// ==================== 数据自动保存配置 ====================

/**
 * 获取数据自动保存配置
 */
export const getAutoSaveConfig = () => {
  return request.get('/api/config/autosave');
};

/**
 * 更新数据自动保存配置
 * @param {Object} config - 配置对象 { enabled: boolean, intervalSeconds: number }
 */
export const updateAutoSaveConfig = (config) => {
  return request.post('/api/config/autosave', config);
};

/**
 * 手动触发保存数据（从ESP32获取）
 */
export const triggerSaveData = () => {
  return request.post('/api/config/autosave/trigger');
};

/**
 * 使用前端传入的数据保存到数据库
 * @param {Object} data - 环境数据 { temperature, humidity, soilAdc, lightIntensity, co2 }
 */
export const saveEnvData = (data) => {
  return request.post('/api/config/autosave/save', data);
};

// ==================== 离线缓存间隔配置 ====================

/**
 * 获取ESP32离线缓存间隔配置及SD卡状态
 */
export const getCacheInterval = () => {
  return request.get('/api/config/cacheInterval');
};

/**
 * 设置ESP32离线缓存间隔
 * @param {number} interval - 缓存间隔秒数(5-3600)
 */
export const setCacheInterval = (interval) => {
  return request.post('/api/config/cacheInterval', { interval });
};
