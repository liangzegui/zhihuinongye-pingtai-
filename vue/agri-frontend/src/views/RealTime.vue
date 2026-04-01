<template>
  <div class="realtime-page">
    <!-- 数据更新指示条（只在手动刷新时显示） -->
    <div v-if="showUpdateIndicator" class="update-indicator pulse">
      <el-icon class="update-icon"><Check /></el-icon>
      <span>数据已更新</span>
    </div>

    <div class="page-header">
      <div class="header-left">
        <h2>🌱 实时环境数据监控</h2>
        <!-- 连接状态指示 -->
        <div class="connection-status">
          <el-icon :class="{ 'status-icon': true, connected: isConnected, disconnected: !isConnected }">
            <CircleCheckFilled v-if="isConnected" />
            <CircleCloseFilled v-else />
          </el-icon>
          <span class="status-text" :class="{ connected: isConnected, disconnected: !isConnected }">
            {{ isConnected ? '已连接' : `连接断开 (${retryCount}/${maxRetries})` }}
          </span>
        </div>
      </div>
      <div class="header-info">
        <span v-if="lastUpdateTime" class="update-time">最后更新: {{ lastUpdateTime }}</span>
      </div>
      <div class="header-actions">
        <el-button type="primary" size="small" @click="manualRefresh">
          <el-icon><Refresh /></el-icon>
          手动刷新
        </el-button>
      </div>
    </div>

    <el-row :gutter="20" v-if="hasData">
      <!-- 左侧：环境数据仪表盘 -->
      <el-col :xs="24" :sm="24" :md="14" :lg="16">
        <div class="dashboard-container">
          <!-- 温度和湿度仪表盘 -->
          <el-row :gutter="15">
            <el-col :xs="24" :sm="12">
              <el-card class="gauge-card" shadow="hover">
                <div class="gauge-header">
                  <span class="gauge-title">
                    <img src="@/assets/thermometer.svg" alt="温度计" class="title-icon" />
                    温度
                  </span>
                  <el-tag :type="getTemperatureStatus(currentData.temperature).type" size="small">
                    {{ getTemperatureStatus(currentData.temperature).text }}
                  </el-tag>
                </div>
                <div class="gauge-chart" ref="tempGauge"></div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-card class="gauge-card" shadow="hover">
                <div class="gauge-header">
                  <span class="gauge-title">
                    <img src="@/assets/humidity.svg" alt="湿度" class="title-icon" />
                    湿度
                  </span>
                  <el-tag :type="getHumidityStatus(currentData.humidity).type" size="small">
                    {{ getHumidityStatus(currentData.humidity).text }}
                  </el-tag>
                </div>
                <div class="gauge-chart" ref="humiGauge"></div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 其他环境参数 -->
          <el-row :gutter="15" style="margin-top: 15px;">
            <el-col :xs="24" :sm="8">
              <el-card class="data-card soil-card" shadow="hover">
                <img src="@/assets/soil.svg" alt="土壤" class="card-icon-img" />
                <div class="card-content">
                  <div class="card-label">土壤ADC</div>
                  <div class="card-value" :class="{ 'is-abnormal': isAbnormal('soilAdc', currentData.soilAdc) }">
                    {{ currentData.soilAdc || 0 }} <span class="unit">ADC</span>
                  </div>
                  <el-tag :type="getSoilStatus(currentData.soilAdc).type" size="small">
                    {{ getSoilStatus(currentData.soilAdc).text }}
                  </el-tag>
                  <div class="data-stats">
                    <div class="stat-item">
                      <span class="stat-label">范围</span>
                      <span class="stat-value">{{ getDataStats('soilAdc').min }}-{{ getDataStats('soilAdc').max }}</span>
                    </div>
                    <div class="stat-item">
                      <span class="stat-label">平均</span>
                      <span class="stat-value">{{ getDataStats('soilAdc').avg }}</span>
                    </div>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-card class="data-card light-card" shadow="hover">
                <img src="@/assets/light.svg" alt="光照" class="card-icon-img" />
                <div class="card-content">
                  <div class="card-label">光照强度</div>
                  <div class="card-value" :class="{ 'is-abnormal': isAbnormal('lightIntensity', currentData.lightIntensity) }">
                    {{ currentData.lightIntensity || 0 }} <span class="unit">lux</span>
                  </div>
                  <el-tag :type="getLightStatus(currentData.lightIntensity).type" size="small">
                    {{ getLightStatus(currentData.lightIntensity).text }}
                  </el-tag>
                  <div class="data-stats">
                    <div class="stat-item">
                      <span class="stat-label">范围</span>
                      <span class="stat-value">{{ getDataStats('lightIntensity').min }}-{{ getDataStats('lightIntensity').max }}</span>
                    </div>
                    <div class="stat-item">
                      <span class="stat-label">平均</span>
                      <span class="stat-value">{{ getDataStats('lightIntensity').avg }}</span>
                    </div>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-card class="data-card co2-card" shadow="hover">
                <img src="@/assets/co2.svg" alt="CO2" class="card-icon-img" />
                <div class="card-content">
                  <div class="card-label">CO₂浓度</div>
                  <div class="card-value" :class="{ 'is-abnormal': isAbnormal('co2', currentData.co2) }">
                    {{ currentData.co2 || 0 }} <span class="unit">ppm</span>
                  </div>
                  <el-tag :type="getCO2Status(currentData.co2).type" size="small">
                    {{ getCO2Status(currentData.co2).text }}
                  </el-tag>
                  <div class="data-stats">
                    <div class="stat-item">
                      <span class="stat-label">范围</span>
                      <span class="stat-value">{{ getDataStats('co2').min }}-{{ getDataStats('co2').max }}</span>
                    </div>
                    <div class="stat-item">
                      <span class="stat-label">平均</span>
                      <span class="stat-value">{{ getDataStats('co2').avg }}</span>
                    </div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <!-- 数据更新时间 -->
          <el-card class="update-info" shadow="never">
            <div class="update-time">
              <el-icon><Clock /></el-icon>
              最后更新时间：{{ lastUpdateTime }}
            </div>
          </el-card>

          <!-- 实时数据趋势图 -->
          <el-row :gutter="15" style="margin-top: 15px;">
            <el-col :xs="24" :sm="12">
              <el-card class="trend-card" shadow="hover">
                <template #header>
                  <div class="trend-header">
                    <span class="trend-title">
                      <img src="@/assets/thermometer.svg" alt="温度" class="trend-icon" />
                      温度趋势
                    </span>
                    <el-tag type="danger" size="small">最近{{ dataHistory.length }}条</el-tag>
                  </div>
                </template>
                <div class="mini-chart" ref="tempTrendChart"></div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-card class="trend-card" shadow="hover">
                <template #header>
                  <div class="trend-header">
                    <span class="trend-title">
                      <img src="@/assets/humidity.svg" alt="湿度" class="trend-icon" />
                      湿度趋势
                    </span>
                    <el-tag type="primary" size="small">最近{{ dataHistory.length }}条</el-tag>
                  </div>
                </template>
                <div class="mini-chart" ref="humiTrendChart"></div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-col>

      <!-- 右侧：设备状态和控制 -->
      <el-col :xs="24" :sm="24" :md="10" :lg="8">
        <DeviceStatus @status-updated="handleStatusUpdate" />
        <DeviceControl
          :external-status="latestDeviceStatus"
          @mode-changed="handleModeChange"
          @device-changed="handleDeviceChange"
          @threshold-changed="handleThresholdChange"
          style="margin-top: 20px;"
        />
      </el-col>
    </el-row>

    <!-- 加载状态 -->
    <div class="loading-state" v-if="loading && !hasData">
      <el-icon class="is-loading" size="40"><Loading /></el-icon>
      <p>正在加载数据...</p>
    </div>

    <!-- 空数据状态 -->
    <div v-if="!loading && !hasData" class="offline-status">
      <el-result 
        icon="error"
        title="连接失败"
        :sub-title="`无法获取数据，请检查网络和服务器状态。重试次数: ${retryCount}/${maxRetries}`"
      >
        <template #extra>
          <div class="button-group">
            <el-button type="primary" @click="fetchRealTimeData">重新加载</el-button>
            <el-button @click="retryFetchData" v-if="retryCount < maxRetries">自动重试</el-button>
            <el-button @click="recoverConnection">标记为已恢复</el-button>
          </div>
        </template>
      </el-result>
    </div>
  </div>
