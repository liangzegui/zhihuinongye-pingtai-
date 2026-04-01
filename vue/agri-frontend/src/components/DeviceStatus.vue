<template>
  <div class="device-status-panel">
    <el-card class="status-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="header-title">
            <el-icon><Monitor /></el-icon>
            设备状态监控
          </span>
          <el-button
            size="small"
            type="primary"
            circle
            @click="refreshStatus"
            :loading="refreshing"
          >
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
      </template>

      <!-- 在线状态 -->
      <div class="status-section">
        <div class="online-status">
          <div class="status-indicator" :class="{ online: isOnline, offline: !isOnline }">
            <span class="indicator-dot"></span>
            <span class="indicator-text">{{ isOnline ? '设备在线' : '设备离线' }}</span>
          </div>
          <div class="last-update">
            <el-icon><Clock /></el-icon>
            最后更新：{{ lastUpdateTime }}
          </div>
        </div>
      </div>

      <!-- 运行模式 -->
      <div class="status-section">
        <div class="status-item mode-status">
          <div class="item-icon">
            <el-icon size="24"><Tools /></el-icon>
          </div>
          <div class="item-info">
            <span class="item-label">运行模式</span>
            <span class="item-value">
              <el-tag :type="isManualMode ? 'warning' : 'success'">
                {{ isManualMode ? '手动模式' : '自动模式' }}
              </el-tag>
            </span>
          </div>
        </div>
      </div>

      <!-- 设备开关状态 -->
      <div class="status-section">
        <div class="section-title">设备状态</div>
        <div class="device-status-list">
          <div class="status-item">
            <div class="item-icon pump-icon">💧</div>
            <div class="item-info">
              <span class="item-label">水泵</span>
              <span class="item-value">
                <el-tag :type="deviceStates.pump ? 'success' : 'info'" size="small">
                  {{ deviceStates.pump ? '运行中' : '已停止' }}
                </el-tag>
              </span>
            </div>
            <div class="status-light" :class="{ active: deviceStates.pump }"></div>
          </div>

          <div class="status-item">
            <div class="item-icon fan-icon">🌀</div>
            <div class="item-info">
              <span class="item-label">风扇</span>
              <span class="item-value">
                <el-tag :type="deviceStates.fan ? 'success' : 'info'" size="small">
                  {{ deviceStates.fan ? '运行中' : '已停止' }}
                </el-tag>
              </span>
            </div>
            <div class="status-light" :class="{ active: deviceStates.fan }"></div>
          </div>

          <div class="status-item">
            <div class="item-icon light-icon">💡</div>
            <div class="item-info">
              <span class="item-label">照明</span>
              <span class="item-value">
                <el-tag :type="deviceStates.light ? 'success' : 'info'" size="small">
                  {{ deviceStates.light ? '已开启' : '已关闭' }}
                </el-tag>
              </span>
            </div>
            <div class="status-light" :class="{ active: deviceStates.light }"></div>
          </div>
        </div>
      </div>

      <!-- 当前阈值 -->
      <div class="status-section">
        <div class="section-title">当前阈值</div>
        <div class="threshold-list">
          <div class="threshold-item">
            <span class="threshold-label">风扇温度阈值：</span>
            <span class="threshold-value">{{ thresholds.fanTemp }} ℃</span>
          </div>
          <div class="threshold-item">
            <span class="threshold-label">风扇CO₂阈值：</span>
            <span class="threshold-value">{{ thresholds.fanCO2 }} ppm</span>
          </div>
          <div class="threshold-item">
            <span class="threshold-label">水泵干旱阈值：</span>
            <span class="threshold-value">{{ thresholds.pumpDrought }} ADC</span>
          </div>
          <div class="threshold-item">
            <span class="threshold-label">光照阈值：</span>
            <span class="threshold-value">{{ thresholds.lightLux }} lux</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Refresh, Clock, Monitor, Tools } from '@element-plus/icons-vue'
import { getDeviceStatus } from '@/api/device'

