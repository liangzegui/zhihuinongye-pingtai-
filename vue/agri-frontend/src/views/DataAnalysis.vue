<template>
  <div class="analysis-page">
    <div class="page-header">
      <h2>📊 数据趋势分析</h2>
      <p>可视化展示环境参数变化趋势与多维度分析</p>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="hover">
      <div class="filter-container">
        <div class="filter-row">
          <div class="filter-item">
            <span class="filter-label">时间范围</span>
            <el-select v-model="timeRange" @change="fetchAnalysisData" placeholder="选择时间范围" class="filter-select">
              <el-option label="全部时间" value="all" />
              <el-option label="最近1天" value="1day" />
              <el-option label="最近3天" value="3day" />
              <el-option label="最近7天" value="7day" />
              <el-option label="最近14天" value="14day" />
              <el-option label="最近30天" value="30day" />
              <el-option label="最近90天" value="90day" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">分析类型</span>
            <el-select v-model="analysisType" @change="handleAnalysisTypeChange" placeholder="选择分析类型" class="filter-select">
              <el-option label="趋势分析" value="trend" />
              <el-option label="对比分析" value="compare" />
              <el-option label="统计分析" value="statistics" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">数据排序</span>
            <el-select v-model="chartSortOrder" @change="handleSortChange" placeholder="选择排序方式" class="filter-select">
              <el-option label="时间升序" value="asc" />
              <el-option label="时间降序" value="desc" />
            </el-select>
          </div>
          <div class="filter-item filter-info">
            <span class="filter-label">数据点数：{{ stats.dataCount || 0 }}</span>
          </div>
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="fetchAnalysisData" :loading="loading" icon="Refresh">
            刷新数据
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计数据卡片 -->
    <el-row :gutter="20" style="margin: 20px 0;">
      <el-col :xs="12" :sm="8" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-label">平均温度</div>
            <div class="stat-value">{{ stats.avgTemp || '--' }} °C</div>
            <div class="stat-trend" :class="{ up: stats.tempTrend > 0, down: stats.tempTrend < 0 }">
              {{ stats.tempTrend > 0 ? '↑' : '↓' }} {{ Math.abs(stats.tempTrend || 0) }}%
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-label">平均湿度</div>
            <div class="stat-value">{{ stats.avgHumi || '--' }} %</div>
            <div class="stat-trend" :class="{ up: stats.humiTrend > 0, down: stats.humiTrend < 0 }">
              {{ stats.humiTrend > 0 ? '↑' : '↓' }} {{ Math.abs(stats.humiTrend || 0) }}%
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-label">平均光照</div>
            <div class="stat-value">{{ stats.avgLight || '--' }} lux</div>
            <div class="stat-trend" :class="{ up: stats.lightTrend > 0, down: stats.lightTrend < 0 }">
              {{ stats.lightTrend > 0 ? '↑' : '↓' }} {{ Math.abs(stats.lightTrend || 0) }}%
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-label">平均CO₂</div>
            <div class="stat-value">{{ stats.avgCO2 || '--' }} ppm</div>
            <div class="stat-trend" :class="{ up: stats.co2Trend > 0, down: stats.co2Trend < 0 }">
              {{ stats.co2Trend > 0 ? '↑' : '↓' }} {{ Math.abs(stats.co2Trend || 0) }}%
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图表 -->
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-title">
                <img src="@/assets/thermometer.svg" alt="温度" class="header-icon" />
                温湿度趋势
              </span>
              <el-tag type="info" size="small">{{ timeRangeText }}</el-tag>
            </div>
          </template>
          <div ref="tempHumiChart" class="chart-container" style="height: 400px;"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-title">
                <img src="@/assets/light.svg" alt="光照" class="header-icon" />
                土壤 & 光照趋势
              </span>
              <el-tag type="info" size="small">{{ timeRangeText }}</el-tag>
            </div>
          </template>
          <div ref="soilLightChart" class="chart-container" style="height: 400px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- CO2趋势图 -->
    <el-card class="chart-card" shadow="hover" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span class="header-title">
            <img src="@/assets/co2.svg" alt="CO2" class="header-icon" />
            CO₂浓度趋势
          </span>
          <el-tag type="info" size="small">{{ timeRangeText }}</el-tag>
        </div>
      </template>
      <div ref="co2Chart" class="chart-container" style="height: 400px;"></div>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getTemperatureTrend, getSoilTrend, getCO2Trend, getDataSummary } from '@/api/data'
