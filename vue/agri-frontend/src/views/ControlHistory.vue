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
      <el-row :gutter="15" align="middle">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="filter-item">
            <span class="filter-label">控制类型：</span>
            <el-select v-model="filterType" @change="handleFilter" placeholder="全部类型" clearable style="width: 100%;">
              <el-option label="全部" value="" />
              <el-option label="水泵" value="pump" />
              <el-option label="风扇" value="fan" />
              <el-option label="照明" value="light" />
              <el-option label="模式切换" value="mode" />
              <el-option label="阈值设置" value="threshold" />
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
          <div class="filter-item">
            <span class="filter-label">操作者：</span>
            <el-input v-model="filterOperator" placeholder="搜索操作者" clearable @clear="handleFilter" @keyup.enter="handleFilter" style="width: 100%;" />
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-button type="primary" @click="fetchData" :loading="loading" style="width: 100%;">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </el-col>
      </el-row>
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
.control-history-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 5px 0;
  color: #303133;
}

.subtitle {
  color: #909399;
  margin: 0;
  font-size: 14px;
}

.stat-card {
  text-align: center;
}

.stat-item .stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-item .stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-item .stat-value.success {
  color: #67c23a;
}

.filter-card {
  margin-bottom: 0;
}

.filter-item {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.filter-label {
  white-space: nowrap;
  font-size: 14px;
  color: #606266;
  margin-right: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-weight: 600;
  font-size: 16px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 15px;
}
</style>
