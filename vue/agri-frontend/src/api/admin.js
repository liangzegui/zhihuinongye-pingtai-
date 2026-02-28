import request from '@/utils/request'

// ==================== 用户管理 ====================

export const getAdminUsers = () => {
  return request.get('/api/admin/users')
}

export const getAdminUserById = (id) => {
  return request.get(`/api/admin/users/${id}`)
}

export const createAdminUser = (data) => {
  return request.post('/api/admin/users', data)
}

export const updateAdminUser = (id, data) => {
  return request.put(`/api/admin/users/${id}`, data)
}

export const deleteAdminUser = (id) => {
  return request.delete(`/api/admin/users/${id}`)
}

// ==================== 统计概览 ====================

export const getAdminStats = () => {
  return request.get('/api/admin/stats')
}

// ==================== 自动保存管理 ====================

export const getAdminAutoSaveConfig = () => {
  return request.get('/api/admin/autosave')
}

export const updateAdminAutoSaveConfig = (config) => {
  return request.post('/api/admin/autosave', config)
}

export const triggerAdminSave = () => {
  return request.post('/api/admin/autosave/trigger')
}

// ==================== 保存数据管理 ====================

export const getAdminEnvData = (params) => {
  return request.get('/api/admin/env-data', { params })
}

export const createAdminEnvData = (data) => {
  return request.post('/api/admin/env-data', data)
}

export const updateAdminEnvData = (id, data) => {
  return request.put(`/api/admin/env-data/${id}`, data)
}

export const deleteAdminEnvData = (id) => {
  return request.delete(`/api/admin/env-data/${id}`)
}

export const batchDeleteAdminEnvData = (ids) => {
  return request.post('/api/admin/env-data/batch-delete', { ids })
}