import { ElMessage } from 'element-plus'

export default {
  name: 'DataAnalysis',
  data() {
    return {
      timeRange: 'all',
      analysisType: 'trend',
      chartSortOrder: 'asc',
      loading: false,
      stats: {
        avgTemp: 0,
        avgHumi: 0,
        avgLight: 0,
        avgCO2: 0,
        dataCount: 0,
        tempTrend: 0,
        humiTrend: 0,
        lightTrend: 0,
        co2Trend: 0
      },
      // 注意：chartInstances 不放在 data() 中，避免被 Vue 3 Proxy 代理
      trendData: {
        timestamps: [],
        temp: [],
        humi: [],
        soil: [],
        light: [],
        co2: []
      }
    };
  },
  created() {
    // ECharts 实例必须作为非响应式属性，避免 Vue 3 Proxy 包裹导致内部崩溃
    this.chartInstances = {
      tempHumi: null,
      soilLight: null,
      co2: null
    }
  },
  computed: {
    timeRangeText() {
      const labels = {
        'all': '全部时间',
        '1day': '最近1天',
        '3day': '最近3天',
        '7day': '最近7天',
        '14day': '最近14天',
        '30day': '最近30天',
        '90day': '最近90天'
      };
      return labels[this.timeRange] || '全部时间';
    }
  },
  mounted() {
    // 初始化所有图表
    this.$nextTick(() => {
      this.initCharts();
      // 加载数据
      this.fetchAnalysisData();
    });
    // 窗口resize时刷新图表
    window.addEventListener('resize', this.resizeCharts);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.resizeCharts);
    // 销毁图表实例
    Object.values(this.chartInstances).forEach(chart => {
      if (chart) chart.dispose();
    });
  },
  methods: {
    // 初始化图表
    initCharts() {
      this.initTempHumiChart();
      this.initSoilLightChart();
      this.initCO2Chart();
    },
    
    // 初始化温湿度图表
    initTempHumiChart() {
      const elem = this.$refs.tempHumiChart;
      if (!elem || elem.offsetWidth === 0 || elem.offsetHeight === 0) return;
      this.chartInstances.tempHumi = echarts.init(elem);
      this.chartInstances.tempHumi.setOption({
        tooltip: { 
          trigger: 'axis',
          axisPointer: { type: 'cross' },
          textStyle: { fontSize: 13 }
        },
        legend: { 
          data: ['温度(℃)', '湿度(%)'], 
          top: 5,
          textStyle: { fontSize: 12 }
        },
        grid: { left: '3%', right: '4%', bottom: '20%', top: '15%', containLabel: true },
        dataZoom: [
          { type: 'inside', start: 0, end: 100, zoomOnMouseWheel: true, moveOnMouseMove: true },
          { type: 'slider', show: true, realtime: true, start: 0, end: 100, height: 25, bottom: 8, handleSize: '110%', borderColor: '#ddd' }
        ],
        xAxis: { 
          type: 'category', 
          boundaryGap: false,
          data: [],
          axisLabel: { rotate: 30, fontSize: 11, color: '#666' },
          axisLine: { lineStyle: { color: '#ddd' } }
        },
        yAxis: [
          { type: 'value', name: '温度(℃)', position: 'left', axisLabel: { formatter: '{value} °C' }, splitLine: { lineStyle: { color: '#eee', type: 'dashed' } } },
          { type: 'value', name: '湿度(%)', min: 0, max: 100, position: 'right', axisLabel: { formatter: '{value} %' }, splitLine: { show: false } }
        ],
        series: [
          { 
            name: '温度(℃)', 
            type: 'line', 
            data: [], 
            yAxisIndex: 0, 
            smooth: true,
            itemStyle: { color: '#ff6b6b' },
            areaStyle: { color: 'rgba(255, 107, 107, 0.1)' }
          },
          { 
            name: '湿度(%)', 
            type: 'line', 
            data: [], 
            yAxisIndex: 1, 
            smooth: true,
            itemStyle: { color: '#4ecdc4' },
            areaStyle: { color: 'rgba(78, 205, 196, 0.1)' }
          }
        ]
      });
    },
    
    // 初始化土壤光照图表
    initSoilLightChart() {
      const elem = this.$refs.soilLightChart;
      if (!elem || elem.offsetWidth === 0 || elem.offsetHeight === 0) return;
      this.chartInstances.soilLight = echarts.init(elem);
      this.chartInstances.soilLight.setOption({
        tooltip: { 
          trigger: 'axis',
          axisPointer: { type: 'cross' },
          textStyle: { fontSize: 13 }
        },
        legend: { 
          data: ['土壤ADC', '光照强度(lux)'], 
          top: 5,
          textStyle: { fontSize: 12 }
        },
        grid: { left: '3%', right: '4%', bottom: '20%', top: '15%', containLabel: true },
        dataZoom: [
          { type: 'inside', start: 0, end: 100, zoomOnMouseWheel: true, moveOnMouseMove: true },
          { type: 'slider', show: true, realtime: true, start: 0, end: 100, height: 25, bottom: 8, handleSize: '110%', borderColor: '#ddd' }
        ],
        xAxis: { 
          type: 'category', 
          boundaryGap: false,
          data: [],
          axisLabel: { rotate: 30, fontSize: 11, color: '#666' },
          axisLine: { lineStyle: { color: '#ddd' } }
        },
        yAxis: [
          { type: 'value', name: '土壤ADC', position: 'left', splitLine: { lineStyle: { color: '#eee', type: 'dashed' } } },
          { type: 'value', name: '光照(lux)', position: 'right', splitLine: { show: false } }
        ],
        series: [
          { 
            name: '土壤ADC', 
            type: 'line', 
            data: [], 
            yAxisIndex: 0, 
            smooth: true,
            itemStyle: { color: '#a0522d' },
            areaStyle: { color: 'rgba(160, 82, 45, 0.1)' }
          },
          { 
            name: '光照强度(lux)', 
            type: 'line', 
            data: [], 
            yAxisIndex: 1, 
            smooth: true,
            itemStyle: { color: '#ffd93d' },
            areaStyle: { color: 'rgba(255, 217, 61, 0.1)' }
          }
        ]
      });
    },
    
    // 初始化CO2图表
    initCO2Chart() {
      const elem = this.$refs.co2Chart;
      if (!elem || elem.offsetWidth === 0 || elem.offsetHeight === 0) return;
      this.chartInstances.co2 = echarts.init(elem);
      this.chartInstances.co2.setOption({
        tooltip: { 
          trigger: 'axis',
          axisPointer: { type: 'cross' },
          formatter: '{b}<br/>CO₂: {c} ppm',
          textStyle: { fontSize: 13 }
        },
        legend: {
          data: ['CO₂浓度'],
          top: 5,
          textStyle: { fontSize: 12 }
        },
        grid: { left: '3%', right: '3%', bottom: '20%', top: '15%', containLabel: true },
        dataZoom: [
          { type: 'inside', start: 0, end: 100, zoomOnMouseWheel: true, moveOnMouseMove: true },
          { type: 'slider', show: true, realtime: true, start: 0, end: 100, height: 25, bottom: 8, handleSize: '110%', borderColor: '#ddd' }
        ],
        xAxis: { 
          type: 'category', 
          boundaryGap: false,
          data: [],
          axisLabel: { rotate: 20, fontSize: 11, color: '#666' },
          axisLine: { lineStyle: { color: '#ddd' } }
        },
        yAxis: { 
          type: 'value', 
          name: 'CO₂(ppm)',
          axisLabel: { formatter: '{value}', fontSize: 11 },
          splitLine: { lineStyle: { color: '#eee', type: 'dashed' } }
        },
        series: [
          { 
            name: 'CO₂浓度', 
            type: 'line', 
            data: [], 
            smooth: true, 
            itemStyle: { color: '#6c5ce7' },
            areaStyle: { color: 'rgba(108, 92, 231, 0.2)' }
          }
        ]
      });
    },
    
    // 获取分析数据
    async fetchAnalysisData() {
      this.loading = true;
      try {
        // 同时获取多个数据源
        const [trendRes, summaryRes] = await Promise.all([
          this.getTrendDataByType(),
          getDataSummary({ timeRange: this.timeRange })
        ]);
        
        // 更新趋势数据
        if (trendRes) {
          this.trendData = trendRes;
          // 检查是否有有效数据
          const hasData = this.trendData.timestamps && this.trendData.timestamps.length > 0;
          if (hasData) {
            this.updateCharts();
          } else {
            // 清除旧图表数据
            this.clearCharts();
            ElMessage.info('所选时间范围内无数据');
          }
        }
        
        // 更新统计数据
        console.log('统计数据响应:', summaryRes)
        if (summaryRes && summaryRes.data) {
          this.stats = summaryRes.data;
        }
      } catch (err) {
        console.error('[DataAnalysis] 获取分析数据异常:', err);
        ElMessage.error('获取分析数据失败，请检查网络');
      } finally {
        this.loading = false;
      }
    },
    
    // 根据类型获取趋势数据
    async getTrendDataByType() {
      try {
        const [tempRes, soilRes, co2Res] = await Promise.all([
          getTemperatureTrend({ timeRange: this.timeRange }),
          getSoilTrend({ timeRange: this.timeRange }),
          getCO2Trend({ timeRange: this.timeRange })
        ]);
        
        console.log('温度趋势响应:', tempRes)
        console.log('土壤趋势响应:', soilRes)
        console.log('CO2趋势响应:', co2Res)
        
        // 从响应中提取数据
        const tempData = tempRes?.data || tempRes || {}
        const soilData = soilRes?.data || soilRes || {}
        const co2Data = co2Res?.data || co2Res || {}
        
        // 合并数据
        return {
          timestamps: tempData.timestamps || [],
          temp: tempData.data || [],
          humi: tempData.humidity || [],
          soil: soilData.data || [],
          light: soilData.light || [],
          co2: co2Data.data || []
        };
      } catch (err) {
        console.error('[DataAnalysis] 获取趋势数据异常:', err);
        return null;
      }
    },
    
    // 销毁旧实例并重新创建，彻底避免 ECharts 内部状态残留（dataSample/markLine 等）
    getOrRecreateChart(key, refName) {
      if (this.chartInstances[key]) {
        this.chartInstances[key].dispose();
        this.chartInstances[key] = null;
      }
      const elem = this.$refs[refName];
      if (!elem || elem.offsetWidth === 0 || elem.offsetHeight === 0) return null;
      this.chartInstances[key] = echarts.init(elem);
      return this.chartInstances[key];
    },

    // 更新所有图表
    updateCharts() {
      // 数据采样：如果数据量太大，进行采样以提高性能
      const maxPoints = 500;
      const baseArr = this.trendData.timestamps || [];
      let sampleIndices = null;
      if (baseArr.length > maxPoints) {
        const step = Math.ceil(baseArr.length / maxPoints);
        sampleIndices = [];
        for (let i = 0; i < baseArr.length; i += step) {
          sampleIndices.push(i);
        }
      }
      const sampleByIndices = (arr) => {
        if (!arr) return [];
        if (!sampleIndices) return arr;
        return sampleIndices.map(i => (i < arr.length ? arr[i] : null));
      };

      let timestamps = sampleByIndices(this.trendData.timestamps);
      let temp = sampleByIndices(this.trendData.temp);
      let humi = sampleByIndices(this.trendData.humi);
      let soil = sampleByIndices(this.trendData.soil);
      let light = sampleByIndices(this.trendData.light);
      let co2 = sampleByIndices(this.trendData.co2);

      // 根据排序方式处理数据
      if (this.chartSortOrder === 'desc') {
        timestamps = [...timestamps].reverse();
        temp = [...temp].reverse();
        humi = [...humi].reverse();
        soil = [...soil].reverse();
        light = [...light].reverse();
        co2 = [...co2].reverse();
      }

      // 根据分析类型决定图表类型
      const chartType = this.analysisType === 'compare' ? 'bar' : 'line';
      const smooth = this.analysisType !== 'compare';
      const showArea = this.analysisType === 'trend';
      const showSymbol = this.analysisType === 'compare';

      // 统计分析模式下计算 min/max/avg 标记线
      // 注意：不能返回 undefined，否则 ECharts 在清除已有 markLine 时内部调用 mgt.clearMarks 会崩溃
      const emptyMarkLine = { data: [] };
      const buildMarkLine = (data) => {
        if (this.analysisType !== 'statistics' || !data || data.length === 0) return emptyMarkLine;
        const nums = data.filter(v => v != null);
        if (nums.length === 0) return emptyMarkLine;
        const avg = nums.reduce((a, b) => a + b, 0) / nums.length;
        return {
          silent: true,
          lineStyle: { type: 'dashed', width: 1 },
          label: { fontSize: 11 },
          data: [
            { type: 'max', label: { formatter: '最大: {c}' } },
            { type: 'min', label: { formatter: '最小: {c}' } },
            { yAxis: Math.round(avg * 10) / 10, label: { formatter: '平均: ' + (Math.round(avg * 10) / 10) } }
          ]
        };
      };

      const dataLength = timestamps.length;
      const labelInterval = Math.max(0, Math.floor(dataLength / 8) - 1);

      const dataZoom = [
        { type: 'inside', start: 0, end: 100, zoomOnMouseWheel: true, moveOnMouseMove: true },
        { type: 'slider', show: true, realtime: true, start: 0, end: 100, height: 25, bottom: 8, handleSize: '110%', borderColor: '#ddd' }
      ];

      const tooltipStyle = {
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#eee',
        borderWidth: 1,
        textStyle: { color: '#333', fontSize: 13 }
      };
      
      // 更新温湿度图表（销毁重建以避免 ECharts 内部状态问题）
      const tempHumiChart = this.getOrRecreateChart('tempHumi', 'tempHumiChart');
      if (tempHumiChart) {
        tempHumiChart.setOption({
          tooltip: { 
            trigger: 'axis', axisPointer: { type: 'cross' },
            ...tooltipStyle
          },
          legend: { data: ['温度(℃)', '湿度(%)'], top: 5, textStyle: { fontSize: 12 } },
          grid: { left: '3%', right: '4%', bottom: '18%', top: '15%', containLabel: true },
          dataZoom,
          xAxis: { 
            type: 'category', boundaryGap: false, data: timestamps,
            axisLabel: { interval: labelInterval, rotate: 30, fontSize: 11, color: '#666' },
            axisLine: { lineStyle: { color: '#ddd' } }
          },
          yAxis: [
            { type: 'value', name: '温度(℃)', position: 'left',
              axisLabel: { formatter: '{value} °C' },
              splitLine: { lineStyle: { color: '#eee', type: 'dashed' } } },
            { type: 'value', name: '湿度(%)', min: 0, max: 100, position: 'right',
              axisLabel: { formatter: '{value} %' },
              splitLine: { show: false } }
          ],
          series: [
            { name: '温度(℃)', type: chartType, data: temp, yAxisIndex: 0, smooth,
              symbol: showSymbol ? 'circle' : 'none',
              lineStyle: { width: 2 },
              itemStyle: { color: '#f56c6c' },
              areaStyle: showArea ? { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(245, 108, 108, 0.25)' },
                { offset: 1, color: 'rgba(245, 108, 108, 0.02)' }
              ]) } : undefined,
              markLine: buildMarkLine(temp) },
            { name: '湿度(%)', type: chartType, data: humi, yAxisIndex: 1, smooth,
              symbol: showSymbol ? 'circle' : 'none',
              lineStyle: { width: 2 },
              itemStyle: { color: '#409eff' },
              areaStyle: showArea ? { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(64, 158, 255, 0.25)' },
                { offset: 1, color: 'rgba(64, 158, 255, 0.02)' }
              ]) } : undefined,
              markLine: buildMarkLine(humi) }
          ]
        });
      }
      
      // 更新土壤光照图表
      const soilLightChart = this.getOrRecreateChart('soilLight', 'soilLightChart');
      if (soilLightChart) {
        soilLightChart.setOption({
          tooltip: { 
            trigger: 'axis', axisPointer: { type: 'cross' },
            ...tooltipStyle
          },
          legend: { data: ['土壤ADC', '光照强度(lux)'], top: 5, textStyle: { fontSize: 12 } },
          grid: { left: '3%', right: '4%', bottom: '18%', top: '15%', containLabel: true },
          dataZoom,
          xAxis: { 
            type: 'category', boundaryGap: false, data: timestamps,
            axisLabel: { interval: labelInterval, rotate: 30, fontSize: 11, color: '#666' },
            axisLine: { lineStyle: { color: '#ddd' } }
          },
          yAxis: [
            { type: 'value', name: '土壤ADC', position: 'left',
              splitLine: { lineStyle: { color: '#eee', type: 'dashed' } } },
            { type: 'value', name: '光照(lux)', position: 'right',
              splitLine: { show: false } }
          ],
          series: [
            { name: '土壤ADC', type: chartType, data: soil, yAxisIndex: 0, smooth,
              symbol: showSymbol ? 'circle' : 'none',
              lineStyle: { width: 2 },
              itemStyle: { color: '#67c23a' },
              areaStyle: showArea ? { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(103, 194, 58, 0.25)' },
                { offset: 1, color: 'rgba(103, 194, 58, 0.02)' }
              ]) } : undefined,
              markLine: buildMarkLine(soil) },
            { name: '光照强度(lux)', type: chartType, data: light, yAxisIndex: 1, smooth,
              symbol: showSymbol ? 'circle' : 'none',
              lineStyle: { width: 2 },
              itemStyle: { color: '#e6a23c' },
              areaStyle: showArea ? { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(230, 162, 60, 0.25)' },
                { offset: 1, color: 'rgba(230, 162, 60, 0.02)' }
              ]) } : undefined,
              markLine: buildMarkLine(light) }
          ]
        });
      }
      
      // 更新CO2图表
      const co2Chart = this.getOrRecreateChart('co2', 'co2Chart');
      if (co2Chart) {
        co2Chart.setOption({
          tooltip: { 
            trigger: 'axis', axisPointer: { type: 'cross' },
            formatter: '{b}<br/>{a}: {c} ppm',
            ...tooltipStyle
          },
          legend: { data: ['CO₂浓度'], top: 5, textStyle: { fontSize: 12 } },
          grid: { left: '3%', right: '3%', bottom: '18%', top: '15%', containLabel: true },
          dataZoom,
          xAxis: { 
            type: 'category', boundaryGap: false, data: timestamps,
            axisLabel: { interval: labelInterval, rotate: 20, fontSize: 11, color: '#666' },
            axisLine: { lineStyle: { color: '#ddd' } }
          },
          yAxis: { 
            type: 'value', name: 'CO₂(ppm)',
            axisLabel: { formatter: '{value}', fontSize: 11 },
            splitLine: { lineStyle: { color: '#eee', type: 'dashed' } }
          },
          series: [
            { name: 'CO₂浓度', type: chartType, data: co2, smooth,
              symbol: showSymbol ? 'circle' : 'none',
              lineStyle: { width: 2 },
              itemStyle: { color: '#909399' },
              areaStyle: showArea ? { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(144, 147, 153, 0.25)' },
                { offset: 1, color: 'rgba(144, 147, 153, 0.02)' }
              ]) } : undefined,
              markLine: this.analysisType === 'statistics' ? buildMarkLine(co2) : {
                silent: true,
                lineStyle: { color: '#f56c6c', type: 'dashed', width: 2 },
                label: { fontSize: 11, color: '#f56c6c' },
                data: [{ yAxis: 1000, label: { formatter: '警戒线 1000ppm' } }]
              }
            }
          ]
        });
      }
    },
    
    // 图表resize（只对容器可见的图表执行，避免 0 尺寸导致 coordinateSystem 丢失）
    resizeCharts() {
      const refMap = {
        tempHumi: this.$refs.tempHumiChart,
        soilLight: this.$refs.soilLightChart,
        co2: this.$refs.co2Chart
      }
      for (const [key, chart] of Object.entries(this.chartInstances)) {
        const el = refMap[key]
        if (chart && el && el.offsetWidth > 0 && el.offsetHeight > 0) {
          chart.resize()
        }
      }
    },
    
    // 分析类型切换：只重绘图表，不重新请求数据
    handleAnalysisTypeChange() {
      const hasData = this.trendData.timestamps && this.trendData.timestamps.length > 0;
      if (hasData) {
        this.updateCharts();
      }
    },

    // 排序方式切换：重新绘制图表
    handleSortChange() {
      const hasData = this.trendData.timestamps && this.trendData.timestamps.length > 0;
      if (hasData) {
        this.updateCharts();
      }
    },
    
    // 清除所有图表数据（dispose 后重新初始化空图表，避免 clear() 导致内部状态残留）
    clearCharts() {
      for (const key of Object.keys(this.chartInstances)) {
        if (this.chartInstances[key]) {
          this.chartInstances[key].dispose();
          this.chartInstances[key] = null;
        }
      }
      this.initCharts();
    }
  }
};
</script>

