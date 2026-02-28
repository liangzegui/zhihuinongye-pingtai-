<template>
  <div class="device-control-panel">
    <el-card class="control-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="header-title">
            <el-icon><Setting /></el-icon>
            设备控制面板
          </span>
          <el-tag :type="isManualMode ? 'warning' : 'success'" effect="dark">
            {{ isManualMode ? '手动模式' : '自动模式' }}
          </el-tag>
        </div>
      </template>

      <!-- 模式切换 -->
      <div class="control-section">
        <div class="section-title">
          <el-icon><Tools /></el-icon>
          <span>运行模式</span>
        </div>
        <div class="mode-switch">
          <span class="switch-label">自动模式</span>
          <el-switch
            v-model="isManualMode"
            :loading="modeLoading"
            active-color="#f56c6c"
            inactive-color="#67c23a"
            @change="handleModeChange"
          />
          <span class="switch-label">手动模式</span>
        </div>
        <div class="mode-description">
          <el-alert
            v-if="isManualMode"
            type="warning"
            :closable="false"
            show-icon
          >
            手动模式下，设备需要手动控制开关状态
          </el-alert>
          <el-alert
            v-else
            type="success"
            :closable="false"
            show-icon
          >
            自动模式下，系统将根据阈值自动控制设备
          </el-alert>
        </div>
      </div>

      <!-- 设备开关控制 -->
      <div class="control-section">
        <div class="section-title">
          <el-icon><Connection /></el-icon>
          <span>设备开关</span>
        </div>
        <div class="device-switches">
          <div class="switch-item">
            <div class="switch-icon pump-icon">💧</div>
            <div class="switch-info">
              <span class="switch-name">水泵</span>
              <span class="switch-status">{{ deviceStatus.pumpState ? '运行中' : '已停止' }}</span>
            </div>
            <el-switch
              v-model="deviceStatus.pumpState"
              :disabled="!isManualMode || pumpLoading"
              :loading="pumpLoading"
              active-color="#409eff"
              @change="handlePumpChange"
            />
          </div>

          <div class="switch-item">
            <div class="switch-icon fan-icon">🌀</div>
            <div class="switch-info">
              <span class="switch-name">风扇</span>
              <span class="switch-status">{{ deviceStatus.fanState ? '运行中' : '已停止' }}</span>
            </div>
            <el-switch
              v-model="deviceStatus.fanState"
              :disabled="!isManualMode || fanLoading"
              :loading="fanLoading"
              active-color="#67c23a"
              @change="handleFanChange"
            />
          </div>

          <div class="switch-item">
            <div class="switch-icon light-icon">💡</div>
            <div class="switch-info">
              <span class="switch-name">照明</span>
              <span class="switch-status">{{ deviceStatus.lightState ? '已开启' : '已关闭' }}</span>
            </div>
            <el-switch
              v-model="deviceStatus.lightState"
              :disabled="!isManualMode || lightLoading"
              :loading="lightLoading"
              active-color="#e6a23c"
              @change="handleLightChange"
            />
          </div>
        </div>
      </div>

      <!-- 阈值设置 -->
      <div class="control-section" @mouseenter="onThresholdAreaEnter" @mouseleave="onThresholdAreaLeave">
        <div class="section-title">
          <el-icon><DataLine /></el-icon>
          <span>阈值设置</span>
        </div>
        <el-form :model="thresholdForm" label-width="120px" class="threshold-form">
          <el-form-item label="风扇温度阈值">
            <div class="threshold-input-group">
              <el-input-number
                v-model="thresholdForm.fanTemp"
                :min="0"
                :max="60"
                :step="0.5"
                :precision="1"
                placeholder="温度阈值"
                @change="onThresholdValueChange"
              />
              <span class="unit">℃</span>
            </div>
                <div class="threshold-desc" style="font-size: 13px; color: #909399; text-align: left;">温度阈值(℃): 超过此值开启风扇</div>
          </el-form-item>

          <el-form-item label="风扇CO₂阈值">
            <div class="threshold-input-group">
              <el-input-number
                v-model="thresholdForm.fanCO2"
                :min="400"
                :max="5000"
                :step="50"
                placeholder="CO₂阈值"
                @change="onThresholdValueChange"
              />
              <span class="unit">ppm</span>
            </div>
                <div class="threshold-desc" style="font-size: 13px; color: #909399; text-align: left;">CO₂阈值(ppm): 超过此值开启风扇</div>
          </el-form-item>

          <el-form-item label="水泵干旱阈值">
            <div class="threshold-input-group">
              <el-input-number
                v-model="thresholdForm.pumpDrought"
                :min="0"
                :max="4095"
                :step="50"
                placeholder="ADC越大越干"
                @change="onThresholdValueChange"
              />
              <span class="unit">ADC</span>
            </div>
                <div class="threshold-desc" style="font-size: 13px; color: #909399; text-align: left;">ADC范围0-4095，值越大越干。参考: 过湿&lt;2200，正常2200-2800，轻旱2800-3200，中旱3200-3500，重旱&gt;3500。超过此值开启水泵</div>
          </el-form-item>

          <el-form-item label="照明光照阈值">
            <div class="threshold-input-group">
              <el-input-number
                v-model="thresholdForm.lightLux"
                :min="0"
                :max="5000"
                :step="50"
                placeholder="lux阈值"
                @change="onThresholdValueChange"
              />
              <span class="unit">lux</span>
            </div>
                <div class="threshold-desc" style="font-size: 13px; color: #909399; text-align: left;">参考: 暗&lt;800lux, 偏暗800-1000, 正常1000-3000, 明亮&gt;3000。低于此值开启灯泡</div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="thresholdLoading"
              @click="handleThresholdSubmit"
              icon="Check"
            >
              保存阈值设置
            </el-button>
            <el-button @click="resetThresholds" icon="RefreshLeft">
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import {
  Connection,
  DataLine,
  Setting,
  Tools
} from '@element-plus/icons-vue'
import {
  controlPump,
  controlFan,
  controlLight,
  setMode,
  setThresholds,
  getDeviceStatus
} from '@/api/device'