export default {
  name: 'DeviceStatus',
  components: {
    Refresh,
    Clock,
    Monitor,
    Tools
  },
  data() {
    return {
      isOnline: true,
      isManualMode: false,
      refreshing: false,
      lastUpdateTime: '--',
      deviceStates: {
        pump: false,
        fan: false,
        light: false
      },
      thresholds: {
        fanTemp: 32,
        fanCO2: 1200,
        pumpDrought: 3300,
        lightLux: 700
      },
      statusTimer: null,
      Refresh,
      Clock,
      Monitor,
      Tools
    }
  },
  mounted() {
    this.fetchDeviceStatus()
    // 每5秒刷新一次状态
    this.statusTimer = setInterval(() => {
      this.fetchDeviceStatus(true)
    }, 5000)
  },
  beforeUnmount() {
    if (this.statusTimer) {
      clearInterval(this.statusTimer)
    }
  },
  methods: {
    async fetchDeviceStatus(silent = false) {
      if (!silent) {
        this.refreshing = true
      }
      
      try {
        const res = await getDeviceStatus()
        if (res && res.code === 200) {
          const data = res.data
          
          // 更新在线状态
          this.isOnline = data.online !== false
          
          // 更新模式
          this.isManualMode = data.manualMode || data.manual || false
          
          // 更新设备状态
          this.deviceStates.pump = data.pumpState || data.pump || false
          this.deviceStates.fan = data.fanState || data.fan || false
          this.deviceStates.light = data.lightState || data.light || false
          
          // 更新阈值
          if (data.thresholds) {
            Object.assign(this.thresholds, this.normalizeThresholds(data.thresholds))
          }
          
          // 更新时间
          if (data.lastUpdate) {
            this.lastUpdateTime = this.formatTime(new Date(data.lastUpdate))
          } else {
            this.lastUpdateTime = this.formatTime(new Date())
          }
          
          this.$emit('status-updated', data)
        } else {
          this.isOnline = false
        }
      } catch (error) {
        console.error('获取设备状态失败:', error)
        this.isOnline = false
        if (!silent) {
          ElMessage.error('设备状态获取失败')
        }
      } finally {
        this.refreshing = false
      }
    },

    refreshStatus() {
      this.fetchDeviceStatus()
      ElMessage.success('状态已刷新')
    },

    formatTime(date) {
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${hours}:${minutes}:${seconds}`
    },

    normalizeThresholds(thresholds) {
      const toNumber = (value, fallback) => {
        const num = Number(value)
        return Number.isFinite(num) ? num : fallback
      }

      return {
        fanTemp: toNumber(
          thresholds.fanTemp ?? thresholds.fanTempThreshold ?? thresholds.fan_temp ?? thresholds.fan_temp_threshold,
          this.thresholds.fanTemp
        ),
        fanCO2: toNumber(
          thresholds.fanCO2 ?? thresholds.fanCO2Threshold ?? thresholds.fan_co2 ?? thresholds.fan_co2_threshold,
          this.thresholds.fanCO2
        ),
        pumpDrought: toNumber(
          thresholds.pumpDrought ?? thresholds.pumpDroughtThreshold ?? thresholds.pump_drought ?? thresholds.pump_drought_threshold,
          this.thresholds.pumpDrought
        ),
        lightLux: toNumber(
          thresholds.lightLux ?? thresholds.lightLuxThreshold ?? thresholds.light_lux ?? thresholds.light_lux_threshold,
          this.thresholds.lightLux
        )
      }
    }
  }
}
</script>

<style scoped>
/* Design tokens: same as RealTime.vue
   Primary: #3a7d44   Accent: #2563eb   Danger: #dc2626
   Text: #1e293b / #475569 / #94a3b8
   Border: #e2e8f0    Surface: #f8fafc */

.device-status-panel {
  width: 100%;
}

.status-card {
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: none;
}

.status-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

:deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.el-card__body) {
  padding: 16px 18px;
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
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.status-section {
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;
}

.status-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.section-title {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 10px;
}

.online-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
}

.status-indicator.online { color: #16a34a; }
.status-indicator.offline { color: #dc2626; }

.indicator-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-indicator.online .indicator-dot {
  background: #16a34a;
  box-shadow: 0 0 0 3px rgba(22, 163, 106, 0.15);
  animation: pulse-dot 2s infinite;
}

.status-indicator.offline .indicator-dot {
  background: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.15);
}

@keyframes pulse-dot {
  0%, 100% { box-shadow: 0 0 0 3px rgba(22, 163, 106, 0.15); }
  50% { box-shadow: 0 0 0 6px rgba(22, 163, 106, 0); }
}

.last-update {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 8px;
}

.status-item:last-child {
  margin-bottom: 0;
}

.mode-status {
  background: #f8fafc;
}

.item-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 20px;
}

.pump-icon { background: #dbeafe; }
.fan-icon { background: #dcfce7; }
.light-icon { background: #fef3c7; }

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.item-value {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.device-status-list .status-item {
  position: relative;
}

.status-light {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e2e8f0;
  transition: all 0.3s;
}

.status-light.active {
  background: #16a34a;
  box-shadow: 0 0 6px rgba(22, 163, 106, 0.5);
}

.threshold-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.threshold-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 7px 10px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 12px;
}

.threshold-label {
  color: #64748b;
}

.threshold-value {
  font-weight: 600;
  color: #1e293b;
  font-variant-numeric: tabular-nums;
}

:deep(.el-tag) {
  border: none;
  font-size: 11px;
}
</style>
