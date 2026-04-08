/**
 * 设备控制API
 * 通过后端服务HTTP直连控制ESP32设备
 */
import request from '@/utils/request'

/**
 * 统一设备控制接口
 * @param {Object} params - 控制参数
 * @param {boolean} [params.pump] - 水泵状态
 * @param {boolean} [params.fan] - 风扇状态
 * @param {boolean} [params.light] - 照明灯状态
 * @param {boolean} [params.manual] - 手动模式
 * @param {number} [params.fanTempThreshold] - 风扇温度阈值(20-50)
 * @param {number} [params.fanCO2Threshold] - 风扇CO2阈值(400-5000)
 * @param {number} [params.pumpDroughtThreshold] - 水泵干旱阈值(0-5000)
 * @param {number} [params.lightLuxThreshold] - 光照阈值(50-5000)
 */
export function controlDevice(params) {
  return request({
    url: '/api/device/control',
    method: 'post',
    data: params
  })
}

/**
 * 控制水泵
 * @param {boolean} state - true开启，false关闭
 */
export function controlPump(state) {
  return controlDevice({ pump: state })
}

/**
 * 控制风扇
 * @param {boolean} state - true开启，false关闭
 */
export function controlFan(state) {
  return controlDevice({ fan: state })
}

/**
 * 控制照明
 * @param {boolean} state - true开启，false关闭
 */
export function controlLight(state) {
  return controlDevice({ light: state })
}

/**
 * 获取设备状态
 * @returns {Promise} 设备状态信息
 */
export function getDeviceStatus() {
  return request({
    url: '/api/device/status',
    method: 'get'
  })
}

/**
 * 切换手动/自动模式
 * @param {boolean} manual - true手动模式，false自动模式
 */
export function setMode(manual) {
  return controlDevice({ manual })
}

/**
 * 设置阈值
 * @param {string} type - 阈值类型: fanTemp, fanCO2, pumpDrought, lightLux
 * @param {number} value - 阈值数值
 */
export function setThreshold(type, value) {
  const thresholdMap = {
    'fanTemp': 'fanTempThreshold',
    'fanCO2': 'fanCO2Threshold',
    'pumpDrought': 'pumpDroughtThreshold',
    'lightLux': 'lightLuxThreshold'
  }
  const paramKey = thresholdMap[type] || type
  return controlDevice({ [paramKey]: value })
}

/**
 * 批量设置阈值
 * @param {Object} thresholds - 阈值对象
 * @param {number} [thresholds.fanTempThreshold] - 风扇温度阈值
 * @param {number} [thresholds.fanCO2Threshold] - 风扇CO2阈值
 * @param {number} [thresholds.pumpDroughtThreshold] - 水泵干旱阈值
 * @param {number} [thresholds.lightLuxThreshold] - 光照阈值
 */
export function setThresholds(thresholds) {
  return controlDevice(thresholds)
}