</template>


<script>
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Clock, Refresh, Loading, CircleCheckFilled, CircleCloseFilled, Check } from '@element-plus/icons-vue'
import { getRealTimeData } from '@/api/data'
import DeviceStatus from '@/components/DeviceStatus.vue'
import DeviceControl from '@/components/DeviceControl.vue'
import websocket from '@/utils/websocket'
import wheatIcon from '@/assets/wheat.svg'
import leafyIcon from '@/assets/leafy.svg'

export default {
  name: 'RealTime',
  components: {
    DeviceStatus,
    DeviceControl,
    Clock,
    Refresh,
    Loading,
    CircleCheckFilled,
    CircleCloseFilled,
    Check
  },
  data() {
    return {
      currentData: {
        temperature: 0,
        humidity: 0,
        soilAdc: 0,
        lightIntensity: 0,
        co2: 0
      },
      previousData: {},  // 用于检测数据变化
      hasData: false,
      loading: true,
      countdown: 1,
      lastUpdateTime: '--',
      refreshTimer: null,
      countdownTimer: null,
      // 注意：图表实例不放在 data() 中，避免被 Vue 3 Proxy 代理导致 ECharts 内部崩溃
      isUnmounted: false,
      wsConnected: false,
      useWebSocket: false,  // 默认走HTTP轮询对接ESP32
      
      // 优化参数
      isConnected: true,      // 连接状态
      retryCount: 0,          // 重试次数
      maxRetries: 3,          // 最大重试次数
      refreshInterval: 1000,  // 刷新间隔（毫秒）
      updateInProgress: false, // 防止并发更新
      debounceTimer: null,     // 防抖定时器
      
      // 设备状态（从 DeviceStatus 传递给 DeviceControl，避免重复轮询）
      latestDeviceStatus: null,

      // 数据统计
      dataHistory: [],        // 数据历史记录（保留最后10条）
      showUpdateIndicator: false,  // 是否显示更新提示（仅手动刷新时）
      maxHistory: 10          // 最多保留条数
    }
  },
  created() {
    // ECharts 实例必须作为非响应式属性
    this.tempChart = null
    this.humiChart = null
    this.tempTrendChart = null
    this.humiTrendChart = null
  },
  mounted() {
    this.fetchRealTimeData()
    
    // 尝试连接WebSocket
    if (this.useWebSocket) {
      this.connectWebSocket()
    } else {
      this.startAutoRefresh()
    }
  },
  beforeUnmount() {
    this.isUnmounted = true
    this.stopAutoRefresh()
    
    // 清理防抖定时器
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer)
    }
    
    // 移除WebSocket监听器
    websocket.removeListener('sensorData', this.handleSensorData)
    websocket.removeListener('deviceStatus', this.handleDeviceStatusWs)
    websocket.removeListener('connectionStatus', this.handleConnectionStatus)
    
    if (this.tempChart) {
      this.tempChart.dispose()
      this.tempChart = null
    }
    if (this.humiChart) {
      this.humiChart.dispose()
      this.humiChart = null
    }
    if (this.tempTrendChart) {
      this.tempTrendChart.dispose()
      this.tempTrendChart = null
    }
    if (this.humiTrendChart) {
      this.humiTrendChart.dispose()
      this.humiTrendChart = null
    }
  },
  methods: {
    /**
     * 连接WebSocket并注册监听器
     */
    connectWebSocket() {
      // 注册监听器
      websocket.onSensorData(this.handleSensorData)
      websocket.onDeviceStatus(this.handleDeviceStatusWs)
      websocket.onConnectionStatus(this.handleConnectionStatus)
      
      // 连接
      websocket.connect().then(() => {
        console.log('[RealTime] WebSocket连接成功')
        this.wsConnected = true
        ElMessage.success('实时数据连接成功')
      }).catch(() => {
        console.log('[RealTime] WebSocket连接失败，使用轮询模式')
        this.wsConnected = false
        this.startAutoRefresh()
      })
    },
    
    /**
     * 处理WebSocket推送的传感器数据
     */
    handleSensorData(data) {
      console.log('[RealTime] 收到实时数据:', data)
      
      const newData = {
        temperature: data.temperature || data.temp || this.currentData.temperature,
        humidity: data.humidity || data.humi || this.currentData.humidity,
        soilAdc: data.soilAdc || data.soil || this.currentData.soilAdc,
        lightIntensity: data.lightIntensity || data.lightLux || this.currentData.lightIntensity,
        co2: data.co2 || data.eco2 || this.currentData.co2
      }
      
      // 只在数据有变化时才更新时间
      if (this.hasDataChanged(newData)) {
        this.currentData = newData
        this.hasData = true
        this.lastUpdateTime = new Date().toLocaleString()
        
        // 更新图表
        this.$nextTick(() => {
          this.updateCharts()
        })
      }
    },
    
    /**
     * 处理WebSocket推送的设备状态
     */
    handleDeviceStatusWs(data) {
      console.log('[RealTime] 收到设备状态:', data)
      // 触发子组件更新
      this.$emit('device-status-updated', data)
    },
    
    /**
     * 处理WebSocket连接状态变化
     */
    handleConnectionStatus(connected) {
      this.wsConnected = connected
      if (!connected && !this.isUnmounted) {
        // 连接断开，切换到轮询模式
        console.log('[RealTime] WebSocket断开，切换到轮询模式')
        this.startAutoRefresh()
      }
    },
    
    /**
     * 更新图表（不重新初始化）
     */
    updateCharts() {
      if (this.tempChart) {
        this.tempChart.setOption({
          series: [{
            data: [{ value: this.currentData.temperature }],
            animationDuration: 500  // 增加动画效果
          }]
        })
      }
      if (this.humiChart) {
        this.humiChart.setOption({
          series: [{
            data: [{ value: this.currentData.humidity }],
            animationDuration: 500  // 增加动画效果
          }]
        })
      }
      // 更新趋势图
      this.updateTrendCharts()
    },

    /**
     * 初始化趋势图
     */
    initTrendCharts() {
      // 温度趋势图
      if (this.$refs.tempTrendChart) {
        if (this.tempTrendChart) this.tempTrendChart.dispose()
        this.tempTrendChart = echarts.init(this.$refs.tempTrendChart)
      }
      // 湿度趋势图
      if (this.$refs.humiTrendChart) {
        if (this.humiTrendChart) this.humiTrendChart.dispose()
        this.humiTrendChart = echarts.init(this.$refs.humiTrendChart)
      }
      this.updateTrendCharts()
    },

    /**
     * 更新趋势图
     */
    updateTrendCharts() {
      if (!this.tempTrendChart || !this.humiTrendChart) {
        this.$nextTick(() => {
          this.initTrendCharts()
        })
        return
      }

      const times = this.dataHistory.map((_, i) => `${i + 1}`)
      const temps = this.dataHistory.map(d => d.temperature || 0)
      const humis = this.dataHistory.map(d => d.humidity || 0)

      // 温度趋势图配置
      this.tempTrendChart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}°C',
          backgroundColor: 'rgba(255,255,255,0.95)',
          borderColor: '#e2e8f0',
          textStyle: { color: '#1e293b', fontSize: 12 }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '10px',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: times,
          axisLabel: { fontSize: 10, color: '#94a3b8' },
          axisLine: { lineStyle: { color: '#e2e8f0' } }
        },
        yAxis: {
          type: 'value',
          name: '°C',
          axisLabel: { fontSize: 10, color: '#94a3b8' },
          nameTextStyle: { fontSize: 10, color: '#94a3b8' },
          splitLine: { lineStyle: { color: '#f1f5f9' } }
        },
        series: [{
          name: '温度',
          type: 'line',
          smooth: true,
          data: temps,
          symbol: 'circle',
          symbolSize: 6,
          itemStyle: { color: '#dc2626' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(220,38,38,0.2)' },
                { offset: 1, color: 'rgba(220,38,38,0.02)' }
              ]
            }
          },
          lineStyle: { width: 2 }
        }]
      })

      // 湿度趋势图配置
      this.humiTrendChart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}%',
          backgroundColor: 'rgba(255,255,255,0.95)',
          borderColor: '#e2e8f0',
          textStyle: { color: '#1e293b', fontSize: 12 }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '10px',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: times,
          axisLabel: { fontSize: 10, color: '#94a3b8' },
          axisLine: { lineStyle: { color: '#e2e8f0' } }
        },
        yAxis: {
          type: 'value',
          name: '%',
          axisLabel: { fontSize: 10, color: '#94a3b8' },
          nameTextStyle: { fontSize: 10, color: '#94a3b8' },
          splitLine: { lineStyle: { color: '#f1f5f9' } }
        },
        series: [{
          name: '湿度',
          type: 'line',
          smooth: true,
          data: humis,
          symbol: 'circle',
          symbolSize: 6,
          itemStyle: { color: '#2563eb' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(37,99,235,0.2)' },
                { offset: 1, color: 'rgba(37,99,235,0.02)' }
              ]
            }
          },
          lineStyle: { width: 2 }
        }]
      })
    },

    // 防抖的fetchRealTimeData（避免频繁调用）
    debouncedFetchRealTimeData() {
      if (this.debounceTimer) {
        clearTimeout(this.debounceTimer)
      }
      this.debounceTimer = setTimeout(() => {
        this.fetchRealTimeData()
      }, 300)  // 300ms内的多次调用只执行一次
    },

    /**
     * 手动刷新 - 显示更新提示
     */
    async manualRefresh() {
      await this.fetchRealTimeData()
      // 只有数据获取成功才显示提示
      if (this.hasData && !this.loading) {
        this.showUpdateIndicator = true
        // 3秒后自动隐藏提示
        setTimeout(() => {
          this.showUpdateIndicator = false
        }, 3000)
      }
    },

    async fetchRealTimeData() {
      // 防止并发请求
      if (this.updateInProgress) {
        return
      }
      
      this.updateInProgress = true
      this.loading = true
      try {
        const res = await getRealTimeData()
        
        // 处理多种响应格式
        let data = null
        if (res && res.code === 200 && res.data) {
          data = Array.isArray(res.data) ? res.data[0] : res.data
        } else if (res && Array.isArray(res)) {
          data = res[0]
        } else if (res && res.data) {
          data = Array.isArray(res.data) ? res.data[0] : res.data
        }

        if (data && typeof data === 'object') {
          // 新数据
          const newData = {
            temperature: this.validateNumber(data.temperature, 0, -50, 80),
            humidity: this.validateNumber(data.humidity, 0, 0, 100),
            soilAdc: this.validateNumber(data.soilAdc || data.soilMoisture, 0, 0, 4095),
            lightIntensity: this.validateNumber(data.lightIntensity || data.light_intensity, 0, 0, 100000),
            co2: this.validateNumber(data.co2, 0, 0, 5000)
          }
          
          // 只在数据有变化时更新（避免不必要的图表重绘）
          if (this.hasDataChanged(newData)) {
            this.currentData = newData
            this.hasData = true
            this.lastUpdateTime = new Date().toLocaleString()
            this.retryCount = 0  // 重置重试计数
            this.isConnected = true
            
            // 记录数据历史（用于统计）
            this.dataHistory.push({ ...newData, timestamp: Date.now() })
            if (this.dataHistory.length > this.maxHistory) {
              this.dataHistory.shift()  // 移除最早的记录
            }
            
            // 更新图表：已有实例则仅更新数值，避免指针从0重新转动
            this.$nextTick(() => {
              if (this.tempChart && this.humiChart) {
                this.updateCharts()
              } else {
                this.initCharts()
              }
            })
          }
        } else {
          console.warn('[RealTime] 响应数据格式不正确:', res)
          this.hasData = true // 即使没有数据也显示页面
          ElMessage.warning('未获取到有效数据')
        }
      } catch (error) {
        console.error('[RealTime] 获取实时数据失败:', error)
        this.isConnected = false
        this.retryCount++
        
        // 错误分类处理
        let errorMsg = '网络错误'
        if (error.message === 'Network Error') {
          errorMsg = '网络连接失败'
        } else if (error.code === 'ECONNABORTED') {
          errorMsg = '连接超时'
        } else if (error.response?.status === 503) {
          errorMsg = '服务器暂时不可用，请稍后'
        } else {
          errorMsg = error.response?.data?.message || error.message || errorMsg
        }
        
        // 重试次数未达上限时，不显示错误提示
        if (this.retryCount <= this.maxRetries) {
          console.log(`[RealTime] 第${this.retryCount}次重试...`)
        } else {
          ElMessage.error('数据加载失败：' + errorMsg)
          this.hasData = false
        }
      } finally {
        this.loading = false
        this.updateInProgress = false
        this.countdown = 5
      }
    },

    // 检测数据是否有变化（避免不必要的更新）
    hasDataChanged(newData) {
      if (!this.previousData || Object.keys(this.previousData).length === 0) {
        this.previousData = { ...newData }
        return true
      }
      
      // 检查是否有任何字段发生变化
      const changed = Object.keys(newData).some(key => {
        const oldVal = this.previousData[key]
        const newVal = newData[key]
        // 数值变化超过0.5才认为是真正的变化（减少浮点数比较的误差）
        return Math.abs(oldVal - newVal) > 0.5
      })
      
      if (changed) {
        this.previousData = { ...newData }
      }
      
      return changed
    },

    // 数据验证方法
    validateNumber(value, defaultValue = 0, min = null, max = null) {
      if (value === null || value === undefined || isNaN(value)) {
        return defaultValue
      }
      const num = Number(value)
      if (min !== null && num < min) return min
      if (max !== null && num > max) return max
      return num
    },

    startAutoRefresh() {
      // 每秒刷新一次数据
      this.refreshTimer = setInterval(() => {
        // 如果有待处理的请求，跳过本次刷新
        if (this.updateInProgress) {
          return
        }
        
        // 如果连接断开且已超过重试次数，降速刷新以减少服务器压力
        if (!this.isConnected && this.retryCount >= this.maxRetries) {
          // 每30秒尝试一次恢复连接
          if (Math.random() * 6 > 1) {
            return
          }
        }
        
        this.fetchRealTimeData()
      }, 1000)

      // 倒计时：每秒更新一次
      this.countdownTimer = setInterval(() => {
        if (this.countdown > 0) {
          this.countdown--
        } else {
          this.countdown = 1
        }
      }, 1000)
    },

    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer)
      }
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
      }
    },

    initCharts() {
      // 如果组件已卸载，不初始化图表
      if (this.isUnmounted) return
      
      // 延迟初始化，确保 DOM 已准备好
      setTimeout(() => {
        if (this.isUnmounted) return
        this.initTemperatureGauge()
        this.initHumidityGauge()
      }, 100)
    },

    initTemperatureGauge() {
      if (this.isUnmounted) return
      const el = this.$refs.tempGauge
      if (!el) return

      if (!this.tempChart) {
        try {
          this.tempChart = echarts.init(el)
        } catch (e) {
          console.warn('温度图表初始化失败:', e)
          return
        }
      }

      const value = this.currentData.temperature || 0

      const option = {
        graphic: [{
          type: 'image',
          style: {
            image: wheatIcon,
            width: 36,
            height: 36,
            opacity: 0.85
          },
          left: 'center',
          top: '38%'
        }],
        series: [{
          type: 'gauge',
          min: 0,
          max: 50,
          startAngle: 225,
          endAngle: -45,
          radius: '90%',
          center: ['50%', '55%'],
          progress: {
            show: true,
            width: 14,
            roundCap: true,
            itemStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 1, y2: 0,
                colorStops: [
                  { offset: 0, color: '#0ea5e9' },
                  { offset: 0.4, color: '#22c55e' },
                  { offset: 0.7, color: '#eab308' },
                  { offset: 1, color: '#ef4444' }
                ]
              },
              shadowColor: 'rgba(234, 179, 8, 0.4)',
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowOffsetY: 0
            }
          },
          axisLine: {
            lineStyle: {
              width: 14,
              color: [[1, 'rgba(71, 85, 99, 0.15)']]
            },
            roundCap: true
          },
          axisTick: { show: false },
          splitLine: { show: false },
          axisLabel: { show: false },
          pointer: {
            icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z',
            length: '55%',
            width: 10,
            offsetCenter: [0, '-10%'],
            itemStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: '#dc2626' },
                  { offset: 1, color: '#f97316' }
                ]
              },
              shadowColor: 'rgba(249, 115, 22, 0.6)',
              shadowBlur: 8,
              shadowOffsetY: 2
            }
          },
          anchor: {
            show: true,
            showAbove: true,
            size: 16,
            itemStyle: {
              color: '#ea580c',
              shadowColor: 'rgba(234, 88, 12, 0.4)',
              shadowBlur: 6
            }
          },
          title: { show: false },
          detail: {
            valueAnimation: true,
            fontSize: 26,
            fontWeight: '700',
            fontFamily: 'Inter, system-ui, sans-serif',
            color: '#1e293b',
            offsetCenter: [0, '85%'],
            formatter: function(val) {
              return val.toFixed(1) + '°C'
            }
          },
          data: [{ value: value }],
          animationDuration: 800,
          animationEasing: 'cubicOut'
        }]
      }

      this.tempChart.setOption(option)
    },

    initHumidityGauge() {
      if (this.isUnmounted) return
      const el = this.$refs.humiGauge
      if (!el) return

      if (!this.humiChart) {
        try {
          this.humiChart = echarts.init(el)
        } catch (e) {
          console.warn('湿度图表初始化失败:', e)
          return
        }
      }

      const value = this.currentData.humidity || 0

      const option = {
        graphic: [{
          type: 'image',
          style: {
            image: leafyIcon,
            width: 36,
            height: 36,
            opacity: 0.85
          },
          left: 'center',
          top: '38%'
        }],
        series: [{
          type: 'gauge',
          min: 0,
          max: 100,
          startAngle: 225,
          endAngle: -45,
          radius: '90%',
          center: ['50%', '55%'],
          progress: {
            show: true,
            width: 14,
            roundCap: true,
            itemStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 1, y2: 0,
                colorStops: [
                  { offset: 0, color: '#0f766e' },
                  { offset: 0.5, color: '#14b8a6' },
                  { offset: 1, color: '#5eead4' }
                ]
              },
              shadowColor: 'rgba(20, 184, 166, 0.5)',
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowOffsetY: 0
            }
          },
          axisLine: {
            lineStyle: {
              width: 14,
              color: [[1, 'rgba(71, 85, 99, 0.15)']]
            },
            roundCap: true
          },
          axisTick: { show: false },
          splitLine: { show: false },
          axisLabel: { show: false },
          pointer: {
            icon: 'path://M12.8,0.7l12,40.1H0.7L12.8,0.7z',
            length: '55%',
            width: 10,
            offsetCenter: [0, '-10%'],
            itemStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: '#0f766e' },
                  { offset: 1, color: '#14b8a6' }
                ]
              },
              shadowColor: 'rgba(15, 118, 110, 0.6)',
              shadowBlur: 8,
              shadowOffsetY: 2
            }
          },
          anchor: {
            show: true,
            showAbove: true,
            size: 16,
            itemStyle: {
              color: '#0f766e',
              shadowColor: 'rgba(15, 118, 110, 0.4)',
              shadowBlur: 6
            }
          },
          title: { show: false },
          detail: {
            valueAnimation: true,
            fontSize: 26,
            fontWeight: '700',
            fontFamily: 'Inter, system-ui, sans-serif',
            color: '#1e293b',
            offsetCenter: [0, '85%'],
            formatter: function(val) {
              return val.toFixed(1) + '%'
            }
          },
          data: [{ value: value }],
          animationDuration: 800,
          animationEasing: 'cubicOut'
        }]
      }

      this.humiChart.setOption(option)
    },

    getTemperatureStatus(temp) {
      if (temp < 15) return { type: 'info', text: '偏低' }
      if (temp < 25) return { type: 'success', text: '正常' }
      if (temp < 35) return { type: 'warning', text: '偏高' }
      return { type: 'danger', text: '过高' }
    },

    getHumidityStatus(humi) {
      if (humi < 40) return { type: 'warning', text: '偏低' }
      if (humi < 70) return { type: 'success', text: '正常' }
      return { type: 'info', text: '偏高' }
    },

    getSoilStatus(value) {
      // ADC值越大越干燥
      if (value < 2200) return { type: 'primary', text: '湿润' }
      if (value < 2800) return { type: 'success', text: '正常' }
      if (value < 3200) return { type: 'warning', text: '轻旱' }
      if (value < 3500) return { type: 'warning', text: '中旱' }
      return { type: 'danger', text: '重旱' }
    },

    getLightStatus(value) {
      if (value < 800) return { type: 'info', text: '偏暗' }
      if (value < 1000) return { type: 'warning', text: '较暗' }
      if (value < 3000) return { type: 'success', text: '正常' }
      return { type: 'primary', text: '明亮' }
    },

    getCO2Status(co2) {
      if (co2 < 400) return { type: 'success', text: '优秀' }
      if (co2 < 800) return { type: 'success', text: '良好' }
      if (co2 < 1000) return { type: 'warning', text: '一般' }
      return { type: 'danger', text: '偏高' }
    },

    handleStatusUpdate(status) {
      console.log('设备状态更新:', status)
      // 将 DeviceStatus 获取的数据传给 DeviceControl，避免重复轮询
      this.latestDeviceStatus = status
    },

    handleModeChange(manual) {
      ElMessage.success(`已切换到${manual ? '手动' : '自动'}模式`)
    },

    handleDeviceChange({ device, state }) {
      ElMessage.success(`${device}已${state ? '开启' : '关闭'}`)
    },

    handleThresholdChange(thresholds) {
      console.log('阈值已更新:', thresholds)
    },

    // 重试获取数据
    retryFetchData() {
      this.retryCount++
      if (this.retryCount > this.maxRetries) {
        ElMessage.error(`连接失败，已超过最大重试次数 (${this.maxRetries})，请检查网络或服务器状态`)
        return
      }
      
      const backoffTime = Math.pow(2, this.retryCount - 1) * 1000
      ElMessage.info(`将在 ${backoffTime / 1000} 秒后进行第 ${this.retryCount} 次重试...`)
      
      setTimeout(() => {
        this.fetchRealTimeData()
      }, backoffTime)
    },

    // 恢复连接
    recoverConnection() {
      if (!this.isConnected) {
        this.retryCount = 0
        this.isConnected = true
        ElMessage.success('连接已恢复，自动重新加载数据...')
        this.fetchRealTimeData()
      }
    },

    // 计算数据统计
    getDataStats(field) {
      if (this.dataHistory.length === 0) return { min: '--', max: '--', avg: '--' }
      
      const values = this.dataHistory.map(d => d[field]).filter(v => typeof v === 'number')
      if (values.length === 0) return { min: '--', max: '--', avg: '--' }
      
      const min = Math.min(...values)
      const max = Math.max(...values)
      const avg = (values.reduce((a, b) => a + b, 0) / values.length).toFixed(2)
      
      return { 
        min: min.toFixed(2), 
        max: max.toFixed(2), 
        avg: avg 
      }
    },

    // 判断是否异常
    isAbnormal(field, value) {
      const stats = this.getDataStats(field)
      if (stats.min === '--') return false
      
      const min = parseFloat(stats.min)
      const max = parseFloat(stats.max)
      const range = max - min
      const threshold = range * 0.5  // 超出范围50%认为异常
      
      return value < min - threshold || value > max + threshold
    }
  }
}
</script>


