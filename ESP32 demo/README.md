# ESP32智能农业环境监控系统 - 技术规格文档

基于 ESP32 的智能农业环境监控系统，通过华为云IoTDA平台进行数据上报和远程控制。系统包含多种传感器用于环境监测，以及水泵、风扇、照明灯等执行器用于自动/手动控制。

---

## 📋 目录

- [项目概述](#项目概述)
- [硬件配置](#硬件配置)
- [华为云IoTDA连接信息](#华为云iotda连接信息)
- [数据格式定义](#数据格式定义)
- [自动控制逻辑](#自动控制逻辑)
- [本地Web API](#本地web-api)
- [华为云北向API](#华为云北向api)
- [数据转发配置](#数据转发配置)
- [软件依赖](#软件依赖)
- [安装部署](#安装部署)

---

## 🎯 项目概述

### 主控制器
| 项目属性 | 值 |
|---------|---|
| **平台** | Espressif32 (ESP32) |
| **开发板** | ESP32 Dev Module |
| **框架** | Arduino |
| **开发环境** | PlatformIO |
| **分区方案** | huge_app.csv |
| **串口波特率** | 9600 |

### 功能特性
- **传感器监测**：温湿度、光照、土壤湿度、CO2、TVOC
- **执行器控制**：水泵、风扇、照明灯
- **双重控制**：自动模式 + 手动模式
- **云端连接**：华为云IoTDA平台，MQTT over TLS
- **本地控制**：内置Web服务器，支持局域网访问
- **数据持久化**：阈值设置保存到Flash

---

## 🔧 硬件配置

### 传感器列表

| 传感器 | 引脚 | 类型 | 测量范围 | 说明 |
|-------|-----|------|---------|------|
| DHT11温湿度 | GPIO14 | 数字 | 温度10-40℃，湿度20-80%RH | 环境温湿度监测 |
| 光照传感器AO | GPIO34 | 模拟 | ADC 0-4095 | 光照强度检测 |
| 光照传感器DO | GPIO15 | 数字 | 0/1 | 光照阈值触发 |
| 土壤湿度AO | GPIO35 | 模拟 | ADC 0-4095 | 土壤含水量检测 |
| 土壤湿度DO | GPIO16 | 数字 | 0/1 | 干旱阈值触发 |
| SGP30空气质量 | I2C (0x58) | I2C | eCO2 400-6000ppm, TVOC 0-6000ppb | 空气质量监测 |
| SSD1315 OLED | I2C | I2C | 128x64像素 | 实时数据显示 |

### 执行器列表

| 设备 | 引脚+ | 引脚- | 控制方式 | 说明 |
|-----|-------|-------|---------|------|
| 水泵 | GPIO32 | GPIO33 | 高电平启动 | 自动浇水 |
| 风扇 | GPIO25 | GPIO26 | 高电平启动 | 通风降温 |
| 照明灯 | GPIO2 | - | 高电平点亮 | 补光照明 |

### 硬件需求清单

| 组件 | 型号 | 数量 |
|------|------|------|
| 主控板 | ESP32 DevKit | 1 |
| 温湿度传感器 | DHT11 | 1 |
| 光敏传感器 | 光敏电阻模块 (AO+DO) | 1 |
| 土壤湿度传感器 | 电容式/电阻式 (AO+DO) | 1 |
| 空气质量传感器 | SGP30 (可选) | 1 |
| OLED显示屏 | SSD1315 128x64 I2C | 1 |
| 继电器模块 | 2路/4路 5V继电器 | 1 |
| 水泵 | 5V/12V 微型水泵 | 1 |
| 风扇 | 5V/12V 散热风扇 | 1 |
| LED灯 | LED模块或灯带 | 1 |
| 电源 | 5V/3A 电源适配器 | 1 |

---

## 🔌 引脚接线

### 传感器接线

| 传感器 | 传感器引脚 | ESP32 引脚 |
|--------|-----------|-----------|
| DHT11 | DATA | GPIO 14 |
| DHT11 | VCC | 3.3V |
| DHT11 | GND | GND |
| 光敏传感器 | AO | GPIO 34 |
| 光敏传感器 | DO | GPIO 15 |
| 土壤湿度 | AO | GPIO 35 |
| 土壤湿度 | DO | GPIO 16 |
| SGP30 | SDA | GPIO 21 (默认) |
| SGP30 | SCL | GPIO 22 (默认) |
| OLED | SDA | GPIO 21 (默认) |
| OLED | SCL | GPIO 22 (默认) |

### 执行器接线

| 执行器 | 继电器通道 | ESP32 引脚 | 控制逻辑 |
|--------|-----------|-----------|---------|
| 水泵 | IN1 | GPIO 32 | HIGH=开启 |
| 水泵 | IN2 | GPIO 33 | LOW |
| 风扇 | IN3 | GPIO 25 | HIGH=开启 |
| 风扇 | IN4 | GPIO 26 | LOW |
| 照明灯 | - | GPIO 2 | HIGH=开启 |

### 接线示意图

```
                    ┌─────────────────┐
                    │     ESP32       │
                    │                 │
    DHT11 ─────────►│ GPIO14          │
    光敏AO ────────►│ GPIO34          │
    光敏DO ────────►│ GPIO15          │
    土壤AO ────────►│ GPIO35          │
    土壤DO ────────►│ GPIO16          │
                    │                 │
    继电器IN1◄─────│ GPIO32 (水泵)    │
    继电器IN2◄─────│ GPIO33          │
    继电器IN3◄─────│ GPIO25 (风扇)    │
    继电器IN4◄─────│ GPIO26          │
    LED◄───────────│ GPIO2  (灯)     │
                    │                 │
    SGP30/OLED◄───►│ GPIO21 (SDA)    │
    SGP30/OLED◄───►│ GPIO22 (SCL)    │
                    └─────────────────┘
```

---

## 📦 软件依赖

### PlatformIO 库

```ini
lib_deps = 
  adafruit/Adafruit SSD1306@^2.5.7
  adafruit/Adafruit GFX Library@^1.11.9
  olikraus/U8g2@^2.35.18
  tzapu/WiFiManager@^2.0.17
  bblanchon/ArduinoJson@^6.21.3
  me-no-dev/ESPAsyncWebServer@^1.2.3
  me-no-dev/AsyncTCP@^1.1.1
  knolleary/PubSubClient@^2.8
```

### 库功能说明

| 库名 | 版本 | 用途 |
|-----|-----|------|
| Adafruit SSD1306 | ^2.5.7 | OLED显示驱动 |
| Adafruit GFX Library | ^1.11.9 | 图形库 |
| U8g2 | ^2.35.18 | OLED中文显示 |
| WiFiManager | ^2.0.17 | WiFi配网管理 |
| ArduinoJson | ^6.21.3 | JSON解析 |
| ESPAsyncWebServer | ^1.2.3 | 异步Web服务器 |
| AsyncTCP | ^1.1.1 | 异步TCP |
| PubSubClient | ^2.8 | MQTT客户端 |

---

## ☁️ 华为云IoTDA连接信息

### MQTT配置

| 配置项 | 值 |
|-------|---|
| **服务器地址** | `0c303a8ecf.st1.iotda-device.cn-south-1.myhuaweicloud.com` |
| **端口** | `8883` (TLS加密) |
| **设备ID** | `69568516c00ccb6d4b302187_esp32-001` |
| **设备密钥** | `Lzg551162` |
| **用户名** | `69568516c00ccb6d4b302187_esp32-001` (与设备ID相同) |
| **上报间隔** | 10秒 |
| **重连间隔** | 5秒 |

### MQTT认证方式

采用HMAC-SHA256动态签名认证：

| 参数 | 格式/计算方式 |
|-----|--------------|
| **ClientId** | `{deviceId}_0_0_{timestamp}` |
| **timestamp格式** | `yyyyMMddHH` (年月日小时) |
| **Password计算** | `hex(HMAC-SHA256(key=timestamp, message=secret))` |

> ⚠️ **重要**：timestamp作为HMAC的**密钥key**，secret作为**消息message**

### MQTT主题

| 类型 | 主题 |
|-----|------|
| **数据上报** | `$oc/devices/69568516c00ccb6d4b302187_esp32-001/sys/properties/report` |
| **命令下发** | `$oc/devices/69568516c00ccb6d4b302187_esp32-001/sys/commands/#` |
| **属性设置** | `$oc/devices/69568516c00ccb6d4b302187_esp32-001/sys/properties/set/#` |

---

## 📊 数据格式定义

### ESP32上报数据格式 (每10秒上报一次)

```json
{
  "services": [{
    "service_id": "default",
    "properties": {
      "temp": 25,                    // 整数，温度，单位℃
      "humi": 60,                    // 整数，湿度，单位%RH
      "soil": 2800,                  // 整数，土壤湿度ADC值，0-4095，越大越干
      "lightLux": 1200,              // 整数，光照强度，单位lux
      "eco2": 450,                   // 整数，CO2浓度，单位ppm
      "tvoc": 20,                    // 整数，TVOC，单位ppb
      "pump": false,                 // 布尔，水泵状态
      "fan": false,                  // 布尔，风扇状态
      "light": false,                // 布尔，照明灯状态
      "manual": false,               // 布尔，true=手动模式，false=自动模式
      "lightLuxThreshold": 800,      // 整数，光照阈值，低于此值开灯
      "pumpDroughtThreshold": 3200,  // 整数，土壤干旱阈值，高于此值开泵
      "fanTempThreshold": 30,        // 整数，温度阈值，高于此值开风扇
      "fanCO2Threshold": 1000        // 整数，CO2阈值，高于此值开风扇
    }
  }]
}
```

### 上报属性说明

| 属性名 | 类型 | 单位 | 范围 | 说明 |
|-------|-----|------|------|------|
| `temp` | int | ℃ | 10-40 | 环境温度 |
| `humi` | int | %RH | 20-80 | 空气湿度 |
| `soil` | int | ADC | 0-4095 | 土壤湿度(越大越干) |
| `lightLux` | int | lux | 50-8800 | 光照强度 |
| `eco2` | int | ppm | 400-6000 | 二氧化碳浓度 |
| `tvoc` | int | ppb | 0-6000 | 挥发性有机物 |
| `pump` | bool | - | true/false | 水泵状态 |
| `fan` | bool | - | true/false | 风扇状态 |
| `light` | bool | - | true/false | 照明灯状态 |
| `manual` | bool | - | true/false | 手动模式 |

### 阈值属性说明

| 属性名 | 类型 | 默认值 | 范围 | 说明 |
|-------|-----|-------|------|------|
| `fanTempThreshold` | int | 30 | 20-50 | 风扇温度阈值(℃) |
| `fanCO2Threshold` | int | 1000 | 400-5000 | 风扇CO2阈值(ppm) |
| `pumpDroughtThreshold` | int | 3200 | 0-5000 | 水泵干旱阈值(ADC) |
| `lightLuxThreshold` | int | 800 | 50-5000 | 照明光照阈值(lux) |

### 控制命令下发格式 (从云端发送到ESP32)

```json
{
  "services": [{
    "service_id": "default",
    "properties": {
      "pump": true,                  // 可选，控制水泵
      "fan": false,                  // 可选，控制风扇
      "light": true,                 // 可选，控制照明灯
      "manual": true,                // 可选，切换手动/自动模式
      "fanTempThreshold": 28,        // 可选，设置温度阈值(20-50)
      "fanCO2Threshold": 800,        // 可选，设置CO2阈值(400-5000)
      "pumpDroughtThreshold": 3000,  // 可选，设置土壤干旱阈值(0-5000)
      "lightLuxThreshold": 1000      // 可选，设置光照阈值(50-5000)
    }
  }]
}
```

---

## 📈 传感器数据对照表

### 土壤湿度ADC值对照

| ADC范围 | 状态 | 说明 | 水泵动作 |
|--------|-----|------|---------|
| 0-2200 | 过湿 | 土壤水分过多 | 关闭 |
| 2200-2800 | 正常 | 适宜状态 | 关闭 |
| 2800-3200 | 轻旱 | 需要关注 | 关闭 |
| 3200-3500 | 中旱 | 建议浇水 | **开启** |
| 3500-3800 | 重旱 | 需要立即浇水 | **开启** |
| 3800-4095 | 特旱 | 严重缺水 | **开启** |

**土壤湿度百分比转换公式**：
```
percentage = 100 - ((soil - 2200) / (4000 - 2200)) * 100
```

### 光照强度等级对照

| ADC范围 | Lux范围 | 状态 | 照明灯动作 |
|--------|---------|-----|----------|
| 0-100 | 6000-8800 | 极亮 | 关闭 |
| 100-500 | 3000-6000 | 明亮 | 关闭 |
| 500-1500 | 1000-3000 | 正常 | 关闭 |
| 1500-3500 | 800-1000 | 偏暗 | **开启** |
| 3500-4095 | 50-800 | 暗 | **开启** |

---

## 🌐 华为云北向API (Web应用调用)

### API接入信息

| 配置项 | 值 |
|-------|---|
| **API端点** | `https://iotda.cn-south-1.myhuaweicloud.com` |
| **认证方式** | IAM Token 或 AK/SK签名 |
| **区域** | 华南-广州 (cn-south-1) |

### 发送属性设置到设备

```
PUT /v5/iot/{project_id}/devices/{device_id}/properties
Content-Type: application/json

请求体:
{
  "services": [{
    "service_id": "default",
    "properties": {
      "pump": true
    }
  }]
}
```

### 查询设备影子

```
GET /v5/iot/{project_id}/devices/{device_id}/shadow
```

---

## 🔄 数据转发配置

在华为云IoTDA控制台配置"数据转发规则"，可将设备上报数据通过HTTP POST转发到您的后端服务：

### 配置步骤

1. 登录华为云IoTDA控制台
2. 进入 **规则** → **数据转发**
3. 创建规则，触发事件选择 **设备属性上报**
4. 添加HTTP转发目标，URL填写后端Webhook地址
5. 测试转发是否正常

### 转发数据格式

```json
{
  "notify_data": {
    "body": {
      "services": [{
        "service_id": "default",
        "properties": {
          "temp": 25,
          "humi": 60,
          "soil": 2800,
          "lightLux": 1200,
          "eco2": 450,
          "tvoc": 20,
          "pump": false,
          "fan": false,
          "light": false,
          "manual": false
        }
      }]
    }
  }
}
```

---

## 🚀 安装部署

### 1. 安装开发环境

1. 安装 [VS Code](https://code.visualstudio.com/)
2. 安装 PlatformIO IDE 扩展

### 2. 克隆/下载项目

将项目文件夹放置到本地目录

### 3. 编译上传

1. 用 VS Code 打开项目文件夹
2. 连接 ESP32 开发板
3. 点击 PlatformIO 的 `Build` 按钮编译
4. 点击 `Upload` 按钮上传

### 4. 首次配网

1. ESP32 启动后会创建热点：`ESP32-Sensor-AP`
2. 用手机/电脑连接该热点
3. 浏览器访问 `192.168.4.1`
4. 选择您的 WiFi 网络并输入密码
5. 配网成功后 OLED 会显示分配的 IP 地址

---

## 📖 使用说明

### OLED 显示内容

```
光照: 2500lux (正常)
温湿度: 25℃ / 60%RH
土壤: 2800 (正常)
eCO₂:450ppm | TVOC:20ppb
自动:泵关|风关|灯关
```

### 控制模式

| 模式 | 说明 |
|------|------|
| 自动模式 | 系统根据传感器数据自动控制水泵、风扇、照明灯 |
| 手动模式 | 通过 Web 界面手动开关各设备 |

---

## 🌐 Web界面

访问 ESP32 的 IP 地址（如 `http://192.168.1.100`）打开控制台。

### 功能区域

1. **状态栏**：显示在线状态和控制模式
2. **数据卡片**：温湿度、光照、土壤湿度、空气质量
3. **详细数据**：传感器原始数据
4. **设备控制**：水泵、风扇、照明灯开关

### 界面特性

- 响应式设计，支持手机和电脑
- 每秒自动刷新数据
- 实时状态同步
- Toast 消息提示

---

## 📡 API接口

### 获取传感器数据

```
GET /data
```

返回 JSON：
```json
{
  "lightIntensity": 2500,
  "lightZone": "正常",
  "lightDO": 1,
  "dhtTemp": 25,
  "dhtHumi": 60,
  "dhtStatus": "正常",
  "soilAO": 2800,
  "soilStatus": "正常",
  "soilDO": 0,
  "eco2": 450,
  "tvoc": 20,
  "sgp30Status": "正常",
  "pumpState": false,
  "fanState": false,
  "lightState": false,
  "manualControl": false,
  "lastUpdate": "2025-12-25 10:30:00"
}
```

### 控制设备

```
GET /pump?state=1    # 开启水泵
GET /pump?state=0    # 关闭水泵
GET /fan?state=1     # 开启风扇
GET /fan?state=0     # 关闭风扇
GET /light?state=1   # 开启照明灯
GET /light?state=0   # 关闭照明灯
```

### 切换模式

```
GET /mode?state=1    # 切换到手动模式
GET /mode?state=0    # 切换到自动模式
```

---

## ⚙️ 自动控制逻辑

### 水泵控制

| 土壤状态 | ADC值范围 | 水泵动作 |
|---------|----------|---------|
| 过湿 | < 2200 | 关闭 |
| 正常 | 2200 - 2800 | 关闭 |
| 轻旱 | 2800 - 3200 | 关闭 |
| 中旱 | 3200 - 3500 | **开启** |
| 重旱 | 3500 - 3800 | **开启** |
| 特旱 | > 3800 | **开启** |

### 风扇控制

| 条件 | 风扇动作 |
|------|---------|
| 温度 > 30℃ | **开启** |
| eCO₂ > 1000 ppm | **开启** |
| 其他情况 | 关闭 |

### 照明灯控制

| 光照等级 | 光照强度 | 照明灯 |
|---------|---------|--------|
| 极亮 | > 6000 lux | 关闭 |
| 明亮 | 3000 - 6000 lux | 关闭 |
| 正常 | 1000 - 3000 lux | 关闭 |
| 偏暗 | 800 - 1000 lux | **开启** |
| 暗 | < 800 lux | **开启** |

---

## 🔧 故障排除

### 水泵启动时系统重启

**原因**：水泵启动瞬间电流冲击导致电压跌落

**解决方案**：
1. 使用 5V/3A 以上大功率电源
2. ESP32 和水泵使用独立电源，共地
3. 在 ESP32 VIN 和 GND 之间并联 470μF 电解电容
4. 在继电器线圈两端反向并联 1N4007 续流二极管

### WiFi 连接失败

**解决方案**：
1. 长按 RST 按钮重启设备
2. 重新连接 `ESP32-Sensor-AP` 热点配网
3. 确保路由器在 2.4GHz 频段

### 传感器数据异常

**DHT11 读数为 0**：
- 检查接线是否松动
- 确认 DATA 引脚连接到 GPIO 14

**土壤湿度一直显示过湿/特旱**：
- 调整传感器模块上的电位器
- 校准 ADC 阈值参数

**SGP30 无数据**：
- 检查 I2C 接线（SDA/SCL）
- 确认 I2C 地址为 0x58
- SGP30 需要预热约 10 秒

### OLED 不显示

- 检查 I2C 接线
- 确认 OLED 模块型号为 SSD1315/SSD1306
- 运行 I2C 扫描确认设备地址

---

## 📊 系统参数

### 采样间隔
- 传感器读取：1000ms
- 串口打印：1000ms
- Web 数据刷新：1000ms

### 保护机制
- 非阻塞延迟启动：500ms
- 水泵/风扇顺序启动，避免同时启动
- WiFi 配网超时：180 秒

### 串口配置
- 波特率：9600
- 用于调试输出

---

## 📄 许可证

MIT License

---

## 🔄 更新日志

### v2.0.0 (2026-01-10)
- 新增华为云IoTDA平台连接
- 新增MQTT over TLS安全通信
- 新增HMAC-SHA256动态签名认证
- 新增远程属性设置功能
- 新增阈值持久化存储到Flash
- 完善技术文档

### v1.0.0 (2025-12-25)
- 初始版本发布
- 支持 DHT11、光敏、土壤湿度、SGP30 传感器
- 支持水泵、风扇、照明灯控制
- 支持自动/手动控制模式
- 支持 Web 远程监控
- 添加非阻塞延迟启动保护机制

---

## 🤖 AI集成提示词

如需让AI帮助开发配套的Web应用，可使用以下提示词：

```
我需要创建一个Web应用来连接华为云IoTDA平台，实现对ESP32智能农业设备的远程监控和控制。

## 华为云IoTDA配置信息
- 区域：华南-广州 (cn-south-1)
- API端点：https://iotda.cn-south-1.myhuaweicloud.com
- 设备ID：69568516c00ccb6d4b302187_esp32-001
- 认证方式：IAM Token 或 AK/SK签名

## ESP32上报的数据
设备每10秒上报: temp(温度℃), humi(湿度%RH), soil(土壤ADC), lightLux(光照lux), 
eco2(CO2 ppm), tvoc(ppb), pump/fan/light(设备状态), manual(模式)

## 需要实现的功能
1. 实时数据仪表盘 - 显示所有传感器数据，自动刷新
2. 设备控制面板 - 水泵/风扇/照明灯开关，手动/自动模式切换
3. 阈值设置 - 设置自动控制的触发阈值
4. 历史数据图表 - 使用ECharts展示趋势

## 技术栈建议
前端: Vue 3 + Element Plus 或 React + Ant Design
后端: Node.js/Java/Python
数据库: MySQL 或 MongoDB
```

---

## 👨‍💻 作者

ESP32智能监控系统 © 2026
