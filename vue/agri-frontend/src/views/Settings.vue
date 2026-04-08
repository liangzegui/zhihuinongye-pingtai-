<template>
  <div class="settings-page">
    <div class="settings-content">
      <div class="page-header">
        <h2><el-icon><Setting /></el-icon> 系统设置</h2>
        <p class="subtitle">配置系统参数和设备连接</p>
      </div>

      <el-tabs v-model="activeTab" class="settings-tabs">
      <el-tab-pane label="设备连接" name="device">
        <Esp32Config />
      </el-tab-pane>
      
      <el-tab-pane label="数据保存" name="autosave">
        <DataAutoSaveConfig />
      </el-tab-pane>
      
      <el-tab-pane label="离线缓存" name="cache">
        <CacheConfig />
      </el-tab-pane>
      
      <el-tab-pane label="预警阈值" name="warning-rules">
        <WarningRuleConfig />
      </el-tab-pane>

      <el-tab-pane label="异常设置" name="exception">
        <ExceptionConfig />
      </el-tab-pane>
    </el-tabs>
    </div>
  </div>
</template>

<script>
import { Setting } from '@element-plus/icons-vue';
import Esp32Config from '@/components/Esp32Config.vue';
import DataAutoSaveConfig from '@/components/DataAutoSaveConfig.vue';
import CacheConfig from '@/components/CacheConfig.vue';
import WarningRuleConfig from '@/components/WarningRuleConfig.vue';
import ExceptionConfig from '@/components/ExceptionConfig.vue';

export default {
  name: 'SystemSettings',
  components: {
    Setting,
    Esp32Config,
    DataAutoSaveConfig,
    CacheConfig,
    WarningRuleConfig,
    ExceptionConfig
  },
  data() {
    return {
      activeTab: 'device',
      isAdmin: localStorage.getItem('agri_platform_role') === 'admin'
    };
  }
};
</script>

<style scoped>
/* ========== 智慧农业主题设计 ========== */
/* Primary: #1a472a (深森林绿) Accent: #3a7d44 (森林绿)
   Secondary: #0f766e (青色) Surface: #f0fdf4 (薄荷绿) */

.settings-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0fdfa 100%);
  position: relative;
}

.settings-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 50%;
  height: 50%;
  background: radial-gradient(ellipse at top left, rgba(58, 125, 68, 0.06) 0%, transparent 70%);
  pointer-events: none;
}

.settings-content {
  max-width: 900px;
  margin: 0 auto;
  position: relative;
  z-index: 10;
}

/* ========== Page Header ========== */
.page-header {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
  padding: 24px 28px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  box-shadow: 0 4px 20px rgba(26, 71, 42, 0.06);
  position: relative;
  z-index: 10;
}

.page-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #1a472a, #3a7d44, #22c55e);
  border-radius: 16px 16px 0 0;
}

.page-header h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 6px 0;
  font-size: 22px;
  font-weight: 700;
  color: #1a472a;
  letter-spacing: -0.02em;
}

.subtitle {
  color: #64748b;
  margin: 0;
  font-size: 14px;
}

/* ========== Settings Tabs ========== */
.settings-tabs {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  box-shadow: 0 4px 20px rgba(26, 71, 42, 0.06);
}

:deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(71, 85, 99, 0.1);
}

:deep(.el-tabs__item) {
  color: #64748b;
  font-weight: 500;
  transition: all 0.3s;
}

:deep(.el-tabs__item:hover) {
  color: #3a7d44;
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

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .settings-page {
    padding: 16px;
  }

  .page-header {
    padding: 18px 20px;
  }

  .page-header h2 {
    font-size: 18px;
  }

  .settings-tabs {
    padding: 15px;
  }
}
</style>
