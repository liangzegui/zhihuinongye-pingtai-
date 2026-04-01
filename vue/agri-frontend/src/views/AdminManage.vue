<template>
  <div class="admin-page">
    <!-- 升级后的页面头部 -->
    <div class="page-header admin-header">
      <div class="header-left">
        <h2><el-icon><Setting /></el-icon> 管理员中心</h2>
        <p>管理用户账号、数据保存策略与系统概览</p>
      </div>
      <div class="header-right">
        <el-tag type="success" effect="dark" size="large">
          <el-icon><User /></el-icon> {{ currentUsername }}
        </el-tag>
      </div>
    </div>

    <!-- 统计概览卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon" style="background: #e8f5e9; color: #2e7d32;"><el-icon :size="22"><User /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.userCount ?? '-' }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon" style="background: #fff3e0; color: #e65100;"><el-icon :size="22"><Key /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.adminCount ?? '-' }}</div>
              <div class="stat-label">管理员数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon" style="background: #e3f2fd; color: #1565c0;"><el-icon :size="22"><DataLine /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalDataCount ?? '-' }}</div>
              <div class="stat-label">数据总量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-item">
            <div class="stat-icon" style="background: #fce4ec; color: #c62828;"><el-icon :size="22"><Calendar /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayDataCount ?? '-' }}</div>
              <div class="stat-label">今日新增</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="user-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div class="header-actions">
            <div class="admin-search-container">
              <el-input v-model.trim="searchKeyword" placeholder="按用户名搜索" clearable class="admin-search-input" />
            </div>
            <el-button type="primary" @click="openCreateDialog">新增用户</el-button>
          </div>
        </div>
      </template>

      <el-table :data="pagedUsers" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="160" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'admin' ? 'danger' : 'info'">
              {{ scope.row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="warning" @click="handleResetPassword(scope.row)">重置密码</el-button>
            <el-tooltip
              v-if="scope.row.username === currentUsername"
              content="不能删除当前登录的账号"
              placement="top"
            >
              <el-button size="small" type="info" disabled>删除</el-button>
            </el-tooltip>
            <el-button v-else size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="filteredUsers.length"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <DataAutoSaveConfig />

    <el-card class="user-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>保存数据管理</span>
          <div class="header-actions">
            <el-button @click="loadEnvData">刷新</el-button>
            <el-button type="danger" :disabled="selectedEnvIds.length === 0" @click="handleBatchDeleteEnv">批量删除</el-button>
            <el-button type="primary" @click="openCreateEnvDialog">新增记录</el-button>
          </div>
        </div>
      </template>

      <!-- 筛选区域 -->
      <div class="env-filters">
        <div class="filter-item">
          <span class="filter-label">日期范围：</span>
          <el-date-picker
            v-model="envDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            clearable
            @change="handleEnvFilterChange"
            style="width: 280px;"
          />
        </div>
        <div class="filter-item">
          <span class="filter-label">保存人：</span>
          <el-select
            v-model="envSaverFilter"
            placeholder="全部"
            clearable
            @change="handleEnvFilterChange"
            style="width: 160px;"
          >
            <el-option
              v-for="name in saverNames"
              :key="name"
              :label="name"
              :value="name"
            />
          </el-select>
        </div>
      </div>

      <el-table :data="envRecords" v-loading="envLoading" border row-key="id" @selection-change="handleEnvSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="保存人" width="160">
          <template #default="scope">
            <el-tag :type="getSaverTagType(scope.row)" size="small">
              {{ getSaverUsername(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="温度" width="110">
          <template #default="scope">
            <span :class="getTempClass(scope.row.temperature)">
              {{ scope.row.temperature != null ? scope.row.temperature + ' °C' : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="湿度" width="110">
          <template #default="scope">
            <span :class="getHumidityClass(scope.row.humidity)">
              {{ scope.row.humidity != null ? scope.row.humidity + ' %' : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="土壤ADC" width="110">
          <template #default="scope">
            {{ scope.row.soilAdc ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="光照" width="120">
          <template #default="scope">
            {{ scope.row.lightIntensity != null ? scope.row.lightIntensity + ' lux' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="CO₂" width="120">
          <template #default="scope">
            <span :class="getCo2Class(scope.row.co2)">
              {{ scope.row.co2 != null ? scope.row.co2 + ' ppm' : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="collectTime" label="采集时间" min-width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEditEnvDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDeleteEnv(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          :current-page="envPage"
          :page-size="envSize"
          :total="envTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleEnvPageChange"
          @size-change="handleEnvSizeChange"
        />
      </div>
    </el-card>

    <!-- ==================== 警告日志管理 ==================== -->
    <el-card class="user-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>⚠️ 警告日志管理</span>
          <div class="header-actions">
            <div class="admin-filter-group">
              <el-select v-model="warnFilterType" @change="handleWarnFilter" placeholder="警告类型" clearable class="admin-filter-select">
                <el-option label="全部" value="" />
                <el-option label="温度异常" value="temperature" />
                <el-option label="湿度异常" value="humidity" />
                <el-option label="土壤干旱" value="soil" />
                <el-option label="光照不足" value="light" />
                <el-option label="CO₂异常" value="co2" />
              </el-select>
              <el-select v-model="warnFilterStatus" @change="handleWarnFilter" placeholder="状态" clearable class="admin-filter-select">
                <el-option label="全部" value="" />
                <el-option label="未处理" value="0" />
                <el-option label="已处理" value="1" />
              </el-select>
            </div>
            <div class="admin-action-group">
              <el-button size="small" @click="loadWarningLogs">刷新</el-button>
              <el-button type="warning" size="small" :disabled="selectedWarnIds.length === 0" @click="handleBatchMarkHandled">
                批量处理
              </el-button>
              <el-button type="danger" size="small" :disabled="selectedWarnIds.length === 0" @click="handleBatchDeleteWarn">
                批量删除
              </el-button>
              <el-button type="danger" size="small" plain @click="handleClearHandledWarn">
                清空已处理
              </el-button>
            </div>
          </div>
        </div>
      </template>

      <el-table :data="warnRecords" v-loading="warnLoading" border row-key="id" @selection-change="handleWarnSelectionChange" :max-height="500">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="警告类型" width="110">
          <template #default="{ row }">
            <el-tag :type="getWarnTypeTag(row.warningType)" size="small">{{ getWarnTypeText(row.warningType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sensorId" label="传感器" width="90" />
        <el-table-column label="触发值" width="100">
          <template #default="{ row }">
            {{ row.triggerValue }} {{ getWarnUnit(row.warningType) }}
          </template>
        </el-table-column>
        <el-table-column label="阈值" width="100">
          <template #default="{ row }">
            {{ row.threshold }} {{ getWarnUnit(row.warningType) }}
          </template>
        </el-table-column>
        <el-table-column prop="triggerTime" label="触发时间" min-width="170" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
              {{ row.status === 1 ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="primary" size="small" @click="handleMarkOneHandled(row.id)">处理</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteOneWarn(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          :current-page="warnPage"
          :page-size="warnSize"
          :total="warnTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleWarnPageChange"
          @size-change="handleWarnSizeChange"
        />
      </div>
    </el-card>

    <!-- ==================== 控制记录管理 ==================== -->
    <el-card class="user-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🎛️ 控制记录管理</span>
          <div class="header-actions">
            <div class="admin-filter-group">
              <el-select v-model="ctrlFilterType" @change="handleCtrlFilter" placeholder="控制类型" clearable class="admin-filter-select">
                <el-option label="全部" value="" />
                <el-option label="水泵" value="pump" />
                <el-option label="风扇" value="fan" />
                <el-option label="照明" value="light" />
                <el-option label="模式切换" value="mode" />
                <el-option label="阈值设置" value="threshold" />
              </el-select>
            </div>
            <div class="admin-action-group">
              <el-button size="small" @click="loadControlHistory">刷新</el-button>
              <el-button type="danger" size="small" :disabled="selectedCtrlIds.length === 0" @click="handleBatchDeleteCtrl">
                批量删除
              </el-button>
              <el-button type="danger" size="small" plain @click="handleClearAllCtrl">
                清空全部
              </el-button>
            </div>
          </div>
        </div>
      </template>

      <el-table :data="ctrlRecords" v-loading="ctrlLoading" border row-key="id" @selection-change="handleCtrlSelectionChange" :max-height="500">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="控制类型" width="110">
          <template #default="{ row }">
            <el-tag :type="getCtrlTypeTag(row.controlType)" size="small">{{ getCtrlTypeText(row.controlType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="controlValue" label="控制值" width="130" show-overflow-tooltip />
        <el-table-column label="来源" width="80">
          <template #default="{ row }">
            <el-tag :type="row.controlSource === 'auto' ? 'success' : 'warning'" size="small" effect="plain">
              {{ row.controlSource === 'auto' ? '自动' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作者" width="100">
          <template #default="{ row }">{{ row.operator || '-' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.result === 'success' ? 'success' : 'danger'" size="small">
              {{ row.result === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" min-width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="handleDeleteOneCtrl(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          :current-page="ctrlPage"
          :page-size="ctrlSize"
          :total="ctrlTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCtrlPageChange"
          @size-change="handleCtrlSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增用户' : '编辑用户'" width="420px">
      <el-form :model="form" :rules="userFormRules" ref="userFormRef" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item :label="dialogMode === 'create' ? '密码' : '新密码'" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="dialogMode === 'create' ? '请输入密码(6-32位)' : '不修改可留空'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="envDialogVisible" :title="envDialogMode === 'create' ? '新增保存数据' : '编辑保存数据'" width="520px">
      <el-form :model="envForm" :rules="envFormRules" ref="envFormRef" label-width="110px">
        <el-form-item label="传感器ID" prop="sensorId">
          <el-input-number v-model="envForm.sensorId" :min="1" />
        </el-form-item>
        <el-form-item label="温度 (°C)" prop="temperature">
          <el-input-number v-model="envForm.temperature" :precision="2" :min="-40" :max="80" />
        </el-form-item>
        <el-form-item label="湿度 (%)" prop="humidity">
          <el-input-number v-model="envForm.humidity" :precision="2" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="土壤湿度(%)" prop="soilMoisture">
          <el-input-number v-model="envForm.soilMoisture" :precision="2" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="土壤ADC" prop="soilAdc">
          <el-input-number v-model="envForm.soilAdc" :min="0" :max="4095" />
        </el-form-item>
        <el-form-item label="光照 (lux)" prop="lightIntensity">
          <el-input-number v-model="envForm.lightIntensity" :min="0" :max="200000" />
        </el-form-item>
        <el-form-item label="CO₂ (ppm)" prop="co2">
          <el-input-number v-model="envForm.co2" :min="0" :max="10000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="envDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="envSaving" @click="submitEnvForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminUsers,
  createAdminUser,
  updateAdminUser,
  deleteAdminUser,
  getAdminEnvData,
  createAdminEnvData,
  updateAdminEnvData,
  deleteAdminEnvData,
  batchDeleteAdminEnvData,
  getAdminStats
} from '@/api/admin'
import { getWarningLogs, markWarningHandled, batchMarkHandled, batchDeleteWarnings, clearHandledWarnings } from '@/api/warning'
import { getControlHistory, batchDeleteControlHistory, clearAllControlHistory } from '@/api/controlHistory'
import DataAutoSaveConfig from '@/components/DataAutoSaveConfig.vue'
import { getUsername } from '@/utils/token'
import { Setting, User, Key, DataLine, Calendar } from '@element-plus/icons-vue'

export default {
  name: 'AdminManage',
  components: { DataAutoSaveConfig, Setting, User, Key, DataLine, Calendar },
  data() {
    // 密码校验器：新增时必填，编辑时可选
    const validatePassword = (rule, value, callback) => {
      if (this.dialogMode === 'create' && !value) {
        callback(new Error('请输入密码'))
      } else if (value && (value.length < 6 || value.length > 32)) {
        callback(new Error('密码长度需为6-32位'))
      } else {
        callback()
      }
    }

    return {
      // 当前登录用户名（用于禁止删除自己）
      currentUsername: getUsername() || '管理员',

      // 统计概览
      stats: {
        userCount: null,
        adminCount: null,
        totalDataCount: null,
        todayDataCount: null,
        autoSaveEnabled: false,
        autoSaveInterval: 0
      },
      saverNames: [],

      // 用户管理
      loading: false,
      saving: false,
      users: [],
      searchKeyword: '',
      currentPage: 1,
      pageSize: 10,
      dialogVisible: false,
      dialogMode: 'create',
      currentId: null,
      form: {
        username: '',
        role: 'user',
        password: ''
      },
      userFormRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 2, max: 20, message: '用户名长度为2-20位', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ],
        password: [
          { validator: validatePassword, trigger: 'blur' }
        ]
      },

      // 保存数据管理
      envLoading: false,
      envSaving: false,
      envRecords: [],
      envPage: 1,
      envSize: 10,
      envTotal: 0,
      envDialogVisible: false,
      envDialogMode: 'create',
      currentEnvId: null,
      selectedEnvIds: [],
      envDateRange: null,
      envSaverFilter: '',
      envForm: {
        sensorId: 1,
        temperature: null,
        humidity: null,
        soilMoisture: null,
        soilAdc: null,
        lightIntensity: null,
        co2: null
      },
      envFormRules: {
        sensorId: [
          { required: true, message: '请输入传感器ID', trigger: 'blur' }
        ],
        temperature: [
          { required: true, message: '请输入温度', trigger: 'blur' }
        ],
        humidity: [
          { required: true, message: '请输入湿度', trigger: 'blur' }
        ]
      },

      // 警告日志管理
      warnLoading: false,
      warnRecords: [],
      warnPage: 1,
      warnSize: 10,
      warnTotal: 0,
      warnFilterType: '',
      warnFilterStatus: '',
      selectedWarnIds: [],

      // 控制记录管理
      ctrlLoading: false,
      ctrlRecords: [],
      ctrlPage: 1,
      ctrlSize: 10,
      ctrlTotal: 0,
      ctrlFilterType: '',
      selectedCtrlIds: []
    }
  },
  mounted() {
    this.loadStats()
    this.loadUsers()
    this.loadEnvData()
    this.loadWarningLogs()
    this.loadControlHistory()
  },
  computed: {
    filteredUsers() {
      const keyword = this.searchKeyword?.toLowerCase() || ''
      if (!keyword) {
        return this.users
      }
      return this.users.filter(item => (item.username || '').toLowerCase().includes(keyword))
    },
    pagedUsers() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredUsers.slice(start, end)
    }
  },
  watch: {
    searchKeyword() {
      this.currentPage = 1
    }
  },
  methods: {
    // ========== 统计概览 ==========
    async loadStats() {
      try {
        const res = await getAdminStats()
        if (res.code === 200 && res.data) {
          this.stats = {
            userCount: res.data.userCount ?? 0,
            adminCount: res.data.adminCount ?? 0,
            totalDataCount: res.data.totalDataCount ?? 0,
            todayDataCount: res.data.todayDataCount ?? 0,
            autoSaveEnabled: res.data.autoSaveEnabled ?? false,
            autoSaveInterval: res.data.autoSaveInterval ?? 0
          }
          this.saverNames = res.data.saverNames || []
        }
      } catch (e) {
        console.warn('加载统计数据失败', e)
      }
    },

    // ========== 条件着色辅助方法 ==========
    getTempClass(val) {
      if (val == null) return ''
      if (val > 35) return 'val-danger'
      if (val < 10) return 'val-cold'
      return 'val-normal'
    },
    getHumidityClass(val) {
      if (val == null) return ''
      if (val > 85) return 'val-danger'
      if (val < 20) return 'val-warning'
      return 'val-normal'
    },
    getCo2Class(val) {
      if (val == null) return ''
      if (val > 1000) return 'val-danger'
      if (val > 800) return 'val-warning'
      return 'val-normal'
    },
    getSaverTagType(row) {
      const name = row?.saveUsername || ''
      if (name.includes('自动保存')) return 'warning'
      if (!name) return 'info'
      return 'success'
    },

    // ========== 用户管理 ==========
    handlePageChange(page) {
      this.currentPage = page
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
    },
    async loadUsers() {
      this.loading = true
      try {
        const res = await getAdminUsers()
        if (res.code === 200) {
          this.users = res.data || []
          if ((this.currentPage - 1) * this.pageSize >= this.users.length) {
            this.currentPage = 1
          }
        } else {
          ElMessage.error(res.msg || '加载用户失败')
        }
      } catch (e) {
        ElMessage.error('加载用户失败')
      } finally {
        this.loading = false
      }
    },
    resetForm() {
      this.form = { username: '', role: 'user', password: '' }
      this.currentId = null
    },
    openCreateDialog() {
      this.dialogMode = 'create'
      this.resetForm()
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.userFormRef?.clearValidate()
      })
    },
    openEditDialog(row) {
      this.dialogMode = 'edit'
      this.currentId = row.id
      this.form = {
        username: row.username,
        role: row.role,
        password: ''
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.userFormRef?.clearValidate()
      })
    },
    async submitForm() {
      try {
        await this.$refs.userFormRef.validate()
      } catch (e) {
        return // 校验失败
      }

      this.saving = true
      try {
        let res
        if (this.dialogMode === 'create') {
          res = await createAdminUser(this.form)
        } else {
          res = await updateAdminUser(this.currentId, this.form)
        }

        if (res.code === 200) {
          ElMessage.success(this.dialogMode === 'create' ? '新增成功' : '更新成功')
          this.dialogVisible = false
          this.loadUsers()
          this.loadStats()
        } else {
          ElMessage.error(res.msg || '操作失败')
        }
      } catch (e) {
        ElMessage.error('操作失败')
      } finally {
        this.saving = false
      }
    },
    async handleDelete(row) {
      // 双重保护：禁止删除当前登录用户
      if (row.username === this.currentUsername) {
        ElMessage.warning('不能删除当前登录的账号')
        return
      }
      try {
        await ElMessageBox.confirm(`确认删除用户【${row.username}】吗？`, '提示', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        })

        const res = await deleteAdminUser(row.id)
        if (res.code === 200) {
          ElMessage.success('删除成功')
          this.loadUsers()
          this.loadStats()
        } else {
          ElMessage.error(res.msg || '删除失败')
        }
      } catch (e) {
        // 用户取消不提示
      }
    },
    async handleResetPassword(row) {
      try {
        const { value } = await ElMessageBox.prompt(
          `请输入用户【${row.username}】的新密码`,
          '重置密码',
          {
            confirmButtonText: '确认重置',
            cancelButtonText: '取消',
            inputType: 'password',
            inputPattern: /^.{6,32}$/,
            inputErrorMessage: '密码长度需为6-32位'
          }
        )

        const res = await updateAdminUser(row.id, {
          username: row.username,
          role: row.role,
          password: value
        })

        if (res.code === 200) {
          ElMessage.success('密码重置成功')
        } else {
          ElMessage.error(res.msg || '密码重置失败')
        }
      } catch (e) {
        // 用户取消不提示
      }
    },

    // ========== 保存数据管理 ==========
    handleEnvFilterChange() {
      this.envPage = 1
      this.loadEnvData()
    },
    handleEnvPageChange(page) {
      this.envPage = page
      this.loadEnvData()
    },
    handleEnvSizeChange(size) {
      this.envSize = size
      this.envPage = 1
      this.loadEnvData()
    },
    handleEnvSelectionChange(selection) {
      this.selectedEnvIds = (selection || [])
        .map(item => Number(item.id))
        .filter(id => Number.isFinite(id) && id !== 0)
    },
    resetEnvForm() {
      this.envForm = {
        sensorId: 1,
        temperature: null,
        humidity: null,
        soilMoisture: null,
        soilAdc: null,
        lightIntensity: null,
        co2: null
      }
      this.currentEnvId = null
    },
    async loadEnvData() {
      this.envLoading = true
      try {
        const params = { page: this.envPage, size: this.envSize }
        // 日期筛选
        if (this.envDateRange && this.envDateRange.length === 2) {
          params.startDate = this.envDateRange[0]
          params.endDate = this.envDateRange[1]
        }
        // 保存人筛选
        if (this.envSaverFilter) {
          params.saveUsername = this.envSaverFilter
        }
        const res = await getAdminEnvData(params)
        if (res.code === 200 && res.data) {
          this.envRecords = res.data.records || []
          this.envTotal = res.data.total || 0
          this.selectedEnvIds = []
        } else {
          ElMessage.error(res.msg || '加载保存数据失败')
        }
      } catch (e) {
        ElMessage.error('加载保存数据失败')
      } finally {
        this.envLoading = false
      }
    },
    openCreateEnvDialog() {
      this.envDialogMode = 'create'
      this.resetEnvForm()
      this.envDialogVisible = true
      this.$nextTick(() => {
        this.$refs.envFormRef?.clearValidate()
      })
    },
    openEditEnvDialog(row) {
      this.envDialogMode = 'edit'
      this.currentEnvId = row.id
      this.envForm = {
        sensorId: row.sensorId || 1,
        temperature: row.temperature,
        humidity: row.humidity,
        soilMoisture: row.soilMoisture,
        soilAdc: row.soilAdc,
        lightIntensity: row.lightIntensity,
        co2: row.co2
      }
      this.envDialogVisible = true
      this.$nextTick(() => {
        this.$refs.envFormRef?.clearValidate()
      })
    },
    async submitEnvForm() {
      try {
        await this.$refs.envFormRef.validate()
      } catch (e) {
        return // 校验失败
      }

      this.envSaving = true
      try {
        let res
        if (this.envDialogMode === 'create') {
          res = await createAdminEnvData(this.envForm)
        } else {
          res = await updateAdminEnvData(this.currentEnvId, this.envForm)
        }
        if (res.code === 200) {
          ElMessage.success(this.envDialogMode === 'create' ? '新增保存数据成功' : '更新保存数据成功')
          this.envDialogVisible = false
          this.loadEnvData()
          this.loadStats()
        } else {
          ElMessage.error(res.msg || '操作失败')
        }
      } catch (e) {
        ElMessage.error('操作失败')
      } finally {
        this.envSaving = false
      }
    },
    async handleDeleteEnv(row) {
      try {
        await ElMessageBox.confirm(`确认删除保存记录 ID=${row.id} 吗？`, '提示', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const res = await deleteAdminEnvData(row.id)
        if (res.code === 200) {
          ElMessage.success('删除保存数据成功')
          this.loadEnvData()
          this.loadStats()
        } else {
          ElMessage.error(res.msg || '删除失败')
        }
      } catch (e) {
        if (e !== 'cancel' && e !== 'close') {
          ElMessage.error(e?.response?.data?.msg || e?.message || '删除失败')
        }
      }
    },
    async handleBatchDeleteEnv() {
      if (!this.selectedEnvIds.length) {
        ElMessage.warning('请先勾选要删除的记录')
        return
      }
      try {
        await ElMessageBox.confirm(`确认批量删除 ${this.selectedEnvIds.length} 条保存记录吗？`, '提示', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        })

        const res = await batchDeleteAdminEnvData(this.selectedEnvIds)
        if (res.code === 200) {
          const deletedCount = res?.data?.deletedCount ?? this.selectedEnvIds.length
          ElMessage.success(`批量删除成功，共删除 ${deletedCount} 条`)
          this.loadEnvData()
          this.loadStats()
        } else {
          ElMessage.error(res.msg || '批量删除失败')
        }
      } catch (e) {
        // 用户取消不提示
      }
    },
    getSaverUsername(row) {
      return row?.saveUsername || '历史数据(未记录保存人)'
    },

    // ========== 警告日志管理 ==========
    handleWarnFilter() {
      this.warnPage = 1
      this.loadWarningLogs()
    },
    handleWarnPageChange(page) {
      this.warnPage = page
      this.loadWarningLogs()
    },
    handleWarnSizeChange(size) {
      this.warnSize = size
      this.warnPage = 1
      this.loadWarningLogs()
    },
    handleWarnSelectionChange(selection) {
      this.selectedWarnIds = (selection || []).map(item => item.id)
    },
    async loadWarningLogs() {
      this.warnLoading = true
      try {
        const params = { page: this.warnPage, pageSize: this.warnSize }
        if (this.warnFilterType) params.warningType = this.warnFilterType
        if (this.warnFilterStatus !== '') params.status = this.warnFilterStatus
        const res = await getWarningLogs(params)
        if (res && res.code === 200 && res.data) {
          this.warnRecords = res.data.list || []
          this.warnTotal = res.data.total || 0
          this.selectedWarnIds = []
        }
      } catch (e) {
        ElMessage.error('加载警告日志失败')
      } finally {
        this.warnLoading = false
      }
    },
    async handleMarkOneHandled(id) {
      try {
        await ElMessageBox.confirm('确认标记此警告为已处理？', '提示', { type: 'warning' })
        const res = await markWarningHandled(id)
        if (res && res.code === 200) {
          ElMessage.success('已标记为已处理')
          this.loadWarningLogs()
        } else {
          ElMessage.error(res?.msg || '操作失败')
        }
      } catch (e) { /* 用户取消 */ }
    },
    async handleBatchMarkHandled() {
      const unhandledIds = this.selectedWarnIds.filter(id => {
        const row = this.warnRecords.find(r => r.id === id)
        return row && row.status === 0
      })
      if (unhandledIds.length === 0) {
        ElMessage.warning('所选记录均已处理')
        return
      }
      try {
        await ElMessageBox.confirm(`确认批量标记 ${unhandledIds.length} 条记录为已处理？`, '批量处理', { type: 'warning' })
        const res = await batchMarkHandled(unhandledIds)
        if (res && res.code === 200) {
          ElMessage.success(`成功处理 ${res.data?.handledCount || unhandledIds.length} 条`)
          this.loadWarningLogs()
        } else {
          ElMessage.error(res?.msg || '批量处理失败')
        }
      } catch (e) { /* 用户取消 */ }
    },
    async handleBatchDeleteWarn() {
      try {
        await ElMessageBox.confirm(`确认删除选中的 ${this.selectedWarnIds.length} 条日志？不可恢复！`, '批量删除', { type: 'error' })
        const res = await batchDeleteWarnings(this.selectedWarnIds)
        if (res && res.code === 200) {
          ElMessage.success(`成功删除 ${res.data?.deletedCount || this.selectedWarnIds.length} 条`)
          this.loadWarningLogs()
        } else {
          ElMessage.error(res?.msg || '批量删除失败')
        }
      } catch (e) { /* 用户取消 */ }
    },
    async handleDeleteOneWarn(id) {
      try {
        await ElMessageBox.confirm('确认删除此条日志？不可恢复！', '删除', { type: 'error' })
        const res = await batchDeleteWarnings([id])
        if (res && res.code === 200) {
          ElMessage.success('删除成功')
          this.loadWarningLogs()
        }
      } catch (e) { /* 用户取消 */ }
    },
    async handleClearHandledWarn() {
      try {
        await ElMessageBox.confirm('确认清空所有已处理的日志？不可恢复！', '清空确认', { type: 'error' })
        const res = await clearHandledWarnings()
        if (res && res.code === 200) {
          ElMessage.success(`已清空 ${res.data?.clearedCount || 0} 条`)
          this.loadWarningLogs()
        }
      } catch (e) { /* 用户取消 */ }
    },
    getWarnTypeText(type) {
      const map = { temperature: '温度异常', humidity: '湿度异常', soil: '土壤干旱', light: '光照不足', co2: 'CO₂异常' }
      if (type && type.includes('温度')) return '温度异常'
      if (type && type.includes('湿度')) return '湿度异常'
      return map[type] || type
    },
    getWarnTypeTag(type) {
      const map = { temperature: 'danger', humidity: 'warning', soil: 'success', light: 'info', co2: 'primary' }
      return map[type] || 'info'
    },
    getWarnUnit(type) {
      const map = { temperature: '°C', humidity: '%', soil: 'ADC', light: 'lux', co2: 'ppm' }
      return map[type] || ''
    },

    // ========== 控制记录管理 ==========
    handleCtrlFilter() {
      this.ctrlPage = 1
      this.loadControlHistory()
    },
    handleCtrlPageChange(page) {
      this.ctrlPage = page
      this.loadControlHistory()
    },
    handleCtrlSizeChange(size) {
      this.ctrlSize = size
      this.ctrlPage = 1
      this.loadControlHistory()
    },
    handleCtrlSelectionChange(selection) {
      this.selectedCtrlIds = (selection || []).map(item => item.id)
    },
    async loadControlHistory() {
      this.ctrlLoading = true
      try {
        const res = await getControlHistory({
          page: this.ctrlPage,
          pageSize: this.ctrlSize,
          controlType: this.ctrlFilterType || undefined
        })
        const data = res.data || res || {}
        this.ctrlRecords = data.list || []
        this.ctrlTotal = data.total || 0
        this.selectedCtrlIds = []
      } catch (e) {
        ElMessage.error('加载控制记录失败')
      } finally {
        this.ctrlLoading = false
      }
    },
    async handleBatchDeleteCtrl() {
      try {
        await ElMessageBox.confirm(`确认删除选中的 ${this.selectedCtrlIds.length} 条记录？不可恢复！`, '批量删除', { type: 'error' })
        const res = await batchDeleteControlHistory(this.selectedCtrlIds)
        if (res && res.code === 200) {
          ElMessage.success(`成功删除 ${res.data?.deletedCount || this.selectedCtrlIds.length} 条`)
          this.loadControlHistory()
        } else {
          ElMessage.error(res?.msg || '批量删除失败')
        }
      } catch (e) { /* 用户取消 */ }
    },
    async handleDeleteOneCtrl(id) {
      try {
        await ElMessageBox.confirm('确认删除此条记录？不可恢复！', '删除', { type: 'error' })
        const res = await batchDeleteControlHistory([id])
        if (res && res.code === 200) {
          ElMessage.success('删除成功')
          this.loadControlHistory()
        }
      } catch (e) { /* 用户取消 */ }
    },
    async handleClearAllCtrl() {
      try {
        await ElMessageBox.confirm('确认清空全部控制记录？此操作不可恢复！', '清空确认', { type: 'error' })
        const res = await clearAllControlHistory()
        if (res && res.code === 200) {
          ElMessage.success(`已清空 ${res.data?.clearedCount || 0} 条记录`)
          this.loadControlHistory()
        }
      } catch (e) { /* 用户取消 */ }
    },
    getCtrlTypeText(type) {
      const map = { pump: '水泵', fan: '风扇', light: '照明', mode: '模式切换', threshold: '阈值设置' }
      return map[type] || type || '-'
    },
    getCtrlTypeTag(type) {
      const map = { pump: 'primary', fan: 'success', light: 'warning', mode: 'info', threshold: '' }
      return map[type] || 'info'
    }
  }
}
</script>

<style scoped>
/* ========== 智慧农业主题设计 ========== */
/* Primary: #1a472a (深森林绿) Accent: #3a7d44 (森林绿)
   Secondary: #0f766e (青色) Surface: #f0fdf4 (薄荷绿) */

.admin-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0fdfa 100%);
  position: relative;
}

.admin-page::before {
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
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 24px 28px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  box-shadow: 0 4px 20px rgba(26, 71, 42, 0.06);
  position: relative;
  z-index: 10;
}

.admin-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #1a472a, #3a7d44, #22c55e);
  border-radius: 16px 16px 0 0;
}

.header-left h2 {
  margin: 0 0 8px;
  color: #1a472a;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.header-left p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.header-right {
  flex-shrink: 0;
}

/* ========== Stat Cards ========== */
.stats-row {
  margin-bottom: 20px;
  position: relative;
  z-index: 10;
}

.stats-row .stat-card {
  border-radius: 14px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(71, 85, 99, 0.08);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
}

.stats-row .stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(26, 71, 42, 0.12);
}

.stats-row .stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 6px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stats-row .stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a472a;
  line-height: 1.2;
}

.stats-row .stat-label {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  font-weight: 500;
}

/* ========== User Card ========== */
.user-card {
  margin-bottom: 20px;
  border-radius: 16px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  position: relative;
  z-index: 10;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header > span {
  font-size: 16px;
  font-weight: 600;
  color: #1a472a;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 220px;
}

.pagination-wrapper {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

/* ========== Filter Area ========== */
.env-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 18px;
  padding: 16px 18px;
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.8), rgba(236, 253, 245, 0.8));
  border-radius: 12px;
  border: 1px solid rgba(71, 85, 99, 0.08);
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  font-weight: 500;
  color: #1a472a;
  white-space: nowrap;
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(58, 125, 68, 0.12), rgba(34, 197, 94, 0.08));
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid rgba(58, 125, 68, 0.15);
  line-height: 1.5;
}

/* ========== Data Value Colors ========== */
.val-normal {
  color: #1a472a;
  font-weight: 500;
}

.val-danger {
  color: #ef4444;
  font-weight: 600;
}

.val-cold {
  color: #0ea5e9;
  font-weight: 600;
}

.val-warning {
  color: #f59e0b;
  font-weight: 600;
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

/* ========== Tab Styles ========== */
:deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(71, 85, 99, 0.1);
}

:deep(.el-tabs__item) {
  color: #64748b;
  font-weight: 500;
}

:deep(.el-tabs__item.is-active) {
  color: #1a472a;
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #1a472a, #3a7d44);
  height: 3px;
  border-radius: 2px;
}

/* ========== Header Actions 筛选框样式 ========== */
.header-actions {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.admin-search-container {
  display: flex;
  align-items: center;
}

.admin-search-input {
  width: 200px;
}

.admin-filter-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.admin-filter-select {
  width: 130px;
}

.admin-action-group {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

/* 统一管理页面筛选框样式 */
.admin-search-input :deep(.el-input__wrapper),
.admin-filter-select :deep(.el-input__wrapper) {
  border-radius: 8px;
  border: 1px solid rgba(58, 125, 68, 0.2);
  background: #fff;
  transition: all 0.2s ease;
}

.admin-search-input :deep(.el-input__wrapper:hover),
.admin-filter-select :deep(.el-input__wrapper:hover) {
  border-color: rgba(58, 125, 68, 0.4);
  box-shadow: 0 2px 8px rgba(58, 125, 68, 0.1);
}

.admin-search-input :deep(.el-input.is-focus .el-input__wrapper),
.admin-filter-select :deep(.el-input.is-focus .el-input__wrapper) {
  border-color: #3a7d44;
  box-shadow: 0 0 0 2px rgba(58, 125, 68, 0.1);
}

.admin-search-input :deep(.el-input__inner),
.admin-filter-select :deep(.el-input__inner) {
  color: #1a472a;
  font-size: 13px;
  font-weight: 500;
}

.admin-search-input :deep(.el-input__placeholder-inner),
.admin-filter-select :deep(.el-select__placeholder) {
  color: rgba(26, 71, 42, 0.5);
  font-size: 13px;
}

.admin-filter-select :deep(.el-select__caret) {
  color: rgba(58, 125, 68, 0.7);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .header-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .admin-search-container,
  .admin-filter-group,
  .admin-action-group {
    width: 100%;
  }

  .admin-search-input,
  .admin-filter-select {
    width: 100%;
  }

  .admin-filter-group,
  .admin-action-group {
    flex-wrap: wrap;
    justify-content: center;
  }
}

/* ========== Pagination ========== */
:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #1a472a, #3a7d44);
  border-radius: 6px;
}

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .admin-page {
    padding: 16px;
  }

  .admin-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
    padding: 18px 20px;
  }

  .header-left h2 {
    font-size: 20px;
  }

  .env-filters {
    flex-direction: column;
  }

  .header-actions {
    flex-wrap: wrap;
    width: 100%;
  }
}
</style>
