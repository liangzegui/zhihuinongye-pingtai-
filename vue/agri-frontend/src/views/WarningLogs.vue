<template>
  <div class="warning-page">
    <div class="page-header">
      <h2>⚠️ 警告日志记录</h2>
      <p>查看所有环境参数超阈值的警告信息</p>
    </div>

    <!-- 筛选和操作区 -->
    <el-card class="filter-card" shadow="hover">
      <div class="filter-container">
        <div class="filter-row">
          <div class="filter-item">
            <span class="filter-label">警告类型</span>
            <el-select v-model="filterType" @change="handleFilter" placeholder="选择警告类型" class="filter-select">
              <el-option label="全部" value="" />
              <el-option label="温度异常" value="temperature" />
              <el-option label="湿度异常" value="humidity" />
              <el-option label="土壤干旱" value="soil" />
              <el-option label="光照不足" value="light" />
              <el-option label="CO₂异常" value="co2" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">状态</span>
            <el-select v-model="filterStatus" @change="handleFilter" placeholder="选择状态" class="filter-select">
              <el-option label="全部" value="" />
              <el-option label="未处理" value="0" />
              <el-option label="已处理" value="1" />
            </el-select>
          </div>
          <div class="filter-item">
            <span class="filter-label">时间范围</span>
            <el-select v-model="filterTime" @change="handleFilter" placeholder="选择时间范围" class="filter-select">
              <el-option label="全部" value="" />
              <el-option label="今天" value="today" />
              <el-option label="本周" value="week" />
              <el-option label="本月" value="month" />
            </el-select>
          </div>
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="fetchWarningLogs" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
          <el-button type="text" @click="exportLogs" size="small">导出日志</el-button>
        </div>
      </div>
    </el-card>

    <!-- 批量操作栏 -->
    <el-card v-if="selectedIds.length > 0" class="batch-bar" shadow="hover" style="margin-top: 12px;">
      <div class="batch-actions">
        <span class="batch-info">已选中 <strong>{{ selectedIds.length }}</strong> 条记录</span>
        <el-button type="warning" size="small" @click="batchHandle" :loading="batchLoading">
          <el-icon><Check /></el-icon> 批量标记已处理
        </el-button>
        <el-button size="small" @click="clearSelection">取消选择</el-button>
      </div>
    </el-card>

    <!-- 警告日志表格 -->
    <el-card class="log-card" shadow="hover" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span class="header-title">日志列表（共 {{ totalLogs }} 条）</span>
        </div>
      </template>

      <el-table
        ref="logTable"
        :data="logList"
        stripe
        style="width: 100%"
        v-loading="loading"
        :max-height="600"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
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

        <el-table-column label="操作" width="200" fixed="right">
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
import { getWarningLogs, markWarningHandled, batchMarkHandled } from '@/api/warning'
import { Check, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'WarningLogs',
  components: { Check, Refresh },
  data() {
    return {
      logList: [],
      loading: false,
      batchLoading: false,
      currentPage: 1,
      pageSize: 20,
      totalLogs: 0,
      filterType: '',
      filterStatus: '',
      filterTime: '',
      detailsVisible: false,
      selectedLog: null,
      selectedIds: [],
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

    // ==================== 多选操作 ====================

    // 多选变化
    handleSelectionChange(selection) {
      this.selectedIds = selection.map(row => row.id)
    },

    // 清除选择
    clearSelection() {
      this.$refs.logTable.clearSelection()
      this.selectedIds = []
    },

    // 批量标记已处理
    async batchHandle() {
      const unhandledIds = this.selectedIds.filter(id => {
        const row = this.logList.find(r => r.id === id)
        return row && row.status === 0
      })
      if (unhandledIds.length === 0) {
        this.$message.warning('所选记录均已处理')
        return
      }
      try {
        await this.$confirm(`确认批量标记 ${unhandledIds.length} 条记录为已处理？`, '批量处理', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        this.batchLoading = true
        const res = await batchMarkHandled(unhandledIds)
        if (res && res.code === 200) {
          this.$message.success(`成功处理 ${res.data?.handledCount || unhandledIds.length} 条记录`)
          this.clearSelection()
          this.fetchWarningLogs()
        } else {
          this.$message.error(res?.msg || '批量处理失败')
        }
      } catch (err) {
        if (err !== 'cancel') this.$message.error('操作失败')
      } finally {
        this.batchLoading = false
      }
    },
    
    // ==================== 单条操作 ====================

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
/* ========== 智慧农业主题设计 ========== */
/* Primary: #1a472a (深森林绿) Accent: #3a7d44 (森林绿)
   Secondary: #0f766e (青色) Surface: #f0fdf4 (薄荷绿) */

.warning-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0fdfa 100%);
  position: relative;
}

.warning-page::before {
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
  background: linear-gradient(90deg, #ea580c, #f97316, #fb923c);
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

  .filter-label {
    min-width: unset;
  }

  .filter-select {
    width: 100%;
  }

  .filter-actions {
    width: 100%;
    justify-content: center;
  }

  .filter-actions .el-button {
    flex: 1;
  }
}

/* ========== Batch Bar ========== */
.batch-bar {
  background: linear-gradient(135deg, rgba(254, 243, 199, 0.9), rgba(254, 249, 195, 0.9));
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-left: 4px solid #f59e0b;
  border-radius: 12px;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.batch-info {
  color: #b45309;
  font-size: 14px;
  font-weight: 500;
  margin-right: 8px;
}

/* ========== Log Card ========== */
.log-card {
  border-radius: 16px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
}

.log-card :deep(.el-card__header) {
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
  font-size: 15px;
  font-weight: 600;
  color: #1a472a;
}

/* ========== Table Styles ========== */
:deep(.el-table) {
  font-size: 13px;
  --el-table-header-bg-color: #f8faf8;
  --el-table-row-hover-bg-color: #f0fdf4;
}

:deep(.el-table th.el-table__cell) {
  background: linear-gradient(180deg, #f0fdf4 0%, #f8faf8 100%);
  color: #1a472a;
  font-weight: 600;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(240, 253, 244, 0.4);
}

.unit {
  color: #64748b;
  font-size: 12px;
  margin-left: 2px;
}

/* ========== Pagination ========== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #1a472a, #3a7d44);
  border-radius: 6px;
}

/* ========== Empty State ========== */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #64748b;
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 15px;
  color: #64748b;
}

/* ========== Badges ========== */
.badge-success :deep(.el-badge__content) {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}

.badge-warning :deep(.el-badge__content) {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

/* ========== Dialog Details ========== */
.log-details {
  padding: 10px 0;
}

.detail-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid rgba(71, 85, 99, 0.08);
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item .label {
  width: 100px;
  color: #64748b;
  font-size: 14px;
  flex-shrink: 0;
}

.detail-item .value {
  color: #1a472a;
  font-size: 14px;
  font-weight: 500;
}

/* ========== Select ========== */
/* 这些样式已经在filter部分统一处理了 */

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .warning-page {
    padding: 16px;
  }

  .page-header {
    padding: 18px 20px;
  }

  .page-header h2 {
    font-size: 18px;
  }
}
</style>