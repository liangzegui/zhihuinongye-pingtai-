<template>
  <div class="exception-config">
    <el-alert 
      title="异常设置用于配置系统如何检测、通知和处理传感器数据异常，与预警阈值设置分开管理"
      type="info" :closable="false" show-icon style="margin-bottom: 20px;" />

    <div v-loading="loading">
      <!-- ==================== 检测设置 ==================== -->
      <el-card class="config-section" shadow="hover">
        <template #header>
          <div class="section-header">
            <span><el-icon><Monitor /></el-icon> 检测设置</span>
            <el-tag size="small" :type="config.detection_enabled === 'true' ? 'success' : 'danger'">
              {{ config.detection_enabled === 'true' ? '已启用' : '已禁用' }}
            </el-tag>
          </div>
        </template>

        <el-form label-width="160px" label-position="left">
          <el-form-item label="异常检测总开关">
            <el-switch
              v-model="config.detection_enabled"
              active-value="true" inactive-value="false"
              active-text="启用" inactive-text="禁用"
              @change="onConfigChange('detection_enabled', $event)" />
          </el-form-item>

          <el-form-item label="检测频率">
            <el-input-number
              v-model.number="detectionInterval"
              :min="10" :max="3600" :step="10"
              :disabled="config.detection_enabled !== 'true'"
              @change="onConfigChange('detection_interval', String($event))" />
            <span class="unit-text">秒（最小10秒，最大3600秒）</span>
          </el-form-item>

          <el-divider content-position="left">各传感器检测开关</el-divider>

          <el-form-item label="温度检测">
            <el-switch v-model="config.detection_temp_enabled"
              active-value="true" inactive-value="false"
              :disabled="config.detection_enabled !== 'true'"
              @change="onConfigChange('detection_temp_enabled', $event)" />
          </el-form-item>
          <el-form-item label="湿度检测">
            <el-switch v-model="config.detection_humidity_enabled"
              active-value="true" inactive-value="false"
              :disabled="config.detection_enabled !== 'true'"
              @change="onConfigChange('detection_humidity_enabled', $event)" />
          </el-form-item>
          <el-form-item label="土壤湿度检测">
            <el-switch v-model="config.detection_soil_enabled"
              active-value="true" inactive-value="false"
              :disabled="config.detection_enabled !== 'true'"
              @change="onConfigChange('detection_soil_enabled', $event)" />
          </el-form-item>
          <el-form-item label="光照检测">
            <el-switch v-model="config.detection_light_enabled"
              active-value="true" inactive-value="false"
              :disabled="config.detection_enabled !== 'true'"
              @change="onConfigChange('detection_light_enabled', $event)" />
          </el-form-item>
          <el-form-item label="CO₂检测">
            <el-switch v-model="config.detection_co2_enabled"
              active-value="true" inactive-value="false"
              :disabled="config.detection_enabled !== 'true'"
              @change="onConfigChange('detection_co2_enabled', $event)" />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- ==================== 通知设置 ==================== -->
      <el-card class="config-section" shadow="hover">
        <template #header>
          <div class="section-header">
            <span><el-icon><Bell /></el-icon> 通知设置</span>
          </div>
        </template>

        <el-form label-width="160px" label-position="left">
          <el-form-item label="WebSocket实时推送">
            <el-switch v-model="config.notify_websocket"
              active-value="true" inactive-value="false"
              active-text="开启" inactive-text="关闭"
              @change="onConfigChange('notify_websocket', $event)" />
            <span class="help-text">异常触发时通过WebSocket推送到前端</span>
          </el-form-item>

          <el-form-item label="声音提醒">
            <el-switch v-model="config.notify_sound"
              active-value="true" inactive-value="false"
              active-text="开启" inactive-text="关闭"
              @change="onConfigChange('notify_sound', $event)" />
            <span class="help-text">异常弹窗弹出时播放提示音</span>
          </el-form-item>

          <el-form-item label="弹窗显示时长">
            <el-input-number
              v-model.number="popupDuration"
              :min="1" :max="60" :step="1"
              @change="onConfigChange('notify_popup_duration', String($event))" />
            <span class="unit-text">秒</span>
          </el-form-item>

          <el-form-item label="重复异常通知">
            <el-switch v-model="config.notify_repeat"
              active-value="true" inactive-value="false"
              active-text="每次通知" inactive-text="仅首次"
              @change="onConfigChange('notify_repeat', $event)" />
            <span class="help-text">冷却期内同类异常是否重复弹窗</span>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- ==================== 处理设置 ==================== -->
      <el-card class="config-section" shadow="hover">
        <template #header>
          <div class="section-header">
            <span><el-icon><Tools /></el-icon> 处理设置</span>
          </div>
        </template>

        <el-form label-width="160px" label-position="left">
          <el-form-item label="自动处理异常">
            <el-switch v-model="config.handling_auto_handle"
              active-value="true" inactive-value="false"
              active-text="自动" inactive-text="手动"
              @change="onConfigChange('handling_auto_handle', $event)" />
            <span class="help-text">开启后系统自动标记低级别异常为已处理</span>
          </el-form-item>

          <el-form-item label="异常冷却时间">
            <el-input-number
              v-model.number="cooldown"
              :min="1" :max="60" :step="1"
              @change="onConfigChange('handling_cooldown', String($event))" />
            <span class="unit-text">分钟（同类型异常不会在冷却期内重复记录）</span>
          </el-form-item>

          <el-form-item label="最大日志保留数">
            <el-input-number
              v-model.number="maxLogs"
              :min="100" :max="10000" :step="100"
              @change="onConfigChange('handling_max_logs', String($event))" />
            <span class="unit-text">条（超过后自动清理最早记录）</span>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- ==================== 严重程度设置 ==================== -->
      <el-card class="config-section" shadow="hover">
        <template #header>
          <div class="section-header">
            <span><el-icon><Warning /></el-icon> 严重程度分级</span>
          </div>
        </template>

        <el-alert type="warning" :closable="false" style="margin-bottom: 15px;"
          title="根据传感器实际值超出阈值的比例自动划分异常等级" />

        <el-form label-width="160px" label-position="left">
          <el-form-item>
            <template #label>
              <el-tag type="warning" size="small">⚠ 警告</el-tag> 比例
            </template>
            <el-input-number
              v-model.number="warningRatio"
              :min="0.1" :max="10" :step="0.1" :precision="1"
              @change="onConfigChange('severity_warning_ratio', String($event))" />
            <span class="unit-text">倍 — 超出阈值 ≤ 该比例为"警告"</span>
          </el-form-item>

          <el-form-item>
            <template #label>
              <el-tag type="danger" size="small">🔴 危险</el-tag> 比例
            </template>
            <el-input-number
              v-model.number="dangerRatio"
              :min="0.1" :max="10" :step="0.1" :precision="1"
              @change="onConfigChange('severity_danger_ratio', String($event))" />
            <span class="unit-text">倍 — 超出阈值 ≤ 该比例为"危险"</span>
          </el-form-item>

          <el-form-item>
            <template #label>
              <el-tag type="danger" size="small" effect="dark">🚨 严重</el-tag> 比例
            </template>
            <el-input-number
              v-model.number="criticalRatio"
              :min="0.1" :max="10" :step="0.1" :precision="1"
              @change="onConfigChange('severity_critical_ratio', String($event))" />
            <span class="unit-text">倍 — 超出阈值 > 该比例为"严重"</span>
          </el-form-item>

          <el-divider />
          <div class="severity-example">
            <p><strong>示例：</strong>温度阈值最高 35°C，当前温度 42°C，超出 7°C</p>
            <p>超出比例 = 7 / 35 ≈ 0.2 倍</p>
            <ul>
              <li>0.2 ≤ <strong>{{ warningRatio }}</strong> → 级别：<el-tag type="warning" size="small">警告</el-tag></li>
              <li>若超出比例 > <strong>{{ warningRatio }}</strong> 且 ≤ <strong>{{ dangerRatio }}</strong> → <el-tag type="danger" size="small">危险</el-tag></li>
              <li>若超出比例 > <strong>{{ dangerRatio }}</strong> → <el-tag type="danger" size="small" effect="dark">严重</el-tag></li>
            </ul>
          </div>
        </el-form>
      </el-card>

      <!-- ==================== 操作按钮 ==================== -->
      <div class="action-bar">
        <el-button type="primary" :icon="Check" @click="saveAll" :loading="saving">
          保存所有设置
        </el-button>
        <el-button :icon="RefreshLeft" @click="resetDefaults" :loading="resetting">
          恢复默认设置
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { Monitor, Bell, Tools, Warning, Check, RefreshLeft } from '@element-plus/icons-vue';
import { getExceptionConfig, updateExceptionConfig, resetExceptionConfig } from '@/api/exceptionConfig';
import { ElMessage, ElMessageBox } from 'element-plus';

