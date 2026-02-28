<template>
  <div class="esp32-config">
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><Setting /></el-icon> 单片机连接配置</span>
          <el-tag :type="connected ? 'success' : 'danger'" size="small">
            {{ connected ? '已连接' : '未连接' }}
          </el-tag>
        </div>
      </template>
      
      <el-form :model="form" label-width="120px" class="config-form">
        <el-form-item label="ESP32 地址">
          <el-input 
            v-model="form.baseUrl" 
            placeholder="请输入单片机IP地址，如: 192.168.1.100"
            clearable
          >
            <template #prepend>http://</template>
          </el-input>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="saveConfig" :loading="saving">
            <el-icon><Check /></el-icon> 保存配置
          </el-button>
          <el-button @click="testConnection" :loading="testing">
            <el-icon><Connection /></el-icon> 测试连接
          </el-button>
          <el-button @click="refreshConfig">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </el-form-item>
      </el-form>
      
      <el-divider />
      
      <div class="tips">
        <h4><el-icon><InfoFilled /></el-icon> 使用说明</h4>
        <ul>
          <li>请确保单片机(ESP32)与本机在同一局域网内</li>
          <li>输入单片机的IP地址（不需要输入 http:// 前缀）</li>
          <li>保存后点击"测试连接"验证配置是否正确</li>
          <li>如果连接失败，请检查：
            <ul>
              <li>单片机是否已开机并连接到WiFi</li>
              <li>IP地址是否正确</li>
              <li>防火墙是否阻止了连接</li>
            </ul>
          </li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getEsp32Config, updateEsp32Config, testEsp32Connection } from '@/api/config';
import { Setting, Check, Connection, Refresh, InfoFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

export default {
  name: 'Esp32Config',
  components: {
    Setting, Check, Connection, Refresh, InfoFilled
  },
  data() {
    return {
      form: {
        baseUrl: ''
      },
      connected: false,
      saving: false,
      testing: false
    };
  },
  mounted() {
    this.refreshConfig();
  },
  methods: {
    // 刷新配置
    async refreshConfig() {
      try {
        const res = await getEsp32Config();
        const data = res?.data?.data || res?.data || {};
        // 移除 http:// 前缀显示
        let url = data.baseUrl || '';
        if (url.startsWith('http://')) {
          url = url.substring(7);
        } else if (url.startsWith('https://')) {
          url = url.substring(8);
        }
        this.form.baseUrl = url;
        this.connected = data.connected || false;
      } catch (err) {
        console.error('获取配置失败', err);
        ElMessage.error('获取配置失败');
      }
    },
    
    // 保存配置
    async saveConfig() {
      if (!this.form.baseUrl.trim()) {
        ElMessage.warning('请输入单片机IP地址');
        return;
      }
      
      this.saving = true;
      try {
        const res = await updateEsp32Config(this.form.baseUrl.trim());
        const data = res?.data?.data || res?.data || {};
        this.connected = data.connected || false;
        
        if (data.connected) {
          ElMessage.success('配置保存成功，连接正常');
        } else {
          ElMessage.warning(data.message || '配置已保存，但连接失败');
        }
      } catch (err) {
        console.error('保存配置失败', err);
        ElMessage.error('保存配置失败');
      } finally {
        this.saving = false;
      }
    },
    
    // 测试连接
    async testConnection() {
      this.testing = true;
      try {
        const res = await testEsp32Connection();
        const data = res?.data?.data || res?.data || {};
        this.connected = data.connected || false;
        
        if (data.connected) {
          ElMessage.success('连接成功！单片机在线');
        } else {
          ElMessage.error(data.message || '连接失败，请检查设备');
        }
      } catch (err) {
        console.error('测试连接失败', err);
        this.connected = false;
        ElMessage.error('测试连接失败');
      } finally {
        this.testing = false;
      }
    }
  }
};
</script>

<style scoped>
.esp32-config {
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

.config-form {
  margin-top: 10px;
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

.tips ul ul {
  margin-top: 5px;
}
</style>
