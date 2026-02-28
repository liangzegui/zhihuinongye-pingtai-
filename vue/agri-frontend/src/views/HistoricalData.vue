<template>
  <div class="historical-page">
    <div class="page-header">
      <h2>📊 历史数据分析</h2>
      <p>查看环境数据趋势与历史记录</p>
    </div>

    <!-- 时间范围选择 -->
    <el-card class="filter-card" shadow="hover">
      <el-row :gutter="15" align="middle">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="filter-item">
            <span class="filter-label">时间范围：</span>
            <el-select v-model="timeRange" @change="handleTimeRangeChange" style="width: 140px;">
              <el-option label="全部时间" value="all" />
              <el-option label="最近1小时" value="1h" />
              <el-option label="最近6小时" value="6h" />
              <el-option label="最近12小时" value="12h" />
              <el-option label="最近24小时" value="24h" />
              <el-option label="最近7天" value="7d" />
            </el-select>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="filter-item">
            <span class="filter-label">数据类型：</span>
            <el-select v-model="dataType" @change="handleDataTypeChange" style="width: 140px;">
              <el-option label="全部数据" value="all" />
              <el-option label="温湿度" value="temp-humi" />
              <el-option label="土壤&光照" value="soil-light" />
              <el-option label="CO₂" value="co2" />
            </el-select>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="filter-item">
            <span class="filter-label">图表排序：</span>
            <el-select v-model="chartSortOrder" @change="handleChartSortChange" style="width: 140px;">
              <el-option label="最新在后" value="asc" />
              <el-option label="最新在前" value="desc" />
            </el-select>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="filter-item">
            <span class="filter-label">表格排序：</span>
            <el-select v-model="tableSortOrder" @change="handleTableSortChange" style="width: 140px;">
              <el-option label="最新在后" value="asc" />
              <el-option label="最新在前" value="desc" />
            </el-select>
          </div>
        </el-col>
      </el-row>
      <el-row style="margin-top: 10px;">
        <el-col :span="24">
          <el-button type="primary" @click="refreshData" :loading="loading" icon="Refresh">
            刷新数据
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 图表展示 -->
    <el-row :gutter="20">
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

    <el-row :gutter="20" style="margin-top: 20px;">
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

    <el-row :gutter="20" style="margin-top: 20px;">
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
      temperatureChart: null,
      humidityChart: null,
      soilMoistureChart: null,
      lightIntensityChart: null,
      co2Chart: null,
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
    }
  },
  mounted() {
    this.fetchHistoricalData()
    this.fetchChartData()
    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    if (this.temperatureChart) this.temperatureChart.dispose()
    if (this.humidityChart) this.humidityChart.dispose()
    if (this.soilMoistureChart) this.soilMoistureChart.dispose()
    if (this.lightIntensityChart) this.lightIntensityChart.dispose()
    if (this.co2Chart) this.co2Chart.dispose()
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
          endDate: timeParams.endDate
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
            username: item.saveUsername || item.username || item.userName || item.user_name || item.operator || fallbackUsername,
            temperature: item.temperature,
            humidity: item.humidity,
            soilAdc: item.soilAdc || item.soil_adc,
            lightIntensity: item.lightIntensity || item.light_intensity,
            co2: item.co2,
            collectTime: item.collectTime || item.collect_time
          }))
          .sort((a, b) => {
            const timeA = new Date(a.collectTime).getTime()
            const timeB = new Date(b.collectTime).getTime()
            return this.tableSortOrder === 'asc' ? timeA - timeB : timeB - timeA
          })
        
        this.total = pageData.total || pageData.count || records.length || 0
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
        // 获取更多数据用于图表展示
        const timeParams = this.getTimeRangeParams()
        const res = await getHistoricalData({ 
          page: 1, 
          pageSize: 200,
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
          this.chartData.temperatures.push(item.temperature || 0)
          this.chartData.humidities.push(item.humidity || 0)
          this.chartData.soilAdcs.push(item.soilAdc || item.soil_adc || 0)
          this.chartData.lightIntensities.push(item.lightIntensity || item.light_intensity || 0)
          this.chartData.co2Values.push(item.co2 || 0)
        })

        this.$nextTick(() => {
          this.initCharts()
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
      if (!this.$refs.temperatureChart) return

      if (!this.temperatureChart) {
        this.temperatureChart = echarts.init(this.$refs.temperatureChart)
      }

      const dataLength = this.chartData.times.length
      const labelInterval = Math.max(0, Math.floor(dataLength / 8) - 1)

      const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            formatter: '{b}<br/>{a}: {c} °C',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#eee',
            borderWidth: 1,
            textStyle: {
              color: '#333'
            }
          },
        legend: {
          data: ['温度'],
          top: 5
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '15%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100
          },
          {
            type: 'slider',
            start: 0,
            end: 100,
            height: 20,
            bottom: 5
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
            color: '#666'
          },
          axisLine: {
            lineStyle: { color: '#ddd' }
          }
        },
        yAxis: {
          type: 'value',
          name: '温度(°C)',
          axisLabel: {
            formatter: '{value} °C'
          }
        },
        series: [
          {
            name: '温度',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineWidth: 2,
            data: this.chartData.temperatures,
            itemStyle: {
              color: '#f56c6c'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(245, 108, 108, 0.25)' },
                { offset: 1, color: 'rgba(245, 108, 108, 0.02)' }
              ])
            }
          }
        ]
      }

      this.temperatureChart.setOption(option)
    },

    initHumidityChart() {
      if (!this.$refs.humidityChart) return

      if (!this.humidityChart) {
        this.humidityChart = echarts.init(this.$refs.humidityChart)
      }

      const dataLength = this.chartData.times.length
      const labelInterval = Math.max(0, Math.floor(dataLength / 8) - 1)

      const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            formatter: '{b}<br/>{a}: {c} %',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#eee',
            borderWidth: 1,
            textStyle: {
              color: '#333'
            }
          },
        legend: {
          data: ['湿度'],
          top: 5
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '15%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100
          },
          {
            type: 'slider',
            start: 0,
            end: 100,
            height: 20,
            bottom: 5
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
            color: '#666'
          },
          axisLine: {
            lineStyle: { color: '#ddd' }
          }
        },
        yAxis: {
          type: 'value',
          name: '湿度(%)',
          axisLabel: {
            formatter: '{value} %'
          }
        },
        series: [
          {
            name: '湿度',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineWidth: 2,
            data: this.chartData.humidities,
            itemStyle: {
              color: '#409eff'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(64, 158, 255, 0.25)' },
                { offset: 1, color: 'rgba(64, 158, 255, 0.02)' }
              ])
            }
          }
        ]
      }

      this.humidityChart.setOption(option)
    },

    initSoilMoistureChart() {
      if (!this.$refs.soilMoistureChart) return

      if (!this.soilMoistureChart) {
        this.soilMoistureChart = echarts.init(this.$refs.soilMoistureChart)
      }

      const dataLength = this.chartData.times.length
      const labelInterval = Math.max(0, Math.floor(dataLength / 8) - 1)

      const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            formatter: '{b}<br/>{a}: {c} ADC',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#eee',
            borderWidth: 1,
            textStyle: {
              color: '#333'
            }
          },
        legend: {
          data: ['土壤ADC'],
          top: 5
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '15%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100
          },
          {
            type: 'slider',
            start: 0,
            end: 100,
            height: 20,
            bottom: 5
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
            color: '#666'
          },
          axisLine: {
            lineStyle: { color: '#ddd' }
          }
        },
        yAxis: {
          type: 'value',
          name: '土壤ADC',
          axisLabel: {
            formatter: '{value}'
          },
          splitLine: {
            lineStyle: { color: '#eee', type: 'dashed' }
          }
        },
        series: [
          {
            name: '土壤ADC',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineWidth: 2,
            data: this.chartData.soilAdcs,
            itemStyle: {
              color: '#67c23a'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(103, 194, 58, 0.25)' },
                { offset: 1, color: 'rgba(103, 194, 58, 0.02)' }
              ])
            }
          }
        ]
      }

      this.soilMoistureChart.setOption(option)
    },

    initLightIntensityChart() {
      if (!this.$refs.lightIntensityChart) return

      if (!this.lightIntensityChart) {
        this.lightIntensityChart = echarts.init(this.$refs.lightIntensityChart)
      }

      const dataLength = this.chartData.times.length
      const labelInterval = Math.max(0, Math.floor(dataLength / 8) - 1)

      const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            formatter: '{b}<br/>{a}: {c} lux',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#eee',
            borderWidth: 1,
            textStyle: {
              color: '#333'
            }
          },
        legend: {
          data: ['光照强度'],
          top: 5
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '15%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100
          },
          {
            type: 'slider',
            start: 0,
            end: 100,
            height: 20,
            bottom: 5
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
            color: '#666'
          },
          axisLine: {
            lineStyle: { color: '#ddd' }
          }
        },
        yAxis: {
          type: 'value',
          name: '光照(lux)',
          axisLabel: {
            formatter: '{value}'
          },
          splitLine: {
            lineStyle: { color: '#eee', type: 'dashed' }
          }
        },
        series: [
          {
            name: '光照强度',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineWidth: 2,
            data: this.chartData.lightIntensities,
            itemStyle: {
              color: '#e6a23c'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(230, 162, 60, 0.25)' },
                { offset: 1, color: 'rgba(230, 162, 60, 0.02)' }
              ])
            }
          }
        ]
      }

      this.lightIntensityChart.setOption(option)
    },

    initCO2Chart() {
      if (!this.$refs.co2Chart) return
      
      if (!this.co2Chart) {
        this.co2Chart = echarts.init(this.$refs.co2Chart)
      }

      // 计算X轴标签间隔
      const dataLength = this.chartData.times.length
      const labelInterval = Math.max(0, Math.floor(dataLength / 10) - 1)
      
      const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            formatter: '{b}<br/>{a}: {c} ppm',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#eee',
            borderWidth: 1,
            textStyle: {
              color: '#333'
            }
          },
        legend: {
          data: ['CO₂浓度'],
          top: 5
        },
        grid: {
          left: '3%',
          right: '3%',
          bottom: '15%',
          top: '15%',
          containLabel: true
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100
          },
          {
            type: 'slider',
            start: 0,
            end: 100,
            height: 20,
            bottom: 5
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
            color: '#666'
          },
          axisLine: {
            lineStyle: { color: '#ddd' }
          }
        },
        yAxis: {
          type: 'value',
          name: 'CO₂(ppm)',
          axisLabel: {
            formatter: '{value}',
            fontSize: 11
          },
          splitLine: {
            lineStyle: { color: '#eee', type: 'dashed' }
          }
        },
        series: [
          {
            name: 'CO₂浓度',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineWidth: 2,
            data: this.chartData.co2Values,
            itemStyle: {
              color: '#909399'
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(144, 147, 153, 0.25)' },
                { offset: 1, color: 'rgba(144, 147, 153, 0.02)' }
              ])
            },
            markLine: {
              silent: true,
              lineStyle: {
                color: '#f56c6c',
                type: 'dashed',
                width: 2
              },
              label: {
                fontSize: 11,
                color: '#f56c6c'
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
      this.temperatureChart?.resize()
      this.humidityChart?.resize()
      this.soilMoistureChart?.resize()
      this.lightIntensityChart?.resize()
      this.co2Chart?.resize()
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
      // 根据数据类型显示/隐藏对应图表
      this.$nextTick(() => {
        this.updateChartsVisibility()
      })
    },

    updateChartsVisibility() {
      // 获取所有图表卡片
      const chartCards = document.querySelectorAll('.chart-card')
      if (!chartCards.length) return

      // 根据数据类型控制显示
      chartCards.forEach((card) => {
        const cardTitle = card.querySelector('.header-title')?.textContent || ''
        
        let shouldShow = true
        switch (this.dataType) {
          case 'temp-humi':
            shouldShow = cardTitle.includes('温度') || cardTitle.includes('湿度')
            break
          case 'soil-light':
            shouldShow = cardTitle.includes('土壤') || cardTitle.includes('光照')
            break
          case 'co2':
            shouldShow = cardTitle.includes('CO')
            break
          case 'all':
          default:
            shouldShow = true
        }
        
        card.style.display = shouldShow ? '' : 'none'
      })
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
            username: item.username || '-',
            temperature: item.temperature || '',
            humidity: item.humidity || '',
            soilAdc: item.soilAdc || '',
            lightIntensity: item.lightIntensity || '',
            co2: item.co2 || '',
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
.historical-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.chart-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  width: 24px;
  height: 24px;
  vertical-align: middle;
}

.chart-container {
  width: 100%;
  height: 350px;
}

.table-card {
  border-radius: 12px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    padding: 15px;
  }

  .chart-container {
    height: 300px;
  }

  .filter-item {
    margin-bottom: 10px;
  }
}

:deep(.el-card__body) {
  padding: 20px;
}

:deep(.el-table) {
  font-size: 13px;
}

:deep(.el-pagination) {
  justify-content: center;
}
</style>