export default {
  name: 'ExceptionConfig',
  components: { Monitor, Bell, Tools, Warning },
  setup() {
    return { Check, RefreshLeft };
  },
  data() {
    return {
      loading: false,
      saving: false,
      resetting: false,
      config: {
        detection_enabled: 'true',
        detection_interval: '60',
        detection_temp_enabled: 'true',
        detection_humidity_enabled: 'true',
        detection_soil_enabled: 'true',
        detection_light_enabled: 'true',
        detection_co2_enabled: 'true',
        notify_websocket: 'true',
        notify_sound: 'true',
        notify_popup_duration: '8',
        notify_repeat: 'false',
        handling_auto_handle: 'false',
        handling_cooldown: '5',
        handling_max_logs: '1000',
        severity_warning_ratio: '1.0',
        severity_danger_ratio: '1.5',
        severity_critical_ratio: '2.0'
      },
      // 数字类型的临时变量（el-input-number需要number类型）
      detectionInterval: 60,
      popupDuration: 8,
      cooldown: 5,
      maxLogs: 1000,
      warningRatio: 1.0,
      dangerRatio: 1.5,
      criticalRatio: 2.0,
      pendingChanges: {} // 待保存的变更
    };
  },
  mounted() {
    this.loadConfig();
  },
  methods: {
    /** 加载配置 */
    async loadConfig() {
      this.loading = true;
      try {
        const res = await getExceptionConfig();
        if (res && res.code === 200) {
          const grouped = res.data;
          // 将分组数据展平为 key-value
          for (const group of Object.values(grouped)) {
            for (const item of group) {
              if (Object.prototype.hasOwnProperty.call(this.config, item.key)) {
                this.config[item.key] = item.value;
              }
            }
          }
          this.syncNumberFields();
        }
      } catch (e) {
        ElMessage.error('加载异常配置失败');
      } finally {
        this.loading = false;
      }
    },

    /** 同步数字字段 */
    syncNumberFields() {
      this.detectionInterval = parseInt(this.config.detection_interval) || 60;
      this.popupDuration = parseInt(this.config.notify_popup_duration) || 8;
      this.cooldown = parseInt(this.config.handling_cooldown) || 5;
      this.maxLogs = parseInt(this.config.handling_max_logs) || 1000;
      this.warningRatio = parseFloat(this.config.severity_warning_ratio) || 1.0;
      this.dangerRatio = parseFloat(this.config.severity_danger_ratio) || 1.5;
      this.criticalRatio = parseFloat(this.config.severity_critical_ratio) || 2.0;
    },

    /** 配置变更时记录到待保存队列 */
    onConfigChange(key, value) {
      this.config[key] = value;
      this.pendingChanges[key] = value;
    },

    /** 保存所有变更 */
    async saveAll() {
      // 收集所有配置
      const allConfig = { ...this.config };
      this.saving = true;
      try {
        const res = await updateExceptionConfig(allConfig);
        if (res && res.code === 200) {
          ElMessage.success('异常设置已保存');
          this.pendingChanges = {};
        } else {
          ElMessage.error(res?.msg || '保存失败');
        }
      } catch (e) {
        ElMessage.error('保存异常设置失败');
      } finally {
        this.saving = false;
      }
    },

    /** 恢复默认设置 */
    async resetDefaults() {
      try {
        await ElMessageBox.confirm(
          '确定要恢复所有异常设置为默认值吗？',
          '恢复默认', { type: 'warning' }
        );
      } catch {
        return;
      }

      this.resetting = true;
      try {
        const res = await resetExceptionConfig();
        if (res && res.code === 200) {
          ElMessage.success('已恢复默认设置');
          this.pendingChanges = {};
          await this.loadConfig();
        } else {
          ElMessage.error(res?.msg || '恢复失败');
        }
      } catch (e) {
        ElMessage.error('恢复默认设置失败');
      } finally {
        this.resetting = false;
      }
    }
  }
};
</script>

<style scoped>
.exception-config {
  max-width: 800px;
}

.config-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 15px;
}

.section-header span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.unit-text {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}

.help-text {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.severity-example {
  background: #fafafa;
  border-radius: 6px;
  padding: 12px 16px;
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}

.severity-example p {
  margin: 2px 0;
}

.severity-example ul {
  margin: 4px 0 0 0;
  padding-left: 20px;
}

.action-bar {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: 20px 0;
}
</style>
