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
.device-status-panel {
  width: 100%;
}

.status-card {
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
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.status-section {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.status-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 12px;
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
  font-size: 16px;
  font-weight: 600;
}

.status-indicator.online {
  color: #67c23a;
}

.status-indicator.offline {
  color: #f56c6c;
}

.indicator-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.status-indicator.online .indicator-dot {
  background: #67c23a;
  box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.7);
}

.status-indicator.offline .indicator-dot {
  background: #f56c6c;
  box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.7);
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.7);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(103, 194, 58, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0);
  }
}

.last-update {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #909399;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 10px;
}

.status-item:last-child {
  margin-bottom: 0;
}

.mode-status {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8edf3 100%);
}

.item-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 24px;
}

.pump-icon {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.fan-icon {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.light-icon {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-label {
  font-size: 14px;
  color: #606266;
}

.item-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.device-status-list .status-item {
  position: relative;
}

.status-light {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #dcdfe6;
  transition: all 0.3s;
}

.status-light.active {
  background: #67c23a;
  box-shadow: 0 0 8px rgba(103, 194, 58, 0.6);
}

.threshold-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.threshold-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 13px;
}

.threshold-label {
  color: #606266;
}

.threshold-value {
  font-weight: 600;
  color: #303133;
}
</style>
