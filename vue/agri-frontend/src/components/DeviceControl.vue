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

        <!-- 高级模式切换器 -->
        <div class="mode-switcher">
          <div
            class="mode-option"
            :class="{ active: !isManualMode, loading: modeLoading && isManualMode }"
            @click="!modeLoading && isManualMode && handleModeChange(false)"
          >
            <div class="mode-icon-wrapper auto">
              <svg class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="3"/>
                <path d="M12 1v2m0 18v2M4.22 4.22l1.42 1.42m12.72 12.72l1.42 1.42M1 12h2m18 0h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
              </svg>
              <div class="mode-pulse"></div>
            </div>
            <div class="mode-text">
              <span class="mode-name">自动模式</span>
              <span class="mode-desc">智能环境调控</span>
            </div>
            <div v-if="!isManualMode" class="mode-active-indicator">
              <span class="indicator-dot"></span>
              <span>运行中</span>
            </div>
          </div>

          <div class="mode-divider">
            <div class="divider-line"></div>
            <div class="divider-icon" :class="{ switching: modeLoading }">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M7 16V4m0 0L3 8m4-4l4 4M17 8v12m0 0l4-4m-4 4l-4-4"/>
              </svg>
            </div>
            <div class="divider-line"></div>
          </div>

          <div
            class="mode-option"
            :class="{ active: isManualMode, loading: modeLoading && !isManualMode }"
            @click="!modeLoading && !isManualMode && handleModeChange(true)"
          >
            <div class="mode-icon-wrapper manual">
              <svg class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
              </svg>
            </div>
            <div class="mode-text">
              <span class="mode-name">手动模式</span>
              <span class="mode-desc">精准设备控制</span>
            </div>
            <div v-if="isManualMode" class="mode-active-indicator warning">
              <span class="indicator-dot"></span>
              <span>已启用</span>
            </div>
          </div>
        </div>

        <div class="mode-status-bar">
          <div class="status-content" :class="isManualMode ? 'manual' : 'auto'">
            <svg v-if="!isManualMode" class="status-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z" opacity="0.3"/>
              <path d="M12 6v6l4 2"/>
            </svg>
            <svg v-else class="status-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/>
              <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
            <span class="status-text">
              {{ isManualMode ? '手动模式已启用，设备需手动操作' : '自动模式运行中，系统根据阈值智能调控' }}
            </span>
          </div>
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
    async handleModeChange(toManual) {
      this.modeLoading = true
      try {
        const res = await setMode(toManual)
        if (res && res.code === 200) {
          this.isManualMode = toManual
          ElMessage.success(`已切换到${toManual ? '手动' : '自动'}模式`)
          this.$emit('mode-changed', toManual)
        } else {
          ElMessage.error('模式切换失败: ' + (res?.message || '未知错误'))
        }
      } catch (error) {
        console.error('模式切换异常:', error)
        ElMessage.error('模式切换失败：' + (error.response?.data?.message || error.message || '网络错误'))
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
/* Design tokens: 智慧农业主题
   Primary: #1a472a (深森林绿)  Accent: #3a7d44 (森林绿)
   Warning: #d97706 (琥珀橙)    Surface: #f0fdf4 (薄荷绿)
   Text: #1e293b / #475569 / #94a3b8
   Border: rgba(71, 85, 99, 0.1) */

.device-control-panel {
  width: 100%;
}

.control-card {
  border-radius: 16px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: none;
  overflow: hidden;
}

.control-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #1a472a, #3a7d44, #22c55e);
  opacity: 0.8;
}

.control-card:hover {
  box-shadow: 0 8px 32px rgba(26, 71, 42, 0.1);
}

:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(71, 85, 99, 0.08);
  background: linear-gradient(180deg, rgba(240, 253, 244, 0.5) 0%, transparent 100%);
}

:deep(.el-card__body) {
  padding: 18px 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 700;
  color: #1a472a;
  letter-spacing: -0.01em;
}

.header-title .el-icon {
  font-size: 18px;
  color: #3a7d44;
}

.control-section {
  margin-bottom: 22px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(71, 85, 99, 0.08);
}

.control-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 14px;
}

.section-title .el-icon {
  font-size: 14px;
  color: #3a7d44;
}

/* ========== 高级模式切换器 ========== */
.mode-switcher {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  padding: 8px;
  border: 1px solid rgba(71, 85, 99, 0.1);
}

.mode-option {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.mode-option::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.3s;
}

