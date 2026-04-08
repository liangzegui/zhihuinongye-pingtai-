<template>
  <div class="cache-config">
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><Coin /></el-icon> 离线缓存设置</span>
          <el-tag :type="esp32Connected ? 'success' : 'danger'" size="small">
            {{ esp32Connected ? 'ESP32 已连接' : 'ESP32 未连接' }}
          </el-tag>
        </div>
      </template>

      <!-- ESP32未连接时的提示 -->
      <el-alert
        v-if="!esp32Connected && !loading"
        title="ESP32 未连接"
        description="无法获取离线缓存配置，请检查单片机连接状态。"
        type="error"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />

      <!-- SD卡不可用警告 -->
      <el-alert
        v-if="esp32Connected && !sdCardAvailable"
        title="SD 卡不可用"
        description="SD卡未插入或初始化失败，离线缓存功能无法正常工作。"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />

      <!-- 状态信息 -->
      <div class="status-grid">
        <div class="status-item">
          <span class="status-label">SD 卡状态</span>
          <el-tag :type="sdCardAvailable ? 'success' : 'danger'" size="small">
            {{ sdCardAvailable ? '可用' : '不可用' }}
          </el-tag>
        </div>
        <div class="status-item">
          <span class="status-label">网页端连接</span>
          <el-tag :type="webClientConnected ? 'success' : 'warning'" size="small">
            {{ webClientConnected ? '在线' : '离线' }}
          </el-tag>
        </div>
        <div class="status-item">
          <span class="status-label">缓存数据</span>
          <el-tag :type="hasCachedData ? 'warning' : 'info'" size="small">
            {{ hasCachedData ? `有缓存 (${formatFileSize(cachedFileSize)})` : '无缓存' }}
          </el-tag>
        </div>
      </div>

      <el-divider />

      <!-- 缓存间隔设置 -->
      <el-form label-width="120px" class="config-form">
        <el-form-item label="缓存间隔">
          <el-input-number
            v-model="cacheInterval"
            :min="5"
            :max="3600"
            :step="5"
            :disabled="!esp32Connected"
            controls-position="right"
            style="width: 200px"
          />
          <span class="unit-label">秒（范围 5 - 3600）</span>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="saveInterval"
            :loading="saving"
            :disabled="!esp32Connected"
          >
            <el-icon><Check /></el-icon> 保存设置
          </el-button>
          <el-button @click="refreshStatus" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新状态
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <div class="tips">
        <h4><el-icon><InfoFilled /></el-icon> 使用说明</h4>
        <ul>
          <li>当后端系统与 ESP32 断连超过 30 秒时，ESP32 会自动将传感器数据缓存到 SD 卡</li>
          <li>缓存间隔决定断连期间每隔多少秒保存一条数据（默认 10 秒）</li>
          <li>系统重新连接后，缓存数据会自动恢复到数据库</li>
          <li>设置范围：最小 5 秒，最大 3600 秒（1 小时）</li>
          <li>设置会持久化保存到 ESP32 的 Flash，重启不丢失</li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getCacheInterval, setCacheInterval } from '@/api/config'
import { Check, Refresh, InfoFilled, Coin } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'CacheConfig',
  components: { Check, Refresh, InfoFilled, Coin },
  data() {
    return {
      cacheInterval: 10,
      sdCardAvailable: false,
      webClientConnected: false,
      hasCachedData: false,
      cachedFileSize: 0,
      esp32Connected: false,
      loading: false,
      saving: false
    }
  },
  mounted() {
    this.refreshStatus()
  },
  methods: {
    async refreshStatus() {
      this.loading = true
      try {
        const res = await getCacheInterval()
        const data = res?.data?.data || res?.data || {}
        this.cacheInterval = data.cacheInterval || 10
        this.sdCardAvailable = data.sdCardAvailable || false
        this.webClientConnected = data.webClientConnected || false
        this.hasCachedData = data.hasCachedData || false
        this.cachedFileSize = data.cachedFileSize || 0
        this.esp32Connected = true
      } catch (err) {
        console.error('获取缓存配置失败', err)
        this.esp32Connected = false
      } finally {
        this.loading = false
      }
    },

    async saveInterval() {
      if (this.cacheInterval < 5 || this.cacheInterval > 3600) {
        ElMessage.warning('缓存间隔范围为 5 - 3600 秒')
        return
      }

      this.saving = true
      try {
        const res = await setCacheInterval(this.cacheInterval)
        const data = res?.data?.data || res?.data || {}
        if (data.success) {
          ElMessage.success(data.message || '缓存间隔设置成功')
        } else {
          ElMessage.error(data.message || '设置失败')
        }
      } catch (err) {
        console.error('设置缓存间隔失败', err)
        ElMessage.error('设置失败，请检查ESP32连接')
      } finally {
        this.saving = false
      }
    },

    formatFileSize(bytes) {
      if (!bytes || bytes === 0) return '0 B'
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    }
  }
}
</script>

<style scoped>
.cache-config {
  padding: 20px;
}

.config-card {
  max-width: 600px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.status-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.status-label {
  font-size: 13px;
  color: #909399;
}

.config-form {
  margin-top: 10px;
}

.unit-label {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}

.tips {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
}

.tips h4 {
  margin: 0 0 10px 0;
  display: flex;
  align-items: center;
  gap: 5px;
  color: #409eff;
}

.tips ul {
  margin: 0;
  padding-left: 20px;
}

.tips li {
  margin: 5px 0;
}

@media (max-width: 768px) {
  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
