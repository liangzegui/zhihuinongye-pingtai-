import request from '@/utils/request'

/**
 * 预警规则管理 API
 */

/**
 * 获取所有预警规则
 */
export const getWarningRules = () => {
  return request.get('/api/warning-rules')
}

/**
 * 新增预警规则
 * @param {Object} rule - { sensorType, minValue, maxValue, enabled }
 */
export const createWarningRule = (rule) => {
  return request.post('/api/warning-rules', rule)
}

/**
 * 更新预警规则
 * @param {Number} id - 规则ID
 * @param {Object} rule - { sensorType, minValue, maxValue, enabled }
 */
export const updateWarningRule = (id, rule) => {
  return request.put(`/api/warning-rules/${id}`, rule)
}

/**
 * 删除预警规则
 * @param {Number} id - 规则ID
 */
export const deleteWarningRule = (id) => {
  return request.delete(`/api/warning-rules/${id}`)
}

/**
 * 切换规则启用/禁用
 * @param {Number} id - 规则ID
 */
export const toggleWarningRule = (id) => {
  return request.put(`/api/warning-rules/${id}/toggle`)
}
