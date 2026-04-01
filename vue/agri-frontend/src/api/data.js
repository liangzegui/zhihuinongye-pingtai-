import request from '@/utils/request'

/**
 * 实时数据接口
 */

// 1. 获取实时环境数据
export const getRealTimeData = () => {
  return request.get('/api/realtime')
}

// 2. 获取历史数据（分页）
export const getHistoricalData = (params = {}) => {
  const queryParams = {
    page: params.page || 1,
    size: params.pageSize || params.size || 10
  }
  // 只有当时间参数有值时才添加到请求参数中
  if (params.startDate) {
    queryParams.startDate = params.startDate
  }
  if (params.endDate) {
    queryParams.endDate = params.endDate
  }
  if (params.sensorType) {
    queryParams.sensorType = params.sensorType
  }
  if (params.sortOrder) {
    queryParams.sortOrder = params.sortOrder
  }
  return request.get('/api/data/historical', { params: queryParams })
}

// 3. 导出历史数据
export const exportHistoricalData = (params = {}) => {
  return request.get('/api/data/export', { 
    params,
    responseType: 'blob'
  })
}

/**
 * 数据分析接口
 */

// 4. 获取温度趋势数据（包含湿度）
export const getTemperatureTrend = (params = {}) => {
  const timeRange = typeof params === 'string' ? params : (params.timeRange || '7day')
  return request.get('/api/analysis/temperature-trend', {
    params: { timeRange }
  })
}

// 5. 获取湿度趋势数据
export const getHumidityTrend = (params = {}) => {
  const timeRange = typeof params === 'string' ? params : (params.timeRange || '7day')
  return request.get('/api/analysis/humidity-trend', {
    params: { timeRange }
  })
}

// 6. 获取土壤水分趋势
export const getSoilTrend = (params = {}) => {
  const timeRange = typeof params === 'string' ? params : (params.timeRange || '7day')
  return request.get('/api/analysis/soil-trend', {
    params: { timeRange }
  })
}

// 7. 获取CO2趋势数据
export const getCO2Trend = (params = {}) => {
  const timeRange = typeof params === 'string' ? params : (params.timeRange || '7day')
  return request.get('/api/analysis/co2-trend', {
    params: { timeRange }
  })
}

// 8. 获取多维度对比数据
export const getMultiDimensionAnalysis = (params = {}) => {
  return request.get('/api/analysis/multi-dimension', { params })
}

/**
 * 数据统计接口
 */

// 9. 获取数据统计摘要
export const getDataSummary = (params = {}) => {
  const timeRange = typeof params === 'string' ? params : (params.timeRange || '7day')
  return request.get('/api/analysis/summary', {
    params: { timeRange }
  })
}

// 10. 获取异常数据
export const getAnomalyData = (params = {}) => {
  return request.get('/api/data/anomaly', { params })
}

// 11. 获取首页仪表盘概览
export const getDashboardOverview = () => {
  return request.get('/api/analysis/dashboard')
}