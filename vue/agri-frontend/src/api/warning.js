import request from '@/utils/request'

/**
 * 获取警告日志（支持分页 + 筛选）
 * @param {Object} params - { page, pageSize, warningType, status, timeRange }
 */
export const getWarningLogs = (params = {}) => {
  return request.get('/api/warning/logs', { params })
}

/**
 * 标记警告为已处理
 * @param {Number} id - 警告日志ID
 */
export const markWarningHandled = (id) => {
  return request.put(`/api/warning/logs/${id}/handle`)
}