.mode-option:hover:not(.active) {
  background: rgba(255, 255, 255, 0.8);
}

.mode-option.active {
  background: #fff;
  box-shadow: 0 4px 20px rgba(26, 71, 42, 0.12);
}

.mode-option.active::before {
  opacity: 1;
  background: linear-gradient(135deg, transparent 60%, rgba(58, 125, 68, 0.05) 100%);
}

.mode-option.loading {
  pointer-events: none;
  opacity: 0.6;
}

.mode-icon-wrapper {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.3s;
}

.mode-icon-wrapper.auto {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #15803d;
}

.mode-icon-wrapper.manual {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #b45309;
}

.mode-option.active .mode-icon-wrapper.auto {
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.4);
}

.mode-option.active .mode-icon-wrapper.manual {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
}

.mode-icon {
  width: 22px;
  height: 22px;
  position: relative;
  z-index: 1;
}

.mode-pulse {
  position: absolute;
  inset: -4px;
  border-radius: 16px;
  opacity: 0;
  animation: none;
}

.mode-option.active .mode-pulse {
  animation: mode-pulse 2s ease-out infinite;
}

.mode-option.active .mode-icon-wrapper.auto .mode-pulse {
  background: rgba(34, 197, 94, 0.3);
}

.mode-option.active .mode-icon-wrapper.manual .mode-pulse {
  background: rgba(245, 158, 11, 0.3);
}

@keyframes mode-pulse {
  0% {
    transform: scale(1);
    opacity: 0.6;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

.mode-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  overflow: hidden;
}

.mode-name {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  transition: color 0.3s;
  white-space: nowrap;
}

.mode-option.active .mode-name {
  color: #1e293b;
}

.mode-desc {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mode-active-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #15803d;
  white-space: nowrap;
  flex-shrink: 0;
}

.mode-active-indicator.warning {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #b45309;
}

.indicator-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: indicator-blink 1.5s ease-in-out infinite;
}

@keyframes indicator-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* 分割线 - 水平布局 */
.mode-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  gap: 10px;
}

.divider-line {
  height: 1px;
  flex: 1;
  background: linear-gradient(90deg, transparent, rgba(71, 85, 99, 0.15), transparent);
}

.divider-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(71, 85, 99, 0.08);
  color: #94a3b8;
  transition: all 0.3s;
  flex-shrink: 0;
}

.divider-icon svg {
  width: 14px;
  height: 14px;
}

.divider-icon.switching {
  animation: divider-spin 1s linear infinite;
  background: rgba(58, 125, 68, 0.1);
  color: #3a7d44;
}

@keyframes divider-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(180deg); }
}

/* 状态栏 */
.mode-status-bar {
  margin-top: 12px;
  border-radius: 10px;
  overflow: hidden;
}

.status-content {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.3s;
}

.status-content.auto {
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.9), rgba(220, 252, 231, 0.9));
  color: #166534;
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.status-content.manual {
  background: linear-gradient(135deg, rgba(254, 252, 232, 0.9), rgba(254, 249, 195, 0.9));
  color: #a16207;
  border: 1px solid rgba(234, 179, 8, 0.3);
}

.status-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.status-text {
  flex: 1;
  line-height: 1.4;
}

.device-switches {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.switch-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  border: 1px solid rgba(71, 85, 99, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.switch-item:hover {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(26, 71, 42, 0.06);
}

.switch-icon {
  font-size: 24px;
  width: 46px;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  transition: all 0.3s;
}

.pump-icon {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
}

.fan-icon {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.15);
}

.light-icon {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.15);
}

.switch-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.switch-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.switch-status {
  font-size: 11px;
  color: #94a3b8;
}

.threshold-form {
  margin-top: 12px;
}

.threshold-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.threshold-form :deep(.el-form-item__label) {
  font-size: 13px;
  color: #475569;
  font-weight: 500;
}

.threshold-input-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unit {
  color: #94a3b8;
  font-size: 12px;
  min-width: 28px;
  font-weight: 500;
}

.threshold-desc {
  font-size: 11px !important;
  color: #94a3b8 !important;
  line-height: 1.5;
  margin-top: 4px;
}

:deep(.el-input-number) {
  width: 140px;
}

:deep(.el-tag) {
  border: none;
  font-size: 11px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 20px;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
  color: #15803d;
}

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #b45309;
}

:deep(.el-switch__core) {
  border-radius: 12px;
  height: 24px;
}

:deep(.el-switch__action) {
  width: 20px;
  height: 20px;
}
</style>