<style scoped>
/* ========== Design Tokens ========== */
/* Primary: #1a472a (deep forest)  Accent: #3a7d44 (forest green)
   Secondary: #0f766e (teal)       Surface: #f0fdf4 (mint)
   Neutral: #475569 (slate)        Border: rgba(71, 85, 99, 0.12)
   Text: #1e293b / #475569 / #94a3b8 */

.realtime-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0fdfa 100%);
  position: relative;
}

.realtime-page::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 40%;
  height: 40%;
  background: radial-gradient(ellipse at top right, rgba(58, 125, 68, 0.08) 0%, transparent 70%);
  pointer-events: none;
}

/* ========== Update Indicator ========== */
.update-indicator {
  padding: 12px 18px;
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.95), rgba(220, 252, 231, 0.95));
  border: 1px solid rgba(58, 125, 68, 0.2);
  border-radius: 12px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #166534;
  font-size: 13px;
  font-weight: 500;
  animation: slideDown 0.3s ease-out;
  box-shadow: 0 4px 12px rgba(26, 71, 42, 0.08);
}

.update-icon {
  color: #16a34a;
  font-size: 16px;
}

.update-indicator.pulse .update-icon {
  animation: pulse-fade 2s infinite;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse-fade {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ========== Page Header ========== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 18px 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  box-shadow: 0 4px 20px rgba(26, 71, 42, 0.06);
  position: relative;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1a472a;
  letter-spacing: -0.02em;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.8), rgba(236, 253, 245, 0.8));
  border-radius: 20px;
  font-size: 12px;
  border: 1px solid rgba(58, 125, 68, 0.15);
}

