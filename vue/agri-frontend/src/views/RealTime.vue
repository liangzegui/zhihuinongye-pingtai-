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
                <div class="gauge-value">
                  {{ currentData.temperature || 0 }} °C
                </div>
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
                <div class="gauge-value">{{ currentData.humidity || 0 }} %</div>
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
                  <el-progress 
                    :percentage="Math.min(100, (currentData.soilAdc || 0) / 40.95)" 
                    :color="getSoilColor(currentData.soilAdc)"
                    :show-text="false"
                  />
                  <!-- 数据统计 -->
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
                  <div class="light-bar">
                    <div class="light-fill" :style="{ width: getLightPercentage(currentData.lightIntensity) + '%' }"></div>
                  </div>
                  <!-- 数据统计 -->
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
                  <!-- 数据统计 -->
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

          <!-- 实时数据趋势图 - 拆分为两个图表 -->
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
      tempChart: null,
      humiChart: null,
      tempTrendChart: null,   // 温度趋势图
      humiTrendChart: null,   // 湿度趋势图
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
          formatter: '{b}: {c}°C'
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
          axisLabel: { fontSize: 10 }
        },
        yAxis: {
          type: 'value',
          name: '°C',
          axisLabel: { fontSize: 10 },
          nameTextStyle: { fontSize: 10 }
        },
        series: [{
          name: '温度',
          type: 'line',
          smooth: true,
          data: temps,
          itemStyle: { color: '#f56c6c' },
          areaStyle: { 
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(245,108,108,0.3)' },
                { offset: 1, color: 'rgba(245,108,108,0.05)' }
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
          formatter: '{b}: {c}%'
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
          axisLabel: { fontSize: 10 }
        },
        yAxis: {
          type: 'value',
          name: '%',
          axisLabel: { fontSize: 10 },
          nameTextStyle: { fontSize: 10 }
        },
        series: [{
          name: '湿度',
          type: 'line',
          smooth: true,
          data: humis,
          itemStyle: { color: '#409eff' },
          areaStyle: { 
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(64,158,255,0.3)' },
                { offset: 1, color: 'rgba(64,158,255,0.05)' }
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

      const option = {
        series: [{
          type: 'gauge',
          min: 0,
          max: 50,
          splitNumber: 5,
          axisLine: {
            lineStyle: {
              width: 15,
              color: [
                [0.3, '#67c23a'],
                [0.7, '#e6a23c'],
                [1, '#f56c6c']
              ]
            }
          },
          pointer: {
            itemStyle: {
              color: 'auto'
            }
          },
          axisTick: {
            distance: -15,
            length: 5,
            lineStyle: {
              color: '#fff',
              width: 1
            }
          },
          splitLine: {
            distance: -20,
            length: 15,
            lineStyle: {
              color: '#fff',
              width: 2
            }
          },
          axisLabel: {
            color: 'auto',
            distance: 20,
            fontSize: 10
          },
          detail: {
            show: false
          },
          data: [{
            value: this.currentData?.temperature || 0
          }],
          animationDuration: 500
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

      const option = {
        series: [{
          type: 'gauge',
          min: 0,
          max: 100,
          splitNumber: 5,
          axisLine: {
            lineStyle: {
              width: 15,
              color: [
                [0.4, '#f56c6c'],
                [0.8, '#67c23a'],
                [1, '#409eff']
              ]
            }
          },
          pointer: {
            itemStyle: {
              color: 'auto'
            }
          },
          axisTick: {
            distance: -15,
            length: 5,
            lineStyle: {
              color: '#fff',
              width: 1
            }
          },
          splitLine: {
            distance: -20,
            length: 15,
            lineStyle: {
              color: '#fff',
              width: 2
            }
          },
          axisLabel: {
            color: 'auto',
            distance: 20,
            fontSize: 10
          },
          detail: {
            show: false
          },
          data: [{
            value: this.currentData?.humidity || 0
          }],
          animationDuration: 500
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

    getSoilColor(value) {
      // ADC值范围：0-4095
      // 土壤湿润（ADC < 1500）：绿色
      // 正常（1500-3000）：黄色
      // 干旱（> 3000）：红色
      if (value < 1500) return '#67c23a'
      if (value < 3000) return '#e6a23c'
      return '#f56c6c'
    },

    getLightPercentage(value) {
      return Math.min((value / 1000) * 100, 100)
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
.realtime-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 数据更新指示条 */
.update-indicator {
  padding: 10px 15px;
  background: linear-gradient(90deg, #d4edda 0%, #c3e6cb 100%);
  border-left: 4px solid #28a745;
  border-radius: 6px;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #155724;
  font-size: 14px;
  font-weight: 500;
  animation: slideDown 0.3s ease-out;
}

.update-icon {
  color: #28a745;
  font-size: 18px;
}

.update-indicator.pulse .update-icon {
  animation: pulse-update 2s infinite;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse-update {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 14px;
}

.status-icon {
  font-size: 16px;
  transition: all 0.3s ease;
}

.status-icon.connected {
  color: #67c23a;
  animation: pulse-green 2s infinite;
}

.status-icon.disconnected {
  color: #f56c6c;
  animation: pulse-red 2s infinite;
}

.status-text {
  font-weight: 500;
}

.status-text.connected {
  color: #67c23a;
}

.status-text.disconnected {
  color: #f56c6c;
}

@keyframes pulse-green {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

@keyframes pulse-red {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

.header-info {
  display: flex;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  margin-left: 15px;
}

.update-time {
  font-size: 13px;
  color: #909399;
  padding: 6px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 200px;
  font-variant-numeric: tabular-nums;
}

/* 当数据更新时的脉冲效果 */
.update-time:hover {
  background: #e6f7ff;
  color: #0050b3;
}

.auto-refresh {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #606266;
}

.dashboard-container {
  width: 100%;
}

.gauge-card {
  height: 280px;
  margin-bottom: 15px;
  border-radius: 12px;
}

.gauge-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.gauge-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  width: 24px;
  height: 36px;
  object-fit: contain;
}

.gauge-chart {
  width: 100%;
  height: 160px;
}

.gauge-value {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.temp-icon {
  font-size: 28px;
  color: #f56c6c;
}

.thermometer-icon {
  width: 32px;
  height: 48px;
  object-fit: contain;
}

.data-card {
  min-height: 170px;
  height: auto;
  border-radius: 12px;
  margin-bottom: 15px;
  transition: all 0.3s;
}

.data-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.soil-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-left: 4px solid #67c23a;
}

.light-card {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-left: 4px solid #e6a23c;
}

.co2-card {
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
  border-left: 4px solid #909399;
}

.card-icon {
  font-size: 48px;
  width: 70px;
  text-align: center;
}

.card-icon-img {
  width: 48px;
  height: 48px;
  object-fit: contain;
  flex-shrink: 0;
}

.card-content {
  flex: 1;
}

.card-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.card-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
  transition: color 0.3s ease;
}

/* 异常数据标记 */
.card-value.is-abnormal {
  color: #f56c6c;
  animation: abnormal-pulse 1s infinite;
}

@keyframes abnormal-pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

/* 数据统计显示 */
.data-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  font-size: 12px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  color: #909399;
  font-weight: 500;
}

.stat-value {
  color: #606266;
  font-weight: bold;
}

.unit {
  font-size: 16px;
  color: #909399;
  font-weight: normal;
}

.light-bar {
  width: 100%;
  height: 8px;
  background: #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  margin-top: 5px;
}

.light-fill {
  height: 100%;
  background: linear-gradient(90deg, #e6a23c 0%, #f59e0b 100%);
  transition: width 0.3s;
  border-radius: 4px;
}

.update-info {
  margin-top: 15px;
  background: #f8f9fa;
}

.update-time {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #606266;
}

/* 迷你趋势图样式 */
.trend-card {
  border-radius: 12px;
  margin-bottom: 15px;
}

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.trend-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 6px;
}

.trend-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.mini-chart {
  width: 100%;
  height: 150px;
}

.loading-state {
  text-align: center;
  padding: 100px 0;
  color: #409eff;
}

.loading-state p {
  margin-top: 15px;
  font-size: 16px;
}

.offline-status {
  padding: 40px 20px;
  background: white;
  border-radius: 12px;
  margin-top: 20px;
  text-align: center;
}

.button-group {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .header-info {
    width: 100%;
    justify-content: space-between;
  }

  .gauge-card {
    height: 250px;
  }

  .data-card {
    height: auto;
    min-height: 120px;
  }

  .card-value {
    font-size: 24px;
  }
}

:deep(.el-card__body) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.data-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 16px 18px;
  overflow: visible;
}

:deep(.el-progress__text) {
  display: none;
}
</style>
