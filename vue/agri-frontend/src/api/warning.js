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

/**
 * 批量标记警告为已处理
 * @param {Number[]} ids - 警告日志ID数组
 */
export const batchMarkHandled = (ids) => {
  return request.put('/api/warning/logs/batch-handle', { ids })
}

/**
 * 批量删除警告日志（管理员）
 * @param {Number[]} ids - 警告日志ID数组
 */
export const batchDeleteWarnings = (ids) => {
  return request.delete('/api/warning/logs/batch-delete', { data: { ids } })
}

/**
 * 清空已处理的日志（管理员）
 */
export const clearHandledWarnings = () => {
  return request.delete('/api/warning/logs/clear-handled')
}