.status-icon {
  font-size: 14px;
  transition: color 0.3s;
}

.status-icon.connected { color: #16a34a; }
.status-icon.disconnected { color: #dc2626; }

.status-text { font-weight: 500; font-size: 12px; }
.status-text.connected { color: #16a34a; }
.status-text.disconnected { color: #dc2626; }

.header-info {
  display: flex;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

/* ========== Dashboard ========== */
.dashboard-container {
  width: 100%;
}

/* ========== Gauge Cards ========== */
.gauge-card {
  height: 280px;
  margin-bottom: 16px;
  border-radius: 16px;
  border: 1px solid rgba(71, 85, 99, 0.12);
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  position: relative;
  overflow: hidden;
}

.gauge-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(58, 125, 68, 0.05),
    transparent
  );
  animation: shimmer 3s infinite;
}

@keyframes shimmer {
  0% { left: -100%; }
  100% { left: 100%; }
}

.gauge-card :deep(.el-card__body) {
  padding: 16px 16px 8px;
  position: relative;
  z-index: 1;
}

.gauge-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(71, 85, 99, 0.08);
}

.gauge-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a472a;
  display: flex;
  align-items: center;
  gap: 8px;
  letter-spacing: -0.01em;
}

.title-icon {
  width: 22px;
  height: 30px;
  object-fit: contain;
  filter: drop-shadow(0 2px 4px rgba(26, 71, 42, 0.15));
}

.gauge-chart {
  width: 100%;
  height: 210px;
  position: relative;
}

.gauge-chart::after {
  content: '';
  position: absolute;
  bottom: 10%;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 4px;
  background: linear-gradient(90deg, transparent, rgba(58, 125, 68, 0.1), transparent);
  border-radius: 2px;
}

/* ========== Data Cards (Soil / Light / CO2) ========== */
.data-card {
  min-height: 140px;
  border-radius: 14px;
  margin-bottom: 16px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.data-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--card-accent, #3a7d44), transparent);
  opacity: 0;
  transition: opacity 0.3s;
}

.data-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(26, 71, 42, 0.12);
}

