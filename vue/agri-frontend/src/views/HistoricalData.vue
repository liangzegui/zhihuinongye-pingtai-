<template>
  <div class="historical-page">
    <div class="page-header">
      <h2>📊 历史数据分析</h2>
      <p>查看环境数据趋势与历史记录</p>
    </div>

    <!-- 时间范围选择 -->
    <el-card class="filter-card" shadow="hover">
      <div class="filter-container">
        <div class="filter-row">
          <div class="filter-item">
            <span class="filter-label">时间范围</span>
            <el-select v-model="timeRange" @change="handleTimeRangeChange" placeholder="选择时间范围" class="filter-select">
              <el-option label="全部时间" value="all" />
              <el-option label="最近1小时" value="1h" />
              <el-option label="最近6小时" value="6h" />
              <el-option label="最近12小时" value="12h" />
              <el-option label="最近24小时" value="24h" />
              <el-option label="最近7天" value="7d" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">数据类型</span>
            <el-select v-model="dataType" @change="handleDataTypeChange" placeholder="选择数据类型" class="filter-select">
              <el-option label="全部数据" value="all" />
              <el-option label="温湿度" value="temp-humi" />
              <el-option label="土壤&光照" value="soil-light" />
              <el-option label="CO₂" value="co2" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">图表数据排序</span>
            <el-select v-model="chartSortOrder" @change="handleChartSortChange" placeholder="选择排序方式" class="filter-select">
              <el-option label="最新在后" value="asc" />
              <el-option label="最新在前" value="desc" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">表格数据排序</span>
            <el-select v-model="tableSortOrder" @change="handleTableSortChange" placeholder="选择排序方式" class="filter-select">
              <el-option label="最新在后" value="asc" />
              <el-option label="最新在前" value="desc" />
            </el-select>
          </div>
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="refreshData" :loading="loading" icon="Refresh">
            刷新数据
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 图表展示 -->
    <el-row :gutter="20" v-show="showTempHumiCharts">
      <el-col :xs="24" :sm="24" :md="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-title">
                <img src="@/assets/thermometer.svg" alt="温度" class="header-icon" />
                温度变化趋势
              </span>
              <el-tag type="info" size="small">{{ timeRangeText }}</el-tag>
            </div>
          </template>
          <div ref="temperatureChart" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-title">
                <img src="@/assets/humidity.svg" alt="湿度" class="header-icon" />
                湿度变化趋势
              </span>
              <el-tag type="info" size="small">{{ timeRangeText }}</el-tag>
            </div>
          </template>
          <div ref="humidityChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;" v-show="showSoilLightCharts">
      <el-col :xs="24" :sm="24" :md="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-title">
                <img src="@/assets/soil.svg" alt="土壤" class="header-icon" />
                土壤ADC变化趋势
              </span>
              <el-tag type="info" size="small">{{ timeRangeText }}</el-tag>
            </div>
          </template>
          <div ref="soilMoistureChart" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-title">
                <img src="@/assets/light.svg" alt="光照" class="header-icon" />
                光照强度变化趋势
              </span>
              <el-tag type="info" size="small">{{ timeRangeText }}</el-tag>
            </div>
          </template>
          <div ref="lightIntensityChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;" v-show="showCO2Chart">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-title">
                <img src="@/assets/co2.svg" alt="CO2" class="header-icon" />
                CO₂浓度变化趋势
              </span>
              <el-tag type="info" size="small">{{ timeRangeText }}</el-tag>
            </div>
          </template>
          <div ref="co2Chart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史数据表格 -->
    <el-card class="table-card" shadow="hover" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span class="header-title">📋 历史数据记录</span>
          <el-button size="small" @click="exportData" icon="Download">导出数据</el-button>
        </div>
      </template>

      <el-table
        :data="tableData"
        stripe
        style="width: 100%"
        v-loading="tableLoading"
        :height="400"
      >
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="temperature" label="温度(℃)" width="100">
          <template #default="{ row }">
            <el-tag :type="getTemperatureType(row.temperature)" size="small">
              {{ row.temperature }}°C
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="humidity" label="湿度(%)" width="100">
          <template #default="{ row }">
            {{ row.humidity }}%
          </template>
        </el-table-column>
        <el-table-column prop="soilAdc" label="土壤ADC" width="120" />
        <el-table-column prop="lightIntensity" label="光照(lux)" width="110" />
        <el-table-column prop="co2" label="CO₂(ppm)" width="110" />
        <el-table-column prop="collectTime" label="采集时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.collectTime) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          prev-text="上一页"
          next-text="下一页"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>


