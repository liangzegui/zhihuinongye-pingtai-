# 华为云IoTDA集成配置指南

## 📋 概述

本指南介绍如何配置华为云IoTDA与您的Spring Boot后端集成，实现：
1. **数据上报**：ESP32传感器数据 → IoTDA → 您的后端 → MySQL数据库
2. **设备控制**：Vue前端 → 后端API → IoTDA → ESP32

---

## 🔧 步骤1：后端配置

### 1.1 在 `application.properties` 或 `application.yml` 中添加IoTDA配置：

**application.properties 格式：**
```properties
# ============ 华为云IoTDA配置 ============
# IoTDA接入地址（根据您的区域调整）
iotda.endpoint=https://iotda.cn-south-1.myhuaweicloud.com

# 项目ID（在华为云控制台获取）
iotda.project-id=您的项目ID

# AK/SK认证密钥（在IAM中创建）
iotda.access-key=您的AccessKey
iotda.secret-key=您的SecretKey

# 设备ID
iotda.device-id=69568516c00ccb6d4b302187_esp32-001

# Webhook安全Token（可自定义，用于验证IoTDA请求）
iotda.webhook-token=your-webhook-security-token
```

**application.yml 格式：**
```yaml
iotda:
  endpoint: https://iotda.cn-south-1.myhuaweicloud.com
  project-id: 您的项目ID
  access-key: 您的AccessKey
  secret-key: 您的SecretKey
  device-id: 69568516c00ccb6d4b302187_esp32-001
  webhook-token: your-webhook-security-token
```

### 1.2 如何获取这些配置值？

#### 项目ID (project-id)
1. 登录华为云控制台
2. 点击右上角用户名 → "我的凭证"
3. 在"项目列表"中找到您的项目ID

#### Access Key / Secret Key
1. 登录华为云控制台
2. 点击右上角用户名 → "我的凭证"
3. 选择"访问密钥" → "新增访问密钥"
4. 下载并保存AK/SK

#### 设备ID
- 就是您ESP32的设备ID：`69568516c00ccb6d4b302187_esp32-001`

---

## 🌐 步骤2：配置IoTDA数据转发规则

### 2.1 登录华为云IoTDA控制台
https://console.huaweicloud.com/iotdm

### 2.2 创建数据转发规则

1. 进入您的IoTDA实例
2. 左侧菜单选择 **"规则" → "数据转发"**
3. 点击 **"创建规则"**

**规则配置：**
- **规则名称**：`forward_to_backend`
- **触发事件**：设备属性上报
- **数据来源**：
  - 产品：选择您的ESP32产品
  - 设备：可选择所有设备或指定设备

### 2.3 设置转发目标

**转发类型：HTTP推送**

- **URL地址**：`http://您的服务器IP:端口/api/iotda/webhook`
  - 例如：`http://123.45.67.89:8080/api/iotda/webhook`
  
⚠️ **重要**：您的后端服务器需要有公网IP或使用内网穿透工具

### 2.4 内网穿透方案（本地开发测试用）

如果您在本地开发，可以使用以下工具：

#### 方案A：使用 ngrok
```bash
# 安装 ngrok
npm install -g ngrok

# 启动后端后，在另一个终端运行
ngrok http 8080
```
ngrok 会提供一个公网URL，例如 `https://abc123.ngrok.io`

将IoTDA的转发地址配置为：`https://abc123.ngrok.io/api/iotda/webhook`

#### 方案B：使用 frp
1. 租用一台云服务器
2. 在云服务器上部署 frps
3. 在本地运行 frpc
4. 将IoTDA转发到云服务器地址

---

## 📱 步骤3：Vue前端集成

### 3.1 设备控制API

在Vue前端中，调用以下API控制设备：

```javascript
// api/device.js
import axios from 'axios'

const BASE_URL = '/api/device'

// 控制水泵
export function controlPump(state) {
  return axios.post(`${BASE_URL}/pump`, { state })
}

// 控制风扇
export function controlFan(state) {
  return axios.post(`${BASE_URL}/fan`, { state })
}

// 控制照明
export function controlLight(state) {
  return axios.post(`${BASE_URL}/light`, { state })
}

// 切换手动/自动模式
export function setMode(manual) {
  return axios.post(`${BASE_URL}/mode`, { manual })
}

// 设置阈值
export function setThreshold(type, value) {
  return axios.post(`${BASE_URL}/threshold`, { type, value })
}

// 获取设备状态
export function getDeviceStatus() {
  return axios.get(`${BASE_URL}/status`)
}
```

