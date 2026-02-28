import request from '@/utils/request'

/**
 * 获取所有异常配置（按分组）
 */
export const getExceptionConfig = () => {
  return request.get('/api/exception-config')
}

/**
 * 获取指定分组的配置
 * @param {string} group - detection / notification / handling / severity
 */
export const getExceptionConfigByGroup = (group) => {
  return request.get(`/api/exception-config/group/${group}`)
}

/**
 * 批量更新异常配置
 * @param {Object} configs - { config_key: config_value, ... }
 */
export const updateExceptionConfig = (configs) => {
  return request.put('/api/exception-config', configs)
}

/**
 * 更新单个配置项
 * @param {string} key - 配置键
 * @param {string} value - 配置值
 */
export const updateSingleConfig = (key, value) => {
  return request.put(`/api/exception-config/${key}`, { value })
}

/**
 * 重置所有配置为默认值
 */
export const resetExceptionConfig = () => {
  return request.post('/api/exception-config/reset')
}