<script>
import * as echarts from 'echarts'
import ExcelJS from 'exceljs'
import { ElMessage } from 'element-plus'
import { getHistoricalData } from '@/api/data'
import { getUsername } from '@/utils/token'

export default {
  name: 'HistoricalData',
  data() {
    return {
      tableData: [],
      page: 1,
      size: 10,
      total: 0,
      loading: false,
      tableLoading: false,
      timeRange: 'all',
      dataType: 'all',
      chartSortOrder: 'asc', // 图表时间排序: asc(最新在后) 或 desc(最新在前)
      tableSortOrder: 'desc', // 表格时间排序: asc(最新在后) 或 desc(最新在前)
      // 注意：图表实例不放在 data() 中，避免被 Vue 3 Proxy 代理导致 ECharts 内部崩溃
      chartData: {
        times: [],
        temperatures: [],
        humidities: [],
        soilAdcs: [],
        lightIntensities: [],
        co2Values: []
      }
    }
  },
  created() {
    // ECharts 实例必须作为非响应式属性存储
    // 放在 data() 中会被 Vue 3 的 Proxy 包裹，导致 ECharts 内部调度器
    // 通过代理访问 coordinateSystem 时返回 undefined → dataSample.js 崩溃
    this.temperatureChart = null
    this.humidityChart = null
    this.soilMoistureChart = null
    this.lightIntensityChart = null
    this.co2Chart = null
  },
  computed: {
    timeRangeText() {
      const map = {
        'all': '全部时间',
        '1h': '最近1小时',
        '6h': '最近6小时',
        '12h': '最近12小时',
        '24h': '最近24小时',
        '7d': '最近7天'
      }
      return map[this.timeRange] || '最近24小时'
    },
    showTempHumiCharts() {
      return this.dataType === 'all' || this.dataType === 'temp-humi'
    },
    showSoilLightCharts() {
      return this.dataType === 'all' || this.dataType === 'soil-light'
    },
    showCO2Chart() {
      return this.dataType === 'all' || this.dataType === 'co2'
    }
  },
  mounted() {
    this.fetchHistoricalData()
    this.fetchChartData()
    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    this.disposeAllCharts()
  },
  methods: {
    // 根据时间范围计算起止时间
    getTimeRangeParams() {
      const now = new Date()
      let startDate = null
      
      switch (this.timeRange) {
        case 'all':
          // 全部时间：不设置起始时间
          startDate = null
          break
        case '1h':
          startDate = new Date(now.getTime() - 1 * 60 * 60 * 1000) // 减去1小时的毫秒数
          break
        case '6h':
          startDate = new Date(now.getTime() - 6 * 60 * 60 * 1000) // 减去6小时
          break
        case '12h':
          startDate = new Date(now.getTime() - 12 * 60 * 60 * 1000) // 减去12小时
          break
        case '24h':
          startDate = new Date(now.getTime() - 24 * 60 * 60 * 1000) // 减去24小时
          break
        case '7d':
          startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000) // 减去7天
          break
        default:
          startDate = new Date(now.getTime() - 24 * 60 * 60 * 1000)
      }
      
      // 辅助函数：格式化为本地时间字符串
      const formatLocalDateTime = (date) => {
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        const hours = String(date.getHours()).padStart(2, '0')
        const minutes = String(date.getMinutes()).padStart(2, '0')
        const seconds = String(date.getSeconds()).padStart(2, '0')
        return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
      }
      
      return {
        startDate: startDate ? formatLocalDateTime(startDate) : null,
        endDate: this.timeRange === 'all' ? null : formatLocalDateTime(now)
      }
    },

    async fetchHistoricalData() {
      this.tableLoading = true
      try {
        const timeParams = this.getTimeRangeParams()
        console.log('当前timeRange:', this.timeRange)
        console.log('timeParams:', timeParams)
        const res = await getHistoricalData({ 
          page: this.page, 
          pageSize: this.size,
          startDate: timeParams.startDate,
          endDate: timeParams.endDate,
          sortOrder: this.tableSortOrder
        })
        
        console.log('历史数据API响应:', res)
        
        // 处理不同的响应格式
        // 后端返回: { code, msg, data: { records, total, ... } }
        const pageData = res.data || res || {}
        
        console.log('分页数据:', pageData)
        
        const records = pageData.records || pageData.list || pageData.data || []
        const fallbackUsername = getUsername() || '-'

        this.tableData = records
          .map(item => ({
            id: item.id,
            username: item.saveUsername ?? item.username ?? item.userName ?? item.user_name ?? item.operator ?? fallbackUsername,
            temperature: item.temperature,
            humidity: item.humidity,
            soilAdc: item.soilAdc ?? item.soil_adc,
            lightIntensity: item.lightIntensity ?? item.light_intensity,
            co2: item.co2,
            collectTime: item.collectTime ?? item.collect_time
          }))
        
        this.total = (pageData.total != null ? pageData.total : (pageData.count != null ? pageData.count : records.length))
        console.log('表格数据条数:', this.tableData.length, '总条数:', this.total)
      } catch (err) {
        console.error('获取历史数据失败:', err)
        ElMessage.error('数据加载失败：' + (err.message || '网络错误'))
      } finally {
        this.tableLoading = false
      }
    },
    async fetchChartData() {
      this.loading = true
      try {
        // 先获取该时间范围的数据总数，再一次性取全部数据用于图表展示
        const timeParams = this.getTimeRangeParams()
        const countRes = await getHistoricalData({ 
          page: 1, 
          pageSize: 1,
          startDate: timeParams.startDate,
          endDate: timeParams.endDate
        })
        const countData = countRes.data || countRes || {}
        const totalRecords = countData.total || countData.count || 0
        // 根据总数确定图表请求的 pageSize，至少取200条，最多取5000条防止数据量过大
        const chartPageSize = Math.max(200, Math.min(totalRecords, 5000))
        const res = await getHistoricalData({ 
          page: 1, 
          pageSize: chartPageSize,
          startDate: timeParams.startDate,
          endDate: timeParams.endDate
        })
        const data = res.data || res || {}
        const records = data.records || data.list || data.data || []
        
        // 准备图表数据
        this.chartData = {
          times: [],
          temperatures: [],
          humidities: [],
          soilAdcs: [],
          lightIntensities: [],
          co2Values: []
        }

        const sortedRecords = [...records].sort((a, b) => {
          const timeA = new Date(a.collectTime || a.collect_time).getTime()
          const timeB = new Date(b.collectTime || b.collect_time).getTime()
          return this.chartSortOrder === 'asc' ? timeA - timeB : timeB - timeA
        })

        sortedRecords.forEach(item => {
          const time = this.formatChartTime(item.collectTime || item.collect_time)
          this.chartData.times.push(time)
          this.chartData.temperatures.push(item.temperature != null ? item.temperature : 0)
          this.chartData.humidities.push(item.humidity != null ? item.humidity : 0)
          this.chartData.soilAdcs.push(item.soilAdc != null ? item.soilAdc : (item.soil_adc != null ? item.soil_adc : 0))
          this.chartData.lightIntensities.push(item.lightIntensity != null ? item.lightIntensity : (item.light_intensity != null ? item.light_intensity : 0))
          this.chartData.co2Values.push(item.co2 != null ? item.co2 : 0)
        })

        this.$nextTick(() => {
          // 使用 requestAnimationFrame 确保浏览器完成布局计算，避免图表容器尺寸为0导致 ECharts coordinateSystem 未创建
          requestAnimationFrame(() => {
            this.initCharts()
          })
        })
      } catch (err) {
        console.error('获取图表数据失败:', err)
        ElMessage.error('图表数据加载失败')
      } finally {
        this.loading = false
      }
    },

    initCharts() {
      this.initTemperatureChart()
      this.initHumidityChart()
      this.initSoilMoistureChart()
      this.initLightIntensityChart()
      this.initCO2Chart()
    },

    initTemperatureChart() {
      const elem = this.$refs.temperatureChart
      if (!elem || elem.offsetWidth === 0 || elem.offsetHeight === 0) return

      // 销毁旧实例再创建，避免 ECharts 内部状态残留导致 dataSample/markLine 崩溃
      if (this.temperatureChart) {
        this.temperatureChart.dispose()
        this.temperatureChart = null
      }
      this.temperatureChart = echarts.init(elem)

      const dataLength = this.chartData.times.length
      const labelInterval = Math.max(0, Math.floor(dataLength / 8) - 1)

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross', crossStyle: { color: '#94a3b8' } },
          formatter: '{b}<br/>{a}: {c} °C',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: 'rgba(71, 85, 99, 0.1)',
          borderWidth: 1,
          textStyle: { color: '#1e293b', fontSize: 12 },
          boxShadow: '0 4px 12px rgba(26, 71, 42, 0.1)'
        },
        legend: {
          data: ['温度'],
          top: 5,
          textStyle: { color: '#475569', fontSize: 12 }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '18%',
          top: '15%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100,
            zoomOnMouseWheel: true,
            moveOnMouseMove: true
          },
          {
            type: 'slider',
            show: true,
            realtime: true,
            start: 0,
            end: 100,
            height: 25,
            bottom: 8,
            handleSize: '110%',
            borderColor: 'rgba(71, 85, 99, 0.2)',
            fillerColor: 'rgba(239, 68, 68, 0.15)',
            handleStyle: { color: '#ef4444' }
          }
        ],
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.chartData.times,
          axisLabel: {
            interval: labelInterval,
            rotate: 30,
            fontSize: 11,
            color: '#64748b'
          },
          axisLine: { lineStyle: { color: 'rgba(71, 85, 99, 0.2)' } },
          splitLine: { show: false }
        },
        yAxis: {
          type: 'value',
          name: '温度(°C)',
          nameTextStyle: { color: '#64748b', fontSize: 11 },
          axisLabel: { formatter: '{value}°C', color: '#64748b', fontSize: 11 },
          splitLine: { lineStyle: { color: 'rgba(71, 85, 99, 0.08)', type: 'dashed' } }
        },
        series: [
          {
            name: '温度',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineWidth: 2.5,
            data: this.chartData.temperatures,
            itemStyle: { color: '#ef4444' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(239, 68, 68, 0.2)' },
                { offset: 1, color: 'rgba(239, 68, 68, 0.02)' }
              ])
            }
          }
        ]
      }

      this.temperatureChart.setOption(option)
    },

    initHumidityChart() {
      const elem = this.$refs.humidityChart
      if (!elem || elem.offsetWidth === 0 || elem.offsetHeight === 0) return

      if (this.humidityChart) {
        this.humidityChart.dispose()
        this.humidityChart = null
      }
      this.humidityChart = echarts.init(elem)

      const dataLength = this.chartData.times.length
      const labelInterval = Math.max(0, Math.floor(dataLength / 8) - 1)

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross', crossStyle: { color: '#94a3b8' } },
          formatter: '{b}<br/>{a}: {c} %',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: 'rgba(71, 85, 99, 0.1)',
          borderWidth: 1,
          textStyle: { color: '#1e293b', fontSize: 12 }
        },
        legend: {
          data: ['湿度'],
          top: 5,
          textStyle: { color: '#475569', fontSize: 12 }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '18%',
          top: '15%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100,
            zoomOnMouseWheel: true,
            moveOnMouseMove: true
          },
          {
            type: 'slider',
            show: true,
            realtime: true,
            start: 0,
            end: 100,
            height: 25,
            bottom: 8,
            handleSize: '110%',
            borderColor: 'rgba(71, 85, 99, 0.2)',
            fillerColor: 'rgba(20, 184, 166, 0.15)',
            handleStyle: { color: '#14b8a6' }
          }
        ],
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.chartData.times,
          axisLabel: {
            interval: labelInterval,
            rotate: 30,
            fontSize: 11,
            color: '#64748b'
          },
          axisLine: { lineStyle: { color: 'rgba(71, 85, 99, 0.2)' } },
          splitLine: { show: false }
        },
        yAxis: {
          type: 'value',
          name: '湿度(%)',
          nameTextStyle: { color: '#64748b', fontSize: 11 },
          axisLabel: { formatter: '{value}%', color: '#64748b', fontSize: 11 },
          splitLine: { lineStyle: { color: 'rgba(71, 85, 99, 0.08)', type: 'dashed' } }
        },
        series: [
          {
            name: '湿度',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineWidth: 2.5,
            data: this.chartData.humidities,
            itemStyle: { color: '#14b8a6' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(20, 184, 166, 0.2)' },
                { offset: 1, color: 'rgba(20, 184, 166, 0.02)' }
              ])
            }
          }
        ]
      }

      this.humidityChart.setOption(option)
    },

    initSoilMoistureChart() {
      const elem = this.$refs.soilMoistureChart
      if (!elem || elem.offsetWidth === 0 || elem.offsetHeight === 0) return

      if (this.soilMoistureChart) {
        this.soilMoistureChart.dispose()
        this.soilMoistureChart = null
      }
      this.soilMoistureChart = echarts.init(elem)

      const dataLength = this.chartData.times.length
      const labelInterval = Math.max(0, Math.floor(dataLength / 8) - 1)

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross', crossStyle: { color: '#94a3b8' } },
          formatter: '{b}<br/>{a}: {c} ADC',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: 'rgba(71, 85, 99, 0.1)',
          borderWidth: 1,
          textStyle: { color: '#1e293b', fontSize: 12 }
        },
        legend: {
          data: ['土壤ADC'],
          top: 5,
          textStyle: { color: '#475569', fontSize: 12 }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '18%',
          top: '15%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100,
            zoomOnMouseWheel: true,
            moveOnMouseMove: true
          },
          {
            type: 'slider',
            show: true,
            realtime: true,
            start: 0,
            end: 100,
            height: 25,
            bottom: 8,
            handleSize: '110%',
            borderColor: 'rgba(71, 85, 99, 0.2)',
            fillerColor: 'rgba(58, 125, 68, 0.15)',
            handleStyle: { color: '#3a7d44' }
          }
        ],
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.chartData.times,
          axisLabel: {
            interval: labelInterval,
            rotate: 30,
            fontSize: 11,
            color: '#64748b'
          },
          axisLine: { lineStyle: { color: 'rgba(71, 85, 99, 0.2)' } },
          splitLine: { show: false }
        },
        yAxis: {
          type: 'value',
          name: '土壤ADC',
          nameTextStyle: { color: '#64748b', fontSize: 11 },
          axisLabel: { formatter: '{value}', color: '#64748b', fontSize: 11 },
          splitLine: { lineStyle: { color: 'rgba(71, 85, 99, 0.08)', type: 'dashed' } }
        },
        series: [
          {
            name: '土壤ADC',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineWidth: 2.5,
            data: this.chartData.soilAdcs,
            itemStyle: { color: '#3a7d44' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(58, 125, 68, 0.2)' },
                { offset: 1, color: 'rgba(58, 125, 68, 0.02)' }
              ])
            }
          }
        ]
      }

      this.soilMoistureChart.setOption(option)
    },

    initLightIntensityChart() {
      const elem = this.$refs.lightIntensityChart
      if (!elem || elem.offsetWidth === 0 || elem.offsetHeight === 0) return

      if (this.lightIntensityChart) {
        this.lightIntensityChart.dispose()
        this.lightIntensityChart = null
      }
      this.lightIntensityChart = echarts.init(elem)

      const dataLength = this.chartData.times.length
      const labelInterval = Math.max(0, Math.floor(dataLength / 8) - 1)

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross', crossStyle: { color: '#94a3b8' } },
          formatter: '{b}<br/>{a}: {c} lux',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: 'rgba(71, 85, 99, 0.1)',
          borderWidth: 1,
          textStyle: { color: '#1e293b', fontSize: 12 }
        },
        legend: {
          data: ['光照强度'],
          top: 5,
          textStyle: { color: '#475569', fontSize: 12 }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '18%',
          top: '15%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100,
            zoomOnMouseWheel: true,
            moveOnMouseMove: true
          },
          {
            type: 'slider',
            show: true,
            realtime: true,
            start: 0,
            end: 100,
            height: 25,
            bottom: 8,
            handleSize: '110%',
            borderColor: 'rgba(71, 85, 99, 0.2)',
            fillerColor: 'rgba(217, 119, 6, 0.15)',
            handleStyle: { color: '#d97706' }
          }
        ],
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.chartData.times,
          axisLabel: {
            interval: labelInterval,
            rotate: 30,
            fontSize: 11,
            color: '#64748b'
          },
          axisLine: { lineStyle: { color: 'rgba(71, 85, 99, 0.2)' } },
          splitLine: { show: false }
        },
        yAxis: {
          type: 'value',
          name: '光照(lux)',
          nameTextStyle: { color: '#64748b', fontSize: 11 },
          axisLabel: { formatter: '{value}', color: '#64748b', fontSize: 11 },
          splitLine: { lineStyle: { color: 'rgba(71, 85, 99, 0.08)', type: 'dashed' } }
        },
        series: [
          {
            name: '光照强度',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineWidth: 2.5,
            data: this.chartData.lightIntensities,
            itemStyle: { color: '#d97706' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(217, 119, 6, 0.2)' },
                { offset: 1, color: 'rgba(217, 119, 6, 0.02)' }
              ])
            }
          }
        ]
      }

      this.lightIntensityChart.setOption(option)
    },

    initCO2Chart() {
      const elem = this.$refs.co2Chart
      if (!elem || elem.offsetWidth === 0 || elem.offsetHeight === 0) return

      if (this.co2Chart) {
        this.co2Chart.dispose()
        this.co2Chart = null
      }
      this.co2Chart = echarts.init(elem)

      // 计算X轴标签间隔
      const dataLength = this.chartData.times.length
      const labelInterval = Math.max(0, Math.floor(dataLength / 10) - 1)

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross', crossStyle: { color: '#94a3b8' } },
          formatter: '{b}<br/>{a}: {c} ppm',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: 'rgba(71, 85, 99, 0.1)',
          borderWidth: 1,
          textStyle: { color: '#1e293b', fontSize: 12 }
        },
        legend: {
          data: ['CO₂浓度'],
          top: 5,
          textStyle: { color: '#475569', fontSize: 12 }
        },
        grid: {
          left: '3%',
          right: '3%',
          bottom: '18%',
          top: '15%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100,
            zoomOnMouseWheel: true,
            moveOnMouseMove: true
          },
          {
            type: 'slider',
            show: true,
            realtime: true,
            start: 0,
            end: 100,
            height: 25,
            bottom: 8,
            handleSize: '110%',
            borderColor: 'rgba(71, 85, 99, 0.2)',
            fillerColor: 'rgba(15, 118, 110, 0.15)',
            handleStyle: { color: '#0f766e' }
          }
        ],
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.chartData.times,
          axisLabel: {
            interval: labelInterval,
            rotate: 20,
            fontSize: 11,
            color: '#64748b'
          },
          axisLine: { lineStyle: { color: 'rgba(71, 85, 99, 0.2)' } },
          splitLine: { show: false }
        },
        yAxis: {
          type: 'value',
          name: 'CO₂(ppm)',
          nameTextStyle: { color: '#64748b', fontSize: 11 },
          axisLabel: { formatter: '{value}', color: '#64748b', fontSize: 11 },
          splitLine: { lineStyle: { color: 'rgba(71, 85, 99, 0.08)', type: 'dashed' } }
        },
        series: [
          {
            name: 'CO₂浓度',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineWidth: 2.5,
            data: this.chartData.co2Values,
            itemStyle: { color: '#0f766e' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(15, 118, 110, 0.2)' },
                { offset: 1, color: 'rgba(15, 118, 110, 0.02)' }
              ])
            },
            markLine: {
              silent: true,
              lineStyle: {
                color: '#ef4444',
                type: 'dashed',
                width: 2
              },
              label: {
                fontSize: 11,
                color: '#ef4444',
                backgroundColor: 'rgba(255, 255, 255, 0.9)',
                padding: [4, 8],
                borderRadius: 4
              },
              data: [
                { yAxis: 1000, label: { formatter: '警戒线 1000ppm' } }
              ]
            }
          }
        ]
      }

      this.co2Chart.setOption(option)
    },

    handleResize() {
      // 只对容器可见（非零尺寸）的图表执行 resize，
      // 避免 ECharts 在 0 尺寸容器上将 coordinateSystem 置为 undefined 导致 dataSample.js 崩溃
      const pairs = [
        [this.$refs.temperatureChart, this.temperatureChart],
        [this.$refs.humidityChart, this.humidityChart],
        [this.$refs.soilMoistureChart, this.soilMoistureChart],
        [this.$refs.lightIntensityChart, this.lightIntensityChart],
        [this.$refs.co2Chart, this.co2Chart]
      ]
      for (const [el, chart] of pairs) {
        if (chart && el && el.offsetWidth > 0 && el.offsetHeight > 0) {
          chart.resize()
        }
      }
    },

    handleSizeChange(size) {
      this.size = size
      this.page = 1
      this.fetchHistoricalData()
    },

    handleTimeRangeChange() {
      this.refreshData()
    },

    handleDataTypeChange() {
      // 关键：先同步销毁所有图表实例。
      // 不能只销毁"隐藏的"，因为此时 Vue 尚未更新 DOM，v-show 变化还在队列中，
      // 检查 offsetWidth 得到的仍然是旧值 → 即将被隐藏的图表不会被 dispose →
      // DOM 更新后容器变 0 尺寸，但旧实例调度器仍存活 → dataSample 崩溃。
      this.disposeAllCharts()
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.initCharts()
        })
      })
    },

    // 无条件销毁所有图表实例
    disposeAllCharts() {
      const props = ['temperatureChart', 'humidityChart', 'soilMoistureChart', 'lightIntensityChart', 'co2Chart']
      for (const prop of props) {
        if (this[prop]) {
          this[prop].dispose()
          this[prop] = null
        }
      }
    },

    handlePageChange(page) {
      this.page = page
      this.fetchHistoricalData()
    },

    handleChartSortChange() {
      // 只刷新图表数据
      this.fetchChartData()
    },

    handleTableSortChange() {
      // 只刷新表格数据
      this.fetchHistoricalData()
    },

    refreshData() {
      this.fetchHistoricalData()
      this.fetchChartData()
    },

    async exportData() {
      if (!this.tableData || this.tableData.length === 0) {
        ElMessage.warning('暂无数据可导出')
        return
      }

      try {
        // 创建工作簿
        const workbook = new ExcelJS.Workbook()
        const worksheet = workbook.addWorksheet('历史数据')
        
        // 设置列
        worksheet.columns = [
          { header: '账号', key: 'username', width: 15 },
          { header: '温度(℃)', key: 'temperature', width: 12 },
          { header: '湿度(%)', key: 'humidity', width: 12 },
          { header: '土壤ADC', key: 'soilAdc', width: 12 },
          { header: '光照(lux)', key: 'lightIntensity', width: 14 },
          { header: 'CO₂(ppm)', key: 'co2', width: 16 },
          { header: '采集时间', key: 'collectTime', width: 26 }
        ]
        
        // 添加数据行
        this.tableData.forEach(item => {
          worksheet.addRow({
            username: item.username ?? '-',
            temperature: item.temperature ?? '',
            humidity: item.humidity ?? '',
            soilAdc: item.soilAdc ?? '',
            lightIntensity: item.lightIntensity ?? '',
            co2: item.co2 ?? '',
            collectTime: this.formatExportTime(item.collectTime)
          })
        })
        
        // 设置采集时间列（G列）右对齐
        worksheet.getColumn('collectTime').alignment = { horizontal: 'right' }
        
        // 设置表头样式
        worksheet.getRow(1).font = { bold: true }
        worksheet.getRow(1).alignment = { horizontal: 'center' }
        
        // 导出文件
        const buffer = await workbook.xlsx.writeBuffer()
        const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `历史数据_${new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')}.xlsx`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)

        ElMessage.success('数据导出成功')
      } catch (err) {
        console.error('导出失败:', err)
        ElMessage.error('导出失败：' + (err.message || '未知错误'))
      }
    },

    // 导出用的时间格式（不含逗号，Excel友好）
    formatExportTime(timeStr) {
      if (!timeStr) return '-'
      const date = new Date(timeStr)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      const second = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hour}:${minute}:${second}`
    },

    formatTime(timeStr) {
      if (!timeStr) return '-'
      return new Date(timeStr).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    },

    formatChartTime(timeStr) {
      if (!timeStr) return '-'
      const date = new Date(timeStr)
      return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
    },

    getTemperatureType(temp) {
      if (temp < 15) return 'info'
      if (temp < 25) return 'success'
      if (temp < 35) return 'warning'
      return 'danger'
    }
  }
}
</script>


<style scoped>
/* ========== Design Tokens: 智慧农业主题 ========== */
/* Primary: #1a472a (深森林绿)  Accent: #3a7d44 (森林绿)
   Secondary: #0f766e (青色)    Surface: #f0fdf4 (薄荷绿)
   Text: #1e293b / #475569 / #94a3b8
   Border: rgba(71, 85, 99, 0.1) */

.historical-page {
  padding: 20px;
  background-color: #f8f9fa;
}

.page-header {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
  padding: 24px 28px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  box-shadow: 0 4px 20px rgba(26, 71, 42, 0.06);
  position: relative;
  z-index: 10;
}

.page-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #1a472a, #3a7d44, #22c55e);
  border-radius: 16px 16px 0 0;
}

.page-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
  letter-spacing: -0.02em;
}

.page-header p {
  margin: 0;
  font-size: 14px;
  color: #6c757d;
}

/* ========== Filter Card ========== */
.filter-card {
  margin-bottom: 20px;
  border-radius: 14px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
}

.filter-card :deep(.el-card__body) {
  padding: 20px;
}

/* 筛选容器 - 现代化布局 */
.filter-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 200px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #1a472a;
  white-space: nowrap;
  flex-shrink: 0;
  min-width: 80px;
}

.filter-select {
  width: 150px;
}

.filter-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 统一筛选框样式 */
.filter-select :deep(.el-input__wrapper) {
  border-radius: 8px;
  border: 1px solid rgba(58, 125, 68, 0.2);
  background: #fff;
  transition: all 0.2s ease;
}

.filter-select :deep(.el-input__wrapper:hover) {
  border-color: rgba(58, 125, 68, 0.4);
  box-shadow: 0 2px 8px rgba(58, 125, 68, 0.1);
}

.filter-select :deep(.el-input.is-focus .el-input__wrapper) {
  border-color: #3a7d44;
  box-shadow: 0 0 0 2px rgba(58, 125, 68, 0.1);
}

.filter-select :deep(.el-input__inner) {
  color: #1a472a;
  font-size: 13px;
  font-weight: 500;
  text-align: center;
}

.filter-select :deep(.el-select__placeholder) {
  color: rgba(26, 71, 42, 0.5);
  font-size: 13px;
  text-align: center;
  width: 100%;
}

.filter-select :deep(.el-select__selected-item) {
  text-align: center;
  width: 100%;
}

.filter-select :deep(.el-input__suffix) {
  position: absolute;
  right: 8px;
}

.filter-select :deep(.el-select__caret) {
  color: rgba(58, 125, 68, 0.7);
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .filter-row {
    gap: 15px;
  }

  .filter-item {
    min-width: 180px;
  }

  .filter-select {
    width: 140px;
  }
}

@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    gap: 12px;
  }

  .filter-item {
    flex-direction: column;
    align-items: flex-start;
    width: 100%;
    min-width: unset;
    gap: 8px;
  }

  .filter-label {
    min-width: unset;
  }

  .filter-select {
    width: 100%;
  }

  .filter-actions {
    width: 100%;
  }

  .filter-actions .el-button {
    flex: 1;
  }
}

/* ========== Chart Cards ========== */
.chart-card {
  margin-bottom: 20px;
  border-radius: 8px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.chart-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(26, 71, 42, 0.1);
}

.chart-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(71, 85, 99, 0.08);
  background: linear-gradient(180deg, rgba(240, 253, 244, 0.5) 0%, transparent 100%);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #343a40;
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: -0.01em;
}

.header-icon {
  width: 20px;
  height: 20px;
  margin-right: 8px;
}

.chart-container {
  width: 100%;
  height: 300px;
}

/* ========== Table Card ========== */
.table-card {
  border-radius: 16px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
}

.table-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(71, 85, 99, 0.08);
  background: linear-gradient(180deg, rgba(240, 253, 244, 0.5) 0%, transparent 100%);
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}


</style>

