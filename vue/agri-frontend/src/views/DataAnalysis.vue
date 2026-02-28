<template>
  <div class="analysis-page">
    <div class="page-header">
      <h2>📊 数据趋势分析</h2>
      <p>可视化展示环境参数变化趋势与多维度分析</p>
    </div>

    <!-- 时间范围和数据类型选择 -->
    <el-card class="filter-card" shadow="hover">
      <el-row :gutter="15" align="middle">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="filter-item">
            <span class="filter-label">时间范围：</span>
            <el-select v-model="timeRange" @change="fetchAnalysisData" placeholder="选择时间范围" style="width: 100%;">
              <el-option label="全部时间" value="all" />
              <el-option label="最近1小时" value="1h" />
              <el-option label="最近6小时" value="6h" />
              <el-option label="最近12小时" value="12h" />
              <el-option label="最近24小时" value="24h" />
              <el-option label="最近7天" value="7day" />
              <el-option label="最近30天" value="30day" />
              <el-option label="最近90天" value="90day" />
            </el-select>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="filter-item">
            <span class="filter-label">分析类型：</span>
            <el-select v-model="analysisType" @change="fetchAnalysisData" placeholder="选择分析类型" style="width: 100%;">
              <el-option label="趋势分析" value="trend" />
              <el-option label="对比分析" value="compare" />
              <el-option label="统计分析" value="statistics" />
            </el-select>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6" :md="6">
          <div class="filter-item">
            <span class="filter-label">数据点数：</span>
            <el-tag type="success">{{ stats.dataCount || 0 }} 条</el-tag>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6" :md="6">
          <el-button type="primary" @click="fetchAnalysisData" :loading="loading" icon="Refresh">
            刷新数据
          </el-button>
        </el-col>
      </el-row>
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
      chartInstances: {
        tempHumi: null,
        soilLight: null,
        co2: null
      },
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
  computed: {
    timeRangeText() {
      const labels = {
        'all': '全部时间',
        '1h': '最近1小时',
        '6h': '最近6小时',
        '12h': '最近12小时',
        '24h': '最近24小时',
        '7day': '最近7天',
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
      if (!elem) return;
      this.chartInstances.tempHumi = echarts.init(elem);
      this.chartInstances.tempHumi.setOption({
        title: { 
          text: '温湿度变化趋势', 
          left: 'center',
          top: 10,
          textStyle: { fontSize: 16, fontWeight: 'bold', color: '#333' }
        },
        tooltip: { 
          trigger: 'axis',
          axisPointer: { type: 'cross' },
          textStyle: { fontSize: 13 }
        },
        legend: { 
          data: ['温度(℃)', '湿度(%)'], 
          top: 40,
          textStyle: { fontSize: 12 }
        },
        grid: { left: '8%', right: '8%', bottom: '15%', top: '20%', containLabel: true },
        xAxis: { 
          type: 'category', 
          data: [],
          axisLabel: { rotate: 30, fontSize: 11, color: '#666' },
          axisLine: { lineStyle: { color: '#ccc' } }
        },
        yAxis: [
          { type: 'value', name: '温度(℃)', min: 0, max: 50, position: 'left', nameTextStyle: { fontSize: 12, color: '#ff6b6b' }, axisLabel: { fontSize: 11 } },
          { type: 'value', name: '湿度(%)', min: 0, max: 100, position: 'right', nameTextStyle: { fontSize: 12, color: '#4ecdc4' }, axisLabel: { fontSize: 11 } }
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
      if (!elem) return;
      this.chartInstances.soilLight = echarts.init(elem);
      this.chartInstances.soilLight.setOption({
        title: { 
          text: '土壤 & 光照变化趋势', 
          left: 'center',
          top: 10,
          textStyle: { fontSize: 16, fontWeight: 'bold', color: '#333' }
        },
        tooltip: { 
          trigger: 'axis',
          axisPointer: { type: 'cross' },
          textStyle: { fontSize: 13 }
        },
        legend: { 
          data: ['土壤ADC', '光照强度(lux)'], 
          top: 40,
          textStyle: { fontSize: 12 }
        },
        grid: { left: '8%', right: '8%', bottom: '15%', top: '20%', containLabel: true },
        xAxis: { 
          type: 'category', 
          data: [],
          axisLabel: { rotate: 30, fontSize: 11, color: '#666' },
          axisLine: { lineStyle: { color: '#ccc' } }
        },
        yAxis: [
          { type: 'value', name: '土壤ADC', position: 'left', nameTextStyle: { fontSize: 12, color: '#a0522d' }, axisLabel: { fontSize: 11 } },
          { type: 'value', name: '光照(lux)', position: 'right', nameTextStyle: { fontSize: 12, color: '#ffd93d' }, axisLabel: { fontSize: 11 } }
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
      if (!elem) return;
      this.chartInstances.co2 = echarts.init(elem);
      this.chartInstances.co2.setOption({
        title: { 
          text: 'CO₂ 浓度变化趋势', 
          left: 'center',
          top: 10,
          textStyle: { fontSize: 16, fontWeight: 'bold', color: '#333' }
        },
          tooltip: { 
            trigger: 'axis',
            axisPointer: { type: 'cross' },
            formatter: '{b}<br/>CO₂: {c} ppm',
            textStyle: { fontSize: 13 }
          },
        legend: {
          data: ['CO₂浓度'],
          top: 40,
          textStyle: { fontSize: 12 }
        },
        grid: { left: '8%', right: '8%', bottom: '15%', top: '18%', containLabel: true },
        xAxis: { 
          type: 'category', 
          data: [],
          axisLabel: { rotate: 30, fontSize: 11, color: '#666' },
          axisLine: { lineStyle: { color: '#ccc' } }
        },
        yAxis: { 
          type: 'value', 
          name: 'CO₂(ppm)',
          nameTextStyle: { fontSize: 12, color: '#6c5ce7' },
          axisLabel: { fontSize: 11 }
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
          this.updateCharts();
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
    
    // 更新所有图表
    updateCharts() {
      // 数据采样：如果数据量太大，进行采样以提高性能
      const maxPoints = 100;
      const sampleData = (arr) => {
        if (!arr || arr.length <= maxPoints) return arr;
        const step = Math.ceil(arr.length / maxPoints);
        return arr.filter((_, i) => i % step === 0);
      };
      
      const timestamps = sampleData(this.trendData.timestamps);
      const temp = sampleData(this.trendData.temp);
      const humi = sampleData(this.trendData.humi);
      const soil = sampleData(this.trendData.soil);
      const light = sampleData(this.trendData.light);
      const co2 = sampleData(this.trendData.co2);
      
      // 更新温湿度图表
      if (this.chartInstances.tempHumi) {
        this.chartInstances.tempHumi.setOption({
          xAxis: { data: timestamps },
          series: [
            { data: temp },
            { data: humi }
          ]
        });
      }
      
      // 更新土壤光照图表
      if (this.chartInstances.soilLight) {
        this.chartInstances.soilLight.setOption({
          xAxis: { data: timestamps },
          series: [
            { data: soil },
            { data: light }
          ]
        });
      }
      
      // 更新CO2图表
      if (this.chartInstances.co2) {
        this.chartInstances.co2.setOption({
          xAxis: { data: timestamps },
          series: [{ data: co2 }]
        });
      }
    },
    
    // 图表resize
    resizeCharts() {
      Object.values(this.chartInstances).forEach(chart => {
        if (chart) chart.resize();
      });
    }
  }
};
</script>

<style scoped>
.analysis-page {
  padding: 20px;
}
.page-header {
  margin-bottom: 25px;
}
.page-header h2 {
  color: #1b5e20;
  margin: 0 0 8px;
  font-size: 22px;
}
.page-header p {
  color: #558b2f;
  margin: 0;
  font-size: 14px;
}
.filter-card {
  margin-bottom: 20px;
}
.filter-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.filter-label {
  font-size: 14px;
  color: #606266;
  margin-right: 10px;
  white-space: nowrap;
}
.stat-card {
  text-align: center;
  margin-bottom: 15px;
}
.stat-item {
  padding: 10px 0;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}
.stat-trend {
  font-size: 12px;
  color: #909399;
}
.stat-trend.up {
  color: #f56c6c;
}
.stat-trend.down {
  color: #67c23a;
}
.chart-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.header-icon {
  width: 24px;
  height: 24px;
  vertical-align: middle;
}
.chart-container {
  width: 100%;
  min-height: 400px;
}
</style>
