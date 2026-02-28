<template>
  <div class="warning-page">
    <div class="page-header">
      <h2>⚠️ 警告日志记录</h2>
      <p>查看所有环境参数超阈值的警告信息</p>
    </div>

    <!-- 筛选和操作区 -->
    <el-card class="filter-card" shadow="hover">
      <el-row :gutter="15" align="middle">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="filter-item">
            <span class="filter-label">警告类型：</span>
            <el-select v-model="filterType" @change="handleFilter" placeholder="全部类型" style="width: 100%;">
              <el-option label="全部" value="" />
              <el-option label="温度异常" value="temperature" />
              <el-option label="湿度异常" value="humidity" />
              <el-option label="土壤干旱" value="soil" />
              <el-option label="光照不足" value="light" />
              <el-option label="CO₂异常" value="co2" />
            </el-select>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="filter-item">
            <span class="filter-label">状态：</span>
            <el-select v-model="filterStatus" @change="handleFilter" placeholder="全部状态" style="width: 100%;">
              <el-option label="全部" value="" />
              <el-option label="未处理" value="0" />
              <el-option label="已处理" value="1" />
            </el-select>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="filter-item">
            <span class="filter-label">时间范围：</span>
            <el-select v-model="filterTime" @change="handleFilter" placeholder="全部时间" style="width: 100%;">
              <el-option label="全部" value="" />
              <el-option label="今天" value="today" />
              <el-option label="本周" value="week" />
              <el-option label="本月" value="month" />
            </el-select>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-button type="primary" @click="fetchWarningLogs" :loading="loading" style="width: 100%;">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 警告日志表格 -->
    <el-card class="log-card" shadow="hover" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span class="header-title">日志列表（共 {{ totalLogs }} 条）</span>
          <el-button type="text" @click="exportLogs" size="small">导出日志</el-button>
        </div>
      </template>

      <el-table
        :data="logList"
        stripe
        style="width: 100%"
        v-loading="loading"
        :max-height="600"
      >
        <el-table-column prop="id" label="ID" width="80" />
        
        <el-table-column label="警告类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getWarningTypeTag(row.warningType)">
              {{ getWarningTypeText(row.warningType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="sensorId" label="传感器ID" width="120" />

        <el-table-column label="触发值" width="120">
          <template #default="{ row }">
            <span>{{ row.triggerValue }}</span>
            <span class="unit">{{ getUnit(row.warningType) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="阈值" width="100">
          <template #default="{ row }">
            <span>{{ row.threshold }}</span>
            <span class="unit">{{ getUnit(row.warningType) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="triggerTime" label="触发时间" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatTime(row.triggerTime) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-badge :value="row.status === 1 ? '已处理' : '未处理'" 
              :class="row.status === 1 ? 'badge-success' : 'badge-warning'" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 0"
              link 
              type="primary" 
              size="small"
              @click="markAsResolved(row.id)"
            >
              标记已处理
            </el-button>
            <el-button 
              link 
              type="info" 
              size="small"
              @click="showDetails(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="totalLogs"
          @current-change="handleCurrentPageChange"
          @size-change="handlePageSizeChange"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>

    <!-- 空状态 -->
    <div class="empty-state" v-if="!loading && logList.length === 0">
      <div class="empty-icon">✅</div>
      <p class="empty-text">暂无警告日志，环境参数一切正常</p>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailsVisible" title="警告详情" width="500px">
      <div v-if="selectedLog" class="log-details">
        <div class="detail-item">
          <span class="label">警告类型：</span>
          <span class="value">{{ getWarningTypeText(selectedLog.warningType) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">传感器ID：</span>
          <span class="value">{{ selectedLog.sensorId }}</span>
        </div>
        <div class="detail-item">
          <span class="label">触发值：</span>
          <span class="value">{{ selectedLog.triggerValue }} {{ getUnit(selectedLog.warningType) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">阈值：</span>
          <span class="value">{{ selectedLog.threshold }} {{ getUnit(selectedLog.warningType) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">触发时间：</span>
          <span class="value">{{ formatTime(selectedLog.triggerTime) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">状态：</span>
          <span class="value">
            <el-tag :type="selectedLog.status === 1 ? 'success' : 'warning'">
              {{ selectedLog.status === 1 ? '已处理' : '未处理' }}
            </el-tag>
          </span>
        </div>
        <div class="detail-item" v-if="selectedLog.description">
          <span class="label">说明：</span>
          <span class="value">{{ selectedLog.description }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailsVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getWarningLogs, markWarningHandled } from '@/api/warning'

export default {
  name: 'WarningLogs',
  data() {
    return {
      logList: [],
      loading: false,
      currentPage: 1,
      pageSize: 20,
      totalLogs: 0,
      filterType: '',
      filterStatus: '',
      filterTime: '',
      detailsVisible: false,
      selectedLog: null,
      refreshTimer: null
    }
  },
  mounted() {
    this.fetchWarningLogs()
    // 定时刷新（5分钟一次）
    this.refreshTimer = setInterval(() => this.fetchWarningLogs(), 300000)
  },
  beforeUnmount() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
    }
  },
  methods: {
    // 获取警告日志
    async fetchWarningLogs() {
      this.loading = true
      try {
        const params = {
          page: this.currentPage,
          pageSize: this.pageSize
        }
        
        if (this.filterType) params.warningType = this.filterType
        if (this.filterStatus !== '') params.status = this.filterStatus
        if (this.filterTime) params.timeRange = this.filterTime
        
        const res = await getWarningLogs(params)
        
        if (res && res.code === 200) {
          this.logList = res.data?.list || []
          this.totalLogs = res.data?.total || 0
        } else {
          this.$message.error('获取日志失败')
        }
      } catch (err) {
        console.error('[WarningLogs] 获取日志异常:', err)
        this.$message.error('加载日志失败，请检查网络')
      } finally {
        this.loading = false
      }
    },
    
    // 筛选处理
    handleFilter() {
      this.currentPage = 1
      this.fetchWarningLogs()
    },
    
    // 分页大小变更
    handlePageSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchWarningLogs()
    },
    
    // 当前页变更
    handleCurrentPageChange(page) {
      this.currentPage = page
      this.fetchWarningLogs()
    },
    
    // 标记为已处理
    async markAsResolved(logId) {
      this.$confirm('确认标记此警告为已处理？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await markWarningHandled(logId)
          if (res && res.code === 200) {
            this.$message.success('已标记为已处理')
            this.fetchWarningLogs()
          } else {
            this.$message.error(res?.msg || '操作失败')
          }
        } catch (err) {
          this.$message.error('操作失败')
        }
      }).catch(() => {})
    },
    
    // 显示详情
    showDetails(log) {
      this.selectedLog = log
      this.detailsVisible = true
    },
    
    // 导出日志
    exportLogs() {
      const csvContent = this.generateCSV()
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)
      link.setAttribute('href', url)
      link.setAttribute('download', `警告日志_${new Date().getTime()}.csv`)
      link.style.visibility = 'hidden'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      this.$message.success('日志已导出')
    },
    
    // 生成CSV内容
    generateCSV() {
      const headers = ['ID', '警告类型', '传感器ID', '触发值', '阈值', '触发时间', '状态']
      const rows = this.logList.map(log => [
        log.id,
        this.getWarningTypeText(log.warningType),
        log.sensorId,
        log.triggerValue,
        log.threshold,
        this.formatTime(log.triggerTime),
        log.status === 1 ? '已处理' : '未处理'
      ])
      
      let csv = headers.join(',') + '\n'
      rows.forEach(row => {
        csv += row.map(cell => `"${cell}"`).join(',') + '\n'
      })
      
      return csv
    },
    
    // 格式化时间
    formatTime(timeStr) {
      if (!timeStr) return '-'
      const date = new Date(timeStr)
      return date.toLocaleString('zh-CN')
    },
    
    // 获取警告类型文本
    getWarningTypeText(type) {
      const map = {
        'temperature': '温度异常',
        'humidity': '湿度异常',
        'soil': '土壤干旱',
        'light': '光照不足',
        'co2': 'CO₂异常'
      }
      // 兼容旧的中文类型（如"温度高于阈值"）
      if (type && type.includes('温度')) return '温度异常'
      if (type && type.includes('湿度')) return '湿度异常'
      return map[type] || type
    },
    
    // 获取警告类型标签类型
    getWarningTypeTag(type) {
      const map = {
        'temperature': 'danger',
        'humidity': 'warning',
        'soil': 'success',
        'light': 'info',
        'co2': 'primary'
      }
      return map[type] || 'info'
    },
    
    // 获取单位
    getUnit(type) {
      const map = {
        'temperature': '°C',
        'humidity': '%',
        'soil': 'ADC',
        'light': 'lux',
        'co2': 'ppm'
      }
      return map[type] || ''
    }
  }
}
</script>

<style scoped>
.warning-page {
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
.log-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 15px;
}
.log-card {
  background: white;
  border-radius: 10px;
  padding: 18px;
  box-shadow: 0 4px 10px rgba(46, 125, 50, 0.1);
  border-left: 4px solid #ef5350;
}
.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.log-type {
  font-size: 16px;
  font-weight: 600;
  color: #c62828;
}
.log-time {
  font-size: 12px;
  color: #81c784;
}
.log-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.log-body p {
  margin: 0;
  font-size: 13px;
  color: #2e7d32;
}
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #81c784;
}
.icon-empty {
  font-size: 48px;
  margin-bottom: 15px;
  display: inline-block;
}
.loading-state {
  text-align: center;
  padding: 80px 0;
  color: #2e7d32;
}
.spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 3px solid rgba(46, 125, 50, 0.2);
  border-radius: 50%;
  border-top-color: #2e7d32;
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 15px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>