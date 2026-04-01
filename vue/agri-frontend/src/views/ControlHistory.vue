<template>
  <div class="control-history-page">
    <div class="page-header">
      <h2><el-icon><List /></el-icon> 设备控制记录</h2>
      <p class="subtitle">查看设备操作历史记录与控制审计日志</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="15" style="margin-bottom: 20px;">
      <el-col :xs="8" :sm="8" :md="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-label">总操作次数</div>
            <div class="stat-value">{{ stats.totalCount || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="8" :sm="8" :md="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-label">今日操作</div>
            <div class="stat-value">{{ stats.todayCount || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="8" :sm="8" :md="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-label">成功率</div>
            <div class="stat-value success">{{ stats.successRate || 100 }}%</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="hover">
      <div class="filter-container">
        <div class="filter-row">
          <div class="filter-item">
            <span class="filter-label">控制类型</span>
            <el-select v-model="filterType" @change="handleFilter" placeholder="选择控制类型" clearable class="filter-select">
              <el-option label="全部" value="" />
              <el-option label="水泵" value="pump" />
              <el-option label="风扇" value="fan" />
              <el-option label="照明" value="light" />
              <el-option label="模式切换" value="mode" />
              <el-option label="阈值设置" value="threshold" />
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
          <div class="filter-item filter-input">
            <span class="filter-label">操作者</span>
            <el-input
              v-model="filterOperator"
              placeholder="搜索操作者"
              clearable
              @clear="handleFilter"
              @keyup.enter="handleFilter"
              class="filter-input-control"
            />
          </div>
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="fetchData" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 控制记录表格 -->
    <el-card class="table-card" shadow="hover" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span class="header-title">操作记录（共 {{ total }} 条）</span>
        </div>
      </template>

      <el-table :data="historyList" stripe v-loading="loading" :max-height="500" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="80" />

        <el-table-column label="控制类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.controlType)" size="small">
              {{ getTypeText(row.controlType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="controlValue" label="控制值" width="150" show-overflow-tooltip />

        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="row.controlSource === 'auto' ? 'success' : 'warning'" size="small" effect="plain">
              {{ row.controlSource === 'auto' ? '自动' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="operator" label="操作者" width="120">
          <template #default="{ row }">
            {{ row.operator || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="执行结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.result === 'success' ? 'success' : 'danger'" size="small">
              {{ row.result === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="deviceId" label="设备ID" width="150" show-overflow-tooltip />

        <el-table-column label="操作时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          :current-page="page"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { getControlHistory, getControlStats } from '@/api/controlHistory'
import { ElMessage } from 'element-plus'
import { List, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'ControlHistory',
  components: { List, Refresh },
  data() {
    return {
      historyList: [],
      page: 1,
      pageSize: 20,
      total: 0,
      loading: false,
      filterType: '',
      filterTime: '',
      filterOperator: '',
      stats: {}
    }
  },
  mounted() {
    this.fetchData()
    this.fetchStats()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await getControlHistory({
          page: this.page,
          pageSize: this.pageSize,
          controlType: this.filterType || undefined,
          operator: this.filterOperator || undefined,
          timeRange: this.filterTime || undefined
        })
        const data = res.data || res || {}
        this.historyList = data.list || []
        this.total = data.total || 0
      } catch (err) {
        console.error('获取控制记录失败:', err)
        ElMessage.error('获取控制记录失败')
      } finally {
        this.loading = false
      }
    },

    async fetchStats() {
      try {
        const res = await getControlStats()
        this.stats = res.data || res || {}
      } catch (err) {
        console.error('获取统计失败:', err)
      }
    },

    handleFilter() {
      this.page = 1
      this.fetchData()
    },

    handleSizeChange(val) {
      this.pageSize = val
      this.page = 1
      this.fetchData()
    },

    handlePageChange(val) {
      this.page = val
      this.fetchData()
    },

    getTypeText(type) {
      const map = { pump: '水泵', fan: '风扇', light: '照明', mode: '模式切换', threshold: '阈值设置' }
      return map[type] || type || '-'
    },

    getTypeTag(type) {
      const map = { pump: 'primary', fan: 'success', light: 'warning', mode: 'info', threshold: '' }
      return map[type] || 'info'
    },

    formatTime(timeStr) {
      if (!timeStr) return '-'
      try {
        const d = new Date(timeStr)
        if (isNaN(d.getTime())) return timeStr
        return d.toLocaleString('zh-CN', { hour12: false })
      } catch {
        return timeStr
      }
    }
  }
}
</script>

<style scoped>
/* ========== 智慧农业主题设计 ========== */
/* Primary: #1a472a (深森林绿) Accent: #3a7d44 (森林绿)
   Secondary: #0f766e (青色) Surface: #f0fdf4 (薄荷绿) */

.control-history-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0fdfa 100%);
  position: relative;
}

.control-history-page::before {
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
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 6px 0;
  font-size: 22px;
  font-weight: 700;
  color: #1a472a;
  letter-spacing: -0.02em;
}

.subtitle {
  color: #64748b;
  margin: 0;
  font-size: 14px;
}

/* ========== Stat Cards ========== */
.stat-card {
  text-align: center;
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

.stat-item .stat-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 10px;
}

.stat-item .stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #1a472a;
}

.stat-item .stat-value.success {
  color: #22c55e;
}

/* ========== Filter Card ========== */
.filter-card {
  margin-bottom: 0;
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

.filter-item.filter-input {
  flex: 1.5;
  min-width: 250px;
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

.filter-input-control {
  width: 200px;
}

.filter-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 统一筛选框样式 */
.filter-select :deep(.el-input__wrapper),
.filter-input-control :deep(.el-input__wrapper) {
  border-radius: 8px;
  border: 1px solid rgba(58, 125, 68, 0.2);
  background: #fff;
  transition: all 0.2s ease;
}

.filter-select :deep(.el-input__wrapper:hover),
.filter-input-control :deep(.el-input__wrapper:hover) {
  border-color: rgba(58, 125, 68, 0.4);
  box-shadow: 0 2px 8px rgba(58, 125, 68, 0.1);
}

.filter-select :deep(.el-input.is-focus .el-input__wrapper),
.filter-input-control :deep(.el-input.is-focus .el-input__wrapper) {
  border-color: #3a7d44;
  box-shadow: 0 0 0 2px rgba(58, 125, 68, 0.1);
}

.filter-select :deep(.el-input__inner),
.filter-input-control :deep(.el-input__inner) {
  color: #1a472a;
  font-size: 13px;
  font-weight: 500;
}

.filter-select :deep(.el-select__placeholder),
.filter-input-control :deep(.el-input__placeholder-inner) {
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

  .filter-item.filter-input {
    min-width: 220px;
  }

  .filter-select {
    width: 140px;
  }

  .filter-input-control {
    width: 180px;
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

  .filter-select,
  .filter-input-control {
    width: 100%;
  }

  .filter-actions {
    width: 100%;
  }

  .filter-actions .el-button {
    flex: 1;
  }
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-weight: 600;
  font-size: 15px;
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

/* ========== Pagination ========== */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #1a472a, #3a7d44);
  border-radius: 6px;
}

/* ========== Select & Input ========== */
/* 这些样式已经在filter部分统一处理了 */

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .control-history-page {
    padding: 16px;
  }

  .page-header {
    padding: 18px 20px;
  }

  .page-header h2 {
    font-size: 18px;
  }

  .stat-item .stat-value {
    font-size: 24px;
  }
}
</style>