<style scoped>
/* ========== 智慧农业主题设计 ========== */
/* Primary: #1a472a (深森林绿) Accent: #3a7d44 (森林绿)
   Secondary: #0f766e (青色) Surface: #f0fdf4 (薄荷绿) */

.analysis-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0fdfa 100%);
  position: relative;
}

.analysis-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 50%;
  height: 50%;
  background: radial-gradient(ellipse at top left, rgba(58, 125, 68, 0.06) 0%, transparent 70%);
  pointer-events: none;
}

/* ========== Page Header ========== */
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
  font-size: 22px;
  font-weight: 700;
  color: #1a472a;
  letter-spacing: -0.02em;
}

.page-header p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
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

.filter-item.filter-info {
  flex: 0;
  min-width: auto;
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

/* 数据点数标签 */
.data-count-tag {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.15), rgba(34, 197, 94, 0.1));
  border: 1px solid rgba(34, 197, 94, 0.2);
  border-radius: 8px;
  color: #166534;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 12px;
  line-height: 1;
  white-space: nowrap;
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
}

.filter-select :deep(.el-select__placeholder) {
  color: rgba(26, 71, 42, 0.5);
  font-size: 13px;
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

  .filter-item.filter-info {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
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

/* ========== Stat Cards ========== */
.stat-card {
  text-align: center;
  margin-bottom: 15px;
  border-radius: 14px;
  border: 1px solid rgba(71, 85, 99, 0.08);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(26, 71, 42, 0.1);
}

.stat-item {
  padding: 14px 10px;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1a472a;
  margin-bottom: 5px;
}

.stat-trend {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.stat-trend.up {
  color: #ef4444;
}

.stat-trend.down {
  color: #22c55e;
}

/* ========== Chart Cards ========== */
.chart-card {
  margin-bottom: 20px;
  border-radius: 16px;
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
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #1a472a;
  letter-spacing: -0.01em;
}

.header-icon {
  width: 26px;
  height: 26px;
  vertical-align: middle;
  filter: drop-shadow(0 2px 4px rgba(26, 71, 42, 0.15));
}

.chart-container {
  width: 100%;
  min-height: 400px;
}

.chart-card :deep(.el-card__body) {
  overflow: visible;
}

/* ========== Tags ========== */
:deep(.el-tag) {
  border: none;
  font-size: 11px;
  font-weight: 500;
  border-radius: 6px;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.9), rgba(220, 252, 231, 0.9));
  color: #166534;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, rgba(220, 252, 231, 0.9), rgba(187, 247, 208, 0.9));
  color: #166534;
}


@media (max-width: 768px) {
  .analysis-page {
    padding: 16px;
  }

  .page-header {
    padding: 18px 20px;
  }

  .page-header h2 {
    font-size: 18px;
  }

  .chart-container {
    min-height: 300px;
  }

  .stat-value {
    font-size: 22px;
  }
}
</style>
