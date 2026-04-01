<template>
  <el-card class="config-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <span class="header-title">
          <el-icon><Timer /></el-icon>
          数据自动保存配置
        </span>
        <el-tag :type="config.enabled ? 'success' : 'info'" size="small">
          {{ config.enabled ? '已启用' : '已禁用' }}
        </el-tag>
      </div>
    </template>

    <el-form :model="config" label-width="120px" v-loading="loading">
      <!-- 启用开关 -->
      <el-form-item label="自动保存">
        <el-switch
          v-model="config.enabled"
          active-text="启用"
          inactive-text="禁用"
          @change="handleEnabledChange"
        />
        <span class="form-tip">启用后，系统会自动将实时数据保存到历史记录</span>
      </el-form-item>

      <!-- 保存间隔 -->
      <el-form-item label="保存间隔">
        <el-input-number
          v-model="config.intervalSeconds"
          :min="5"
          :max="3600"
          :step="5"
          :disabled="!config.enabled"
          style="width: 150px;"
        />
        <span class="unit">秒</span>
        <span class="form-tip">范围：5秒 - 3600秒（1小时）</span>
      </el-form-item>

      <!-- 快捷选项 -->
      <el-form-item label="快捷设置">
        <el-button-group :disabled="!config.enabled">
          <el-button size="small" @click="setInterval(10)" :disabled="!config.enabled">10秒</el-button>
          <el-button size="small" @click="setInterval(30)" :disabled="!config.enabled">30秒</el-button>
          <el-button size="small" @click="setInterval(60)" :disabled="!config.enabled">1分钟</el-button>
          <el-button size="small" @click="setInterval(300)" :disabled="!config.enabled">5分钟</el-button>
          <el-button size="small" @click="setInterval(600)" :disabled="!config.enabled">10分钟</el-button>
        </el-button-group>
      </el-form-item>

      <!-- 上次保存时间 -->
      <el-form-item label="上次保存时间">
        <span class="last-save-time">{{ formatLastSaveTime }}</span>
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button type="primary" @click="saveConfig" :loading="saving">
          <el-icon><Check /></el-icon>
          保存配置
        </el-button>
        <el-button type="success" @click="triggerSave" :loading="triggering">
          <el-icon><Upload /></el-icon>
          立即保存数据
        </el-button>
        <el-button @click="loadConfig">
          <el-icon><Refresh /></el-icon>
          刷新状态
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script>
import { Timer, Check, Upload, Refresh } from '@element-plus/icons-vue'
import { getAutoSaveConfig, updateAutoSaveConfig, saveEnvData } from '@/api/config'
import { getRealTimeData } from '@/api/data'
import { ElMessage } from 'element-plus'

export default {
  name: 'DataAutoSaveConfig',
  components: { Timer, Check, Upload, Refresh },
  data() {
    return {
      loading: false,
      saving: false,
      triggering: false,
      config: {
        enabled: true,
        intervalSeconds: 60,
        lastSaveTime: null
      }
    }
  },
  computed: {
    formatLastSaveTime() {
      if (!this.config.lastSaveTime) {
        return '暂无记录'
      }
      try {
        const date = new Date(this.config.lastSaveTime)
        return date.toLocaleString()
      } catch (e) {
        return this.config.lastSaveTime
      }
    }
  },
  mounted() {
    this.loadConfig()
  },
  methods: {
    async loadConfig() {
      this.loading = true
      try {
        const res = await getAutoSaveConfig()
        if (res && res.data) {
          this.config = {
            enabled: res.data.enabled,
            intervalSeconds: res.data.intervalSeconds,
            lastSaveTime: res.data.lastSaveTime
          }
        }
      } catch (error) {
        console.error('加载配置失败:', error)
        ElMessage.error('加载配置失败')
      } finally {
        this.loading = false
      }
    },

    async saveConfig() {
      this.saving = true
      try {
        const res = await updateAutoSaveConfig({
          enabled: this.config.enabled,
          intervalSeconds: this.config.intervalSeconds
        })
        if (res && res.code === 200) {
          ElMessage.success('配置保存成功')
        } else {
          ElMessage.error(res?.message || '保存失败')
        }
      } catch (error) {
        console.error('保存配置失败:', error)
        ElMessage.error('保存配置失败')
      } finally {
        this.saving = false
      }
    },

    async triggerSave() {
      this.triggering = true
      try {
        // 先获取实时数据
        const realTimeRes = await getRealTimeData()
        let envData = null
        
        // axios拦截器已经返回 response.data，所以 realTimeRes = {code: 200, data: [...]}
        if (realTimeRes && realTimeRes.data) {
          const dataList = realTimeRes.data
          if (Array.isArray(dataList) && dataList.length > 0) {
            envData = dataList[0]
          } else if (typeof dataList === 'object' && !Array.isArray(dataList)) {
            envData = dataList
          }
        }
        
        if (!envData || (envData.temperature === undefined && envData.humidity === undefined)) {
          ElMessage.warning('无法获取实时数据，请确保设备已连接')
          return
        }
        
        // 检查数据来源，如果是数据库数据则提示用户
        if (envData.dataSource === 'database' || envData.isRealTime === false) {
          ElMessage.warning('当前显示的是历史数据（设备未连接），无法保存为新数据')
          return
        }
        
        // 使用实时数据保存到数据库
        // 注意: soilMoisture 是百分比(0-100)，soilAdc 是原始ADC值(0-4095)
        const saveData = {
          temperature: envData.temperature || 0,
          humidity: envData.humidity || 0,
          soilMoisture: envData.soilMoisture || 0,
          soilAdc: envData.soilAdc || 0,
          lightIntensity: envData.lightIntensity || 0,
          co2: envData.co2 || envData.eco2 || 0
        }
        const res = await saveEnvData(saveData)
        
        // axios拦截器已返回 response.data，所以 res = {code: 200, data: {success: true}}
        if (res && res.data?.success) {
          ElMessage.success('数据保存成功')
          this.loadConfig() // 刷新上次保存时间
        } else {
          ElMessage.warning(res?.data?.message || '保存失败')
        }
      } catch (error) {
        console.error('保存数据失败:', error)
        ElMessage.error('保存数据失败: ' + (error.message || '未知错误'))
      } finally {
        this.triggering = false
      }
    },

    handleEnabledChange() {
      // 开关变化时不自动保存，需要点击保存按钮
    },

    setInterval(seconds) {
      this.config.intervalSeconds = seconds
    }
  }
}
</script>

<style scoped>
.config-card {
  margin-top: 20px;
  border-radius: 12px;
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

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.unit {
  margin-left: 8px;
  color: #606266;
}

.last-save-time {
  color: #409eff;
  font-weight: 500;
}
</style>