### 3.2 在组件中使用

```vue
<template>
  <div class="device-control">
    <el-switch v-model="pumpState" @change="togglePump" active-text="水泵开" inactive-text="水泵关"/>
    <el-switch v-model="fanState" @change="toggleFan" active-text="风扇开" inactive-text="风扇关"/>
    <el-switch v-model="lightState" @change="toggleLight" active-text="照明开" inactive-text="照明关"/>
    <el-switch v-model="manualMode" @change="toggleMode" active-text="手动" inactive-text="自动"/>
  </div>
</template>

<script>
import { controlPump, controlFan, controlLight, setMode, getDeviceStatus } from '@/api/device'

export default {
  data() {
    return {
      pumpState: false,
      fanState: false,
      lightState: false,
      manualMode: false
    }
  },
  
  mounted() {
    this.fetchStatus()
    // 每5秒刷新一次状态
    this.timer = setInterval(this.fetchStatus, 5000)
  },
  
  beforeDestroy() {
    clearInterval(this.timer)
  },
  
  methods: {
    async fetchStatus() {
      const res = await getDeviceStatus()
      if (res.data.code === 200) {
        const data = res.data.data
        this.pumpState = data.pump
        this.fanState = data.fan
        this.lightState = data.light
        this.manualMode = data.manual
      }
    },
    
    async togglePump(state) {
      await controlPump(state)
    },
    
    async toggleFan(state) {
      await controlFan(state)
    },
    
    async toggleLight(state) {
      await controlLight(state)
    },
    
    async toggleMode(manual) {
      await setMode(manual)
    }
  }
}
</script>
```

---

## 🔄 步骤4：验证数据流

### 4.1 测试Webhook接收

启动后端后，访问健康检查接口：
```
GET http://localhost:8080/api/iotda/health
```

应返回：
```json
{
  "code": 200,
  "message": "IoTDA Webhook service is running",
  "data": null
}
```

### 4.2 模拟IoTDA数据推送

使用Postman或curl测试webhook：

```bash
curl -X POST http://localhost:8080/api/iotda/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "notify_data": {
      "body": {
        "services": [{
          "service_id": "default",
          "properties": {
            "temp": 25.5,
            "humi": 60.0,
            "soil": 45,
            "lightLux": 1200,
            "eco2": 450,
            "pump": false,
            "fan": false,
            "light": false
          }
        }]
      }
    }
  }'
```

### 4.3 查看数据库

检查 `t_env_data` 表是否有新数据：
```sql
SELECT * FROM t_env_data ORDER BY collect_time DESC LIMIT 10;
```

---

## 🐛 常见问题

### Q1: IoTDA数据无法转发到后端？
- 确认后端服务器有公网IP或使用了内网穿透
- 检查防火墙是否开放端口
- 在IoTDA控制台查看规则执行日志

### Q2: 设备控制命令发送失败？
- 确认AK/SK配置正确
- 确认设备在线
- 查看后端控制台日志

### Q3: 前端显示数据不更新？
- 确认后端接收到了IoTDA推送
- 检查数据库中是否有新数据
- 查看前端网络请求是否正常

---

## 📊 数据流示意图

```
┌─────────────┐                    ┌─────────────┐                    ┌─────────────┐
│   ESP32     │ ── MQTT上报 ──→   │   IoTDA     │ ── HTTP推送 ──→   │  Spring Boot │
│  传感器数据  │                    │   平台      │                    │   后端       │
└─────────────┘                    └─────────────┘                    └──────┬──────┘
                                                                             │
                                                                             ↓
┌─────────────┐                    ┌─────────────┐                    ┌─────────────┐
│   ESP32     │ ← 属性下发 ────   │   IoTDA     │ ← API调用 ────    │   MySQL     │
│  控制执行   │                    │   平台      │                    │   数据库    │
└─────────────┘                    └─────────────┘                    └─────────────┘
                                                                             ↑
                                                                             │
                                   ┌─────────────┐                    ┌──────┴──────┐
                                   │   Vue       │ ── HTTP请求 ──→   │  Spring Boot │
                                   │   前端      │                    │   后端       │
                                   └─────────────┘                    └─────────────┘
```

---

## ✅ 集成完成检查清单

- [ ] 后端添加了IoTDA配置（AK/SK/ProjectID）
- [ ] IoTDA创建了数据转发规则
- [ ] 后端能接收IoTDA的Webhook推送
- [ ] 数据成功写入t_env_data表
- [ ] 前端能调用设备控制API
- [ ] 设备能响应控制命令