.data-card:hover::before {
  opacity: 1;
}

.soil-card {
  --card-accent: #3a7d44;
  border-left: 4px solid #3a7d44;
}

.light-card {
  --card-accent: #d97706;
  border-left: 4px solid #d97706;
}

.co2-card {
  --card-accent: #0f766e;
  border-left: 4px solid #0f766e;
}

.card-icon-img {
  width: 36px;
  height: 36px;
  object-fit: contain;
  flex-shrink: 0;
  opacity: 0.85;
  align-self: center;
}

.card-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.card-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  margin-bottom: 4px;
}

.card-value {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 6px;
  line-height: 1.2;
  transition: color 0.3s ease;
}

.card-value.is-abnormal {
  color: #dc2626;
  animation: abnormal-pulse 1.5s infinite;
}

@keyframes abnormal-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.65; }
}

.unit {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 400;
}

/* ========== Data Stats ========== */
.data-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px 4px;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #f1f5f9;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 10px;
  line-height: 1.4;
}

.stat-label {
  color: #94a3b8;
  font-weight: 500;
  font-size: 10px;
}

.stat-value {
  color: #475569;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  font-size: 10px;
  max-width: 70px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ========== Update Info Bar ========== */
.update-info {
  margin-top: 16px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(71, 85, 99, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(8px);
}

.update-info :deep(.el-card__body) {
  padding: 10px 16px;
}

.update-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
}

/* ========== Trend Charts ========== */
.trend-card {
  border-radius: 14px;
  margin-bottom: 16px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
}

.trend-card :deep(.el-card__header) {
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.trend-card :deep(.el-card__body) {
  padding: 12px 8px 8px;
}

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.trend-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a472a;
  display: flex;
  align-items: center;
  gap: 6px;
}

.trend-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.mini-chart {
  width: 100%;
  height: 150px;
}

/* ========== Loading & Offline States ========== */
.loading-state {
  text-align: center;
  padding: 80px 0;
  color: #1a472a;
}

.loading-state p {
  margin-top: 12px;
  font-size: 14px;
  color: #475569;
}

.offline-status {
  padding: 48px 24px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  margin-top: 20px;
  text-align: center;
  backdrop-filter: blur(8px);
}

.button-group {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .realtime-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 16px;
  }

  .page-header h2 {
    font-size: 16px;
  }

  .header-info {
    width: 100%;
    justify-content: space-between;
  }

  .gauge-card {
    height: 240px;
  }

  .data-card {
    min-height: 130px;
  }

  .card-value {
    font-size: 18px;
  }
}

/* ========== Deep Overrides ========== */
:deep(.el-card__body) {
  display: flex;
  flex-direction: column;
}

.data-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  min-height: 100%;
  box-sizing: border-box;
}

:deep(.el-tag) {
  border: none;
  font-size: 11px;
  font-weight: 500;
}

:deep(.el-card) {
  box-shadow: none;
}

:deep(.el-card.is-hover-shadow:hover) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
</style>