export default {
  name: 'DeviceControl',
  components: {
    Connection,
    DataLine,
    Setting,
    Tools
  },
  data() {
    return {
      isManualMode: false,
      modeLoading: false,
      pumpLoading: false,
      fanLoading: false,
      lightLoading: false,
      thresholdLoading: false,
      deviceStatus: {
        pumpState: false,
        fanState: false,
        lightState: false
      },
      thresholdForm: {
        fanTemp: 32,
        fanCO2: 1200,
        pumpDrought: 3300,
        lightLux: 700
      },
      isEditingThreshold: false,
      thresholdBlurTimer: null,
      refreshTimer: null
    }
  },
  props: {
    /** 外部传入的设备状态（由 DeviceStatus 组件统一轮询） */
    externalStatus: { type: Object, default: null }
  },
  watch: {
    // 外部状态变化时自动同步（去重轮询的关键）
    externalStatus: {
      handler(val) {
        if (val) this.applyStatus(val)
      },
      deep: true
    }
  },
  mounted() {
    // 只在没有外部数据源时自行轮询
    if (!this.externalStatus) {
      this.fetchDeviceStatus()
      this.refreshTimer = setInterval(() => {
        this.fetchDeviceStatus()
      }, 10000)
    }
  },
  beforeUnmount() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
    }
    if (this.thresholdBlurTimer) {
      clearTimeout(this.thresholdBlurTimer)
    }
  },
  methods: {
    /** 将后端数据应用到本组件状态 */
    applyStatus(data) {
      this.isManualMode = data.manualMode || data.manual || false
      this.deviceStatus.pumpState = data.pumpState || data.pump || false
      this.deviceStatus.fanState = data.fanState || data.fan || false
      this.deviceStatus.lightState = data.lightState || data.light || false
      // 用户正在编辑阈值时，不用后端数据覆盖表单
      if (data.thresholds && !this.isEditingThreshold) {
        Object.assign(this.thresholdForm, this.normalizeThresholds(data.thresholds))
      }
    },
    // 获取设备状态
    async fetchDeviceStatus() {
      try {
        const res = await getDeviceStatus()
        if (res && res.code === 200) {
          this.applyStatus(res.data)
        }
      } catch (error) {
        console.error('获取设备状态失败:', error)
        ElMessage.warning('设备状态刷新失败，请检查网络连接')
      }
    },

    normalizeThresholds(thresholds) {
      const toNumber = (value, fallback) => {
        const num = Number(value)
        return Number.isFinite(num) ? num : fallback
      }

      return {
        fanTemp: toNumber(
          thresholds.fanTemp ?? thresholds.fanTempThreshold ?? thresholds.fan_temp ?? thresholds.fan_temp_threshold,
          this.thresholdForm.fanTemp
        ),
        fanCO2: toNumber(
          thresholds.fanCO2 ?? thresholds.fanCO2Threshold ?? thresholds.fan_co2 ?? thresholds.fan_co2_threshold,
          this.thresholdForm.fanCO2
        ),
        pumpDrought: toNumber(
          thresholds.pumpDrought ?? thresholds.pumpDroughtThreshold ?? thresholds.pump_drought ?? thresholds.pump_drought_threshold,
          this.thresholdForm.pumpDrought
        ),
        lightLux: toNumber(
          thresholds.lightLux ?? thresholds.lightLuxThreshold ?? thresholds.light_lux ?? thresholds.light_lux_threshold,
          this.thresholdForm.lightLux
        )
      }
    },

    // 切换模式
    async handleModeChange(value) {
      this.modeLoading = true
      try {
        const res = await setMode(value)
        if (res && res.code === 200) {
          ElMessage.success(`已切换到${value ? '手动' : '自动'}模式`)
          this.$emit('mode-changed', value)
        } else {
          ElMessage.error('模式切换失败: ' + (res?.message || '未知错误'))
          this.isManualMode = !value
        }
      } catch (error) {
        console.error('模式切换异常:', error)
        ElMessage.error('模式切换失败：' + (error.response?.data?.message || error.message || '网络错误'))
        this.isManualMode = !value
      } finally {
        this.modeLoading = false
      }
    },

    // 控制水泵
    async handlePumpChange(value) {
      this.pumpLoading = true
      try {
        const res = await controlPump(value)
        if (res && res.code === 200) {
          ElMessage.success(`水泵已${value ? '开启' : '关闭'}`)
          this.$emit('device-changed', { device: 'pump', state: value })
        } else {
          ElMessage.error('水泵控制失败: ' + (res?.message || '未知错误'))
          this.deviceStatus.pumpState = !value
        }
      } catch (error) {
        console.error('水泵控制异常:', error)
        ElMessage.error('水泵控制失败：' + (error.response?.data?.message || error.message || '网络错误'))
        this.deviceStatus.pumpState = !value
      } finally {
        this.pumpLoading = false
      }
    },

    // 控制风扇
    async handleFanChange(value) {
      this.fanLoading = true
      try {
        const res = await controlFan(value)
        if (res && res.code === 200) {
          ElMessage.success(`风扇已${value ? '开启' : '关闭'}`)
          this.$emit('device-changed', { device: 'fan', state: value })
        } else {
          ElMessage.error('风扇控制失败: ' + (res?.message || '未知错误'))
          this.deviceStatus.fanState = !value
        }
      } catch (error) {
        console.error('风扇控制异常:', error)
        ElMessage.error('风扇控制失败：' + (error.response?.data?.message || error.message || '网络错误'))
        this.deviceStatus.fanState = !value
      } finally {
        this.fanLoading = false
      }
    },

    // 控制照明
    async handleLightChange(value) {
      this.lightLoading = true
      try {
        const res = await controlLight(value)
        if (res && res.code === 200) {
          ElMessage.success(`照明已${value ? '开启' : '关闭'}`)
          this.$emit('device-changed', { device: 'light', state: value })
        } else {
          ElMessage.error('照明控制失败: ' + (res?.message || '未知错误'))
          this.deviceStatus.lightState = !value
        }
      } catch (error) {
        console.error('照明控制异常:', error)
        ElMessage.error('照明控制失败：' + (error.response?.data?.message || error.message || '网络错误'))
        this.deviceStatus.lightState = !value
      } finally {
        this.lightLoading = false
      }
    },

    // 提交阈值设置
    async handleThresholdSubmit() {
      // 验证阈值参数
      if (!this.validateThresholds()) {
        return
      }
      
      this.thresholdLoading = true
      try {
        const res = await setThresholds(this.thresholdForm)
        if (res && res.code === 200) {
          ElMessage.success('阈值设置已保存并已下发到设备')
          // 保存成功后解除编辑锁定，允许远程数据同步
          this.isEditingThreshold = false
          if (this.thresholdBlurTimer) {
            clearTimeout(this.thresholdBlurTimer)
            this.thresholdBlurTimer = null
          }
          this.$emit('threshold-changed', this.thresholdForm)
        } else {
          ElMessage.error('阈值设置失败: ' + (res?.message || '未知错误'))
        }
      } catch (error) {
        console.error('阈值设置异常:', error)
        ElMessage.error('阈值设置失败：' + (error.response?.data?.message || error.message || '网络错误'))
      } finally {
        this.thresholdLoading = false
      }
    },

    // 验证阈值
    validateThresholds() {
      if (this.thresholdForm.fanTemp < 0 || this.thresholdForm.fanTemp > 60) {
        ElMessage.error('风扇温度阈值应该在 0-60℃ 之间')
        return false
      }
      if (this.thresholdForm.fanCO2 < 400 || this.thresholdForm.fanCO2 > 5000) {
        ElMessage.error('风扇CO₂阈值应该在 400-5000ppm 之间')
        return false
      }
      if (this.thresholdForm.pumpDrought < 0 || this.thresholdForm.pumpDrought > 4095) {
        ElMessage.error('水泵干旱阈值应该在 0-4095ADC 之间')
        return false
      }
      if (this.thresholdForm.lightLux < 0 || this.thresholdForm.lightLux > 100000) {
        ElMessage.error('光照阈值应该在 0-100000lux 之间')
        return false
      }
      return true
    },

    // 重置阈值
    resetThresholds() {
      this.thresholdForm = {
        fanTemp: 32,
        fanCO2: 1200,
        pumpDrought: 3300,
        lightLux: 700
      }
      ElMessage.info('已重置为默认值')
    },

    // 鼠标进入阈值区域——锁定编辑状态，防止轮询覆盖
    onThresholdAreaEnter() {
      if (this.thresholdBlurTimer) {
        clearTimeout(this.thresholdBlurTimer)
        this.thresholdBlurTimer = null
      }
      this.isEditingThreshold = true
    },

    // 鼠标离开阈值区域——延迟解锁
    onThresholdAreaLeave() {
      if (this.thresholdBlurTimer) {
        clearTimeout(this.thresholdBlurTimer)
      }
      this.thresholdBlurTimer = setTimeout(() => {
        this.isEditingThreshold = false
        this.thresholdBlurTimer = null
      }, 5000) // 鼠标离开后5秒再允许远程数据同步
    },

    // 阈值数值变化时（点击加减号或手动输入）——锁定并重置定时器
    onThresholdValueChange() {
      this.isEditingThreshold = true
      if (this.thresholdBlurTimer) {
        clearTimeout(this.thresholdBlurTimer)
      }
      this.thresholdBlurTimer = setTimeout(() => {
        this.isEditingThreshold = false
        this.thresholdBlurTimer = null
      }, 5000) // 值变化后5秒内无操作才允许远程数据同步
    }
  }
}
</script>

<style scoped>
.device-control-panel {
  width: 100%;
}

.control-card {
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

.control-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.control-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 15px;
}

.mode-switch {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  margin-bottom: 15px;
}

.switch-label {
  font-size: 14px;
  color: #606266;
}

.mode-description {
  margin-top: 15px;
}

.device-switches {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.switch-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.switch-item:hover {
  background: #f0f2f5;
}

.switch-icon {
  font-size: 32px;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
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

.switch-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.switch-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.switch-status {
  font-size: 12px;
  color: #909399;
}

.threshold-form {
  margin-top: 15px;
}

.threshold-input-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.range-separator {
  color: #909399;
  font-weight: bold;
}

.unit {
  color: #606266;
  font-size: 14px;
  min-width: 30px;
}

:deep(.el-input-number) {
  width: 140px;
}
</style>
