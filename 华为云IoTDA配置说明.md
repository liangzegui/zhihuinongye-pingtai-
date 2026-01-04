# 华为云IoTDA数据转发配置说明

## 📋 概述

您的项目已经有完整的IoTDA Webhook接收代码，只需在华为云控制台配置数据转发规则即可。

---

## 🔧 第一步：配置后端接收地址

### 1.1 您的Webhook接收接口
后端已实现以下接口：

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/iotda/webhook` | POST | 接收IoTDA数据转发 |
| `/api/iotda/direct` | POST | ESP32直接上报（备用） |
| `/api/iotda/health` | GET | 健康检查 |

### 1.2 数据格式
IoTDA会推送以下格式的数据：
```json
{
  "notify_data": {
    "body": {
      "services": [{
        "service_id": "default",
        "properties": {
          "temp": 25.5,
          "humi": 60.0,
          "soil": 2000,
          "lightLux": 1200,
          "eco2": 450
        }
      }]
    }
  }
}
```

---

## 🌐 第二步：获取公网访问地址

### 方案A：使用云服务器（推荐生产环境）

如果您有云服务器（阿里云、腾讯云、华为云等），将后端部署到服务器上：

```bash
# 1. 打包后端
cd idea/agri-backend
mvn clean package -DskipTests

# 2. 上传到服务器并运行
java -jar target/agri-backend-0.0.1-SNAPSHOT.jar
```

Webhook地址：`http://您的服务器IP:8080/api/iotda/webhook`

### 方案B：使用ngrok（推荐开发测试）

1. **下载ngrok**：https://ngrok.com/download
2. **注册并获取authtoken**：https://dashboard.ngrok.com/auth
3. **配置并启动**：
```bash
# 配置authtoken（只需一次）
ngrok authtoken YOUR_AUTH_TOKEN

# 启动隧道
ngrok http 8080
```
4. **获取公网地址**：ngrok会显示类似 `https://xxxx.ngrok.io` 的地址
5. **配置IoTDA**：使用 `https://xxxx.ngrok.io/api/iotda/webhook`

### 方案C：使用花生壳（国内推荐）

1. 下载花生壳客户端：https://hsk.oray.com/
2. 注册账号并实名认证
3. 添加映射：外网端口 → 127.0.0.1:8080
4. 获取外网地址

### 方案D：使用cpolar

1. 下载cpolar：https://www.cpolar.com/
2. 注册账号
3. 启动隧道：`cpolar http 8080`
4. 获取公网地址

---

## 📱 第三步：在华为云IoTDA配置数据转发

### 3.1 登录华为云IoTDA控制台

访问：https://console.huaweicloud.com/iotdm/

### 3.2 进入规则引擎

1. 选择您的IoTDA实例
2. 左侧菜单 → **规则** → **数据转发**
3. 点击 **创建规则**

### 3.3 配置规则

**基本信息：**
| 配置项 | 值 |
|--------|-----|
| 规则名称 | `forward_sensor_data` |
| 规则描述 | 转发ESP32传感器数据到后端 |

**触发条件：**
| 配置项 | 值 |
|--------|-----|
| 数据来源 | 设备属性 |
| 触发事件 | 属性上报 |
| 产品 | 选择您的ESP32产品 |

**转发目标：**
| 配置项 | 值 |
|--------|-----|
| 转发类型 | HTTP推送 |
| 服务器地址 | `https://您的公网地址/api/iotda/webhook` |
| 请求方式 | POST |

### 3.4 启用规则

配置完成后，点击 **启用** 按钮激活规则。

---

## ✅ 第四步：验证配置

### 4.1 测试健康检查
```bash
# 使用curl或浏览器访问
curl http://localhost:8080/api/iotda/health

# 应返回
{"code":200,"message":"success","data":"IoTDA Webhook服务正常运行"}
```

### 4.2 模拟IoTDA推送数据
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
            "soil": 2000,
            "lightLux": 1200,
            "eco2": 450
          }
        }]
      }
    }
  }'
```

### 4.3 查看数据库
```sql
SELECT * FROM t_env_data ORDER BY collect_time DESC LIMIT 10;
```

---

## 📊 数据流程图

```
┌─────────────┐                    ┌─────────────┐                    ┌─────────────┐
│   ESP32     │ ── MQTT上报 ──→   │   IoTDA     │ ── HTTP推送 ──→   │  Spring Boot │
│  传感器数据  │                    │   平台      │                    │   后端       │
└─────────────┘                    └─────────────┘                    └──────┬──────┘
                                                                             │
                                                                             ↓
┌─────────────┐                    ┌─────────────┐                    ┌─────────────┐
│   Vue       │ ── HTTP请求 ──→   │  Spring Boot │ ── 查询 ────→    │   MySQL     │
│   前端      │                    │   后端       │                    │   数据库    │
└─────────────┘                    └─────────────┘                    └─────────────┘
```

---

## 🔑 可选：配置设备控制（双向通信）

如果需要从后端控制ESP32设备，需要在 `application.properties` 中配置IoTDA访问密钥：

```properties
# ============ 华为云IoTDA配置（用于设备控制） ============
# 在华为云控制台 → 我的凭证 → 访问密钥 中获取
iotda.endpoint=https://iotda.cn-south-1.myhuaweicloud.com
iotda.project-id=您的项目ID
iotda.access-key=您的AccessKey
iotda.secret-key=您的SecretKey
iotda.device-id=69568516c00ccb6d4b302187_esp32-001
```

---

## ❓ 常见问题

### Q1: IoTDA显示推送失败？
- 检查公网地址是否可访问
- 检查防火墙是否放行端口
- 查看后端控制台日志

### Q2: 数据没有保存到数据库？
- 检查数据库连接配置
- 查看后端控制台是否有错误日志
- 确认 `t_env_data` 表已创建

### Q3: 如何查看推送的原始数据？
后端会在控制台打印收到的数据：
```
====== 收到IoTDA数据推送 ======
{...完整JSON...}
====== 数据已保存到数据库 ======
```

---

## 📝 完成清单

- [ ] 后端服务已启动（端口8080）
- [ ] 已获取公网访问地址
- [ ] 已在IoTDA创建数据转发规则
- [ ] 规则已启用
- [ ] ESP32设备在线并上报数据
- [ ] 数据成功写入数据库
- [ ] 前端能显示实时数据
