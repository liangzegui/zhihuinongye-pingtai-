import request from '@/utils/request'

/**
 * 设备控制历史记录 API
 */

/**
 * 分页查询控制历史
 * @param {Object} params - { page, pageSize, controlType, operator, timeRange }
 */
export const getControlHistory = (params = {}) => {
  return request.get('/api/control-history', { params })
}

/**
 * 获取控制操作统计
 */
export const getControlStats = () => {
  return request.get('/api/control-history/stats')
}
