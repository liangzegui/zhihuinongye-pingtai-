<template>
  <div class="settings-page">
    <div class="page-header">
      <h2><el-icon><Setting /></el-icon> 系统设置</h2>
      <p class="subtitle">配置系统参数和设备连接</p>
    </div>
    
    <el-tabs v-model="activeTab" class="settings-tabs">
      <el-tab-pane label="设备连接" name="device">
        <Esp32Config />
      </el-tab-pane>
      
      <el-tab-pane v-if="isAdmin" label="数据保存" name="autosave">
        <DataAutoSaveConfig />
      </el-tab-pane>

      <el-tab-pane v-else label="数据保存" name="autosave-readonly">
        <el-card>
          <el-empty description="仅管理员可管理数据自动保存配置" />
        </el-card>
      </el-tab-pane>
      
      <el-tab-pane label="预警阈值" name="warning-rules">
        <WarningRuleConfig />
      </el-tab-pane>

      <el-tab-pane label="异常设置" name="exception">
        <ExceptionConfig />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { Setting } from '@element-plus/icons-vue';
import Esp32Config from '@/components/Esp32Config.vue';
import DataAutoSaveConfig from '@/components/DataAutoSaveConfig.vue';
import WarningRuleConfig from '@/components/WarningRuleConfig.vue';
import ExceptionConfig from '@/components/ExceptionConfig.vue';

export default {
  name: 'SystemSettings',
  components: {
    Setting,
    Esp32Config,
    DataAutoSaveConfig,
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
.settings-page {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 5px 0;
  color: #303133;
}

.subtitle {
  color: #909399;
  margin: 0;
  font-size: 14px;
}

.settings-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 15px;
}
</style>
