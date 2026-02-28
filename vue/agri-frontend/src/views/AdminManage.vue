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
            <el-input v-model.trim="searchKeyword" placeholder="按用户名搜索" clearable class="search-input" />
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
      }
    }
  },
  mounted() {
    this.loadStats()
    this.loadUsers()
    this.loadEnvData()
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
    }
  }
}
</script>

<style scoped>
.admin-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 升级后的页面头部 */
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e0e0e0;
}

.header-left h2 {
  margin: 0 0 8px;
  color: #2e7d32;
  font-size: 26px;
  font-weight: 600;
}

.header-left p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.header-right {
  flex-shrink: 0;
}

/* 统计概览卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stats-row .stat-card {
  border-radius: 10px;
  transition: all 0.3s ease;
  border: none;
}

.stats-row .stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.stats-row .stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 4px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stats-row .stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #333;
  line-height: 1.2;
}

.stats-row .stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

/* 通用卡片 */
.user-card {
  margin-bottom: 20px;
  border-radius: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header > span {
  font-size: 16px;
  font-weight: 600;
  color: #2e7d32;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 220px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 筛选区域 */
.env-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #f8faf8;
  border-radius: 8px;
  border: 1px solid #e8f5e9;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  font-weight: 500;
  color: #555;
  white-space: nowrap;
}

/* 数据条件着色 */
.val-normal {
  color: #333;
  font-weight: 500;
}

.val-danger {
  color: #f56c6c;
  font-weight: 600;
}

.val-cold {
  color: #409eff;
  font-weight: 600;
}

.val-warning {
  color: #e6a23c;
  font-weight: 600;
}

/* 响应式 */
@media (max-width: 768px) {
  .admin-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .env-filters {
    flex-direction: column;
  }

  .header-actions {
    flex-wrap: wrap;
  }
}
</style>
