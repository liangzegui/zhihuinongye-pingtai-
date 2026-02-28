# 华为云IoTDA配置指南

## 📋 配置步骤总览

根据ESP32技术规格文档，设备已具备以下信息：
- **设备ID**: `69568516c00ccb6d4b302187_esp32-001`
- **设备密钥**: `Lzg551162`
- **MQTT服务器**: `0c303a8ecf.st1.iotda-device.cn-south-1.myhuaweicloud.com:8883`
- **区域**: 华南-广州 (cn-south-1)

---

## 1️⃣ 验证设备注册状态

### 1.1 登录华为云IoTDA控制台
```
https://console.huaweicloud.com/iotdm/?region=cn-south-1
```

### 1.2 检查设备列表
- 左侧菜单: **设备** → **所有设备**
- 搜索设备ID: `69568516c00ccb6d4b302187_esp32-001`
- 确认设备状态: **在线/离线**

---

## 2️⃣ 配置产品模型

### 2.1 查看产品信息
- 左侧菜单: **产品**
- 找到设备所属产品(如: `ESP32_Agri_Monitor`)

### 2.2 配置服务能力
进入产品详情 → **模型定义** → **自定义模型**

#### 服务1: sensor_data (传感器数据上报)
| 属性名 | 数据类型 | 取值范围 | 单位 | 说明 |
|--------|---------|---------|------|------|
| temp | int | -40~80 | ℃ | 温度 |
| humi | int | 0~100 | % | 湿度 |
| soil | int | 0~4095 | ADC | 土壤湿度ADC值 |
| lightLux | int | 0~65535 | Lux | 光照强度 |
| eco2 | int | 400~60000 | ppm | CO2浓度 |
| tvoc | int | 0~60000 | ppb | TVOC浓度 |

#### 服务2: device_control (设备控制状态)
| 属性名 | 数据类型 | 取值范围 | 说明 |
|--------|---------|---------|------|
| pump | boolean | true/false | 水泵状态 |
| fan | boolean | true/false | 风扇状态 |
| light | boolean | true/false | 照明状态 |
| manual | boolean | true/false | 手动模式 |

#### 服务3: thresholds (阈值配置)
| 属性名 | 数据类型 | 取值范围 | 单位 | 说明 |
|--------|---------|---------|------|------|
| fanTempThreshold | int | 20~50 | ℃ | 风扇启动温度 |
| fanCO2Threshold | int | 400~5000 | ppm | 风扇启动CO2浓度 |
| pumpDroughtThreshold | int | 0~4095 | ADC | 水泵启动干旱阈值 |
| lightLuxThreshold | int | 50~5000 | Lux | 照明启动光照阈值 |

---

## 3️⃣ 配置数据转发规则

### 3.1 创建转发规则
左侧菜单: **规则** → **数据转发**

#### 基本信息
- **规则名称**: `forward_to_functiongraph`
- **描述**: 将ESP32上报数据转发到FunctionGraph
- **数据源**: 设备消息
- **触发条件**: 设备属性上报

#### 规则条件
```sql
SELECT 
  notify_data.header.device_id,
  notify_data.body.services
FROM 
  /huawei/v1/devices/+/data/json
WHERE 
  notify_data.header.device_id = '69568516c00ccb6d4b302187_esp32-001'
```

#### 转发目标
- **目标类型**: FunctionGraph
- **地域**: 华南-广州
- **函数URN**: 选择 `receive_iotda_data` 函数
- **调用方式**: 同步调用
- **请求体模板**:
```json
{
  "device_id": "${notify_data.header.device_id}",
  "notify_data": ${notify_data}
}
```

### 3.2 启用规则
- 保存后点击 **启用** 按钮
- 状态显示为 **运行中**

---

## 4️⃣ 配置设备侧鉴权信息

### 4.1 ESP32连接参数
根据技术文档,设备已配置:

```cpp
// MQTT连接参数
const char* mqttServer = "0c303a8ecf.st1.iotda-device.cn-south-1.myhuaweicloud.com";
const int mqttPort = 8883;  // TLS加密端口
const char* deviceId = "69568516c00ccb6d4b302187_esp32-001";
const char* deviceSecret = "Lzg551162";

// MQTT Topic
// 属性上报: $oc/devices/{device_id}/sys/properties/report
// 命令接收: $oc/devices/{device_id}/sys/commands/#
```

### 4.2 HMAC-SHA256签名算法
```cpp
// 用户名格式
username = device_id

// 密码格式 (HMAC-SHA256)
timestamp = 当前时间戳(毫秒)
password = hmac_sha256(deviceSecret, timestamp)
```

---

## 5️⃣ 测试数据上报

### 5.1 发送测试数据
设备上报JSON格式:
```json
{
  "services": [
    {
      "service_id": "sensor_data",
      "properties": {
        "temp": 25,
        "humi": 60,
        "soil": 3200,
        "lightLux": 800,
        "eco2": 600,
        "tvoc": 120
      }
    },
    {
      "service_id": "device_control",
      "properties": {
        "pump": false,
        "fan": true,
        "light": true,
        "manual": false
      }
    },
    {
      "service_id": "thresholds",
      "properties": {
        "fanTempThreshold": 30,
        "fanCO2Threshold": 1000,
        "pumpDroughtThreshold": 3200,
        "lightLuxThreshold": 800
      }
    }
  ]
}
```

### 5.2 验证数据转发
1. 进入 **规则引擎** → 查看规则运行日志
2. 进入 **FunctionGraph控制台** → 查看 `receive_iotda_data` 函数日志
3. 登录RDS数据库,查询 `t_env_data` 表:
```sql
SELECT * FROM t_env_data ORDER BY collect_time DESC LIMIT 10;
```

---

## 6️⃣ 测试设备控制

### 6.1 API Gateway调用控制接口
```bash
curl -X POST https://your-apigw-domain.apigw.cn-south-1.huaweicloud.com/api/device/control \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "pump": true,
    "manual": true
  }'
```

### 6.2 验证IoTDA命令下发
1. IoTDA控制台 → **设备详情** → **属性查询**
2. 查看 `pump` 属性是否变为 `true`
3. 查看设备端OLED显示是否更新

### 6.3 查看命令历史
IoTDA控制台 → **设备详情** → **命令下发记录**
- 检查命令状态: 成功/失败/超时
- 查看响应内容

---

## 7️⃣ 常见问题排查

### 7.1 设备连接失败
- **检查项**:
  - 设备ID和密钥是否正确
  - MQTT服务器地址和端口是否正确
  - 网络防火墙是否允许8883端口
  - TLS证书是否有效

- **日志查看**:
  - IoTDA控制台 → **监控运维** → **设备日志**
  - ESP32串口输出

### 7.2 数据未转发到FunctionGraph
- **检查项**:
  - 转发规则是否已启用
  - SQL条件是否匹配设备ID
  - FunctionGraph函数是否正常运行
  - 函数是否配置正确的环境变量

- **调试方法**:
  - 查看规则引擎执行日志
  - 手动触发FunctionGraph函数测试
  - 检查函数代码中的数据库连接

### 7.3 控制命令无响应
- **检查项**:
  - API Gateway是否正确绑定control_device函数
  - IoTDA北向API调用参数是否正确
  - 设备是否在线
  - 设备是否订阅命令Topic

- **验证步骤**:
  1. 在IoTDA控制台手动下发命令
  2. 查看设备端是否收到MQTT消息
  3. 检查FunctionGraph函数日志

---

## 8️⃣ 监控与告警

### 8.1 配置设备监控
IoTDA控制台 → **监控运维** → **设备监控**
- 设备在线率
- 消息上报成功率
- 命令下发成功率

### 8.2 配置FunctionGraph监控
FunctionGraph控制台 → **监控** → **函数监控**
- 调用次数
- 错误次数
- 平均响应时间

### 8.3 数据库监控
RDS控制台 → **监控** → **性能监控**
- CPU使用率
- 内存使用率
- 连接数
- 慢查询日志

---

## 9️⃣ 安全建议

### 9.1 设备侧安全
- ✅ 使用TLS/SSL加密通信(端口8883)
- ✅ 定期更换设备密钥
- ✅ 使用HMAC-SHA256签名认证
- ❌ 避免在代码中硬编码密钥

### 9.2 云侧安全
- ✅ API Gateway启用HTTPS
- ✅ 配置JWT令牌认证
- ✅ 设置IP白名单(如有固定出口IP)
- ✅ 开启访问日志审计

### 9.3 数据库安全
- ✅ RDS仅允许FunctionGraph访问(VPC内网)
- ✅ 使用强密码策略
- ✅ 定期备份数据
- ✅ 开启SQL审计

---

## 🔟 性能优化建议

### 10.1 数据上报频率
- **建议**: 30秒-1分钟上报一次
- **原因**: 减少流量费用,降低数据库压力
- **实现**: ESP32代码中设置`delay(30000)`

### 10.2 数据库连接池
在FunctionGraph函数中使用连接池:
```python
import pymysql.cursors
from dbutils.pooled_db import PooledDB

pool = PooledDB(
    creator=pymysql,
    maxconnections=5,
    host=os.environ['DB_HOST'],
    user=os.environ['DB_USER'],
    password=os.environ['DB_PASSWORD'],
    database=os.environ['DB_NAME'],
    charset='utf8mb4'
)
```

### 10.3 缓存策略
- 使用Redis缓存最新数据(可选)
- 减少数据库查询压力
- 提升实时数据接口响应速度

---

## 📞 技术支持

### 华为云服务支持
- IoTDA文档: https://support.huaweicloud.com/iotda/
- FunctionGraph文档: https://support.huaweicloud.com/functiongraph/
- 工单系统: https://console.huaweicloud.com/ticket/

### ESP32开发资源
- ESP-IDF文档: https://docs.espressif.com/
- Arduino ESP32: https://github.com/espressif/arduino-esp32

---

**配置完成后,系统架构图:**

```
┌─────────────────────────────────────────────────────────────────┐
│                    智慧农业监控系统架构                           │
└─────────────────────────────────────────────────────────────────┘

 ┌──────────────┐
 │   ESP32设备   │ (DHT11/SGP30/土壤传感器/水泵/风扇/灯)
 │ Device ID:    │
 │ esp32-001     │
 └──────┬───────┘
        │ MQTT (TLS 8883)
        │ 30秒/次上报
        ↓
 ┌──────────────────────────────────────┐
 │   华为云IoTDA物联网平台                │
 │   - 设备管理                          │
 │   - MQTT Broker                      │
 │   - 数据转发规则                      │
 │   - 命令下发                          │
 └──────┬───────────────────────┬───────┘
        │ HTTP推送               │ 北向API调用
        │ (转发规则)             │ (属性设置)
        ↓                       ↑
 ┌──────────────┐         ┌──────────────┐
 │ FunctionGraph │         │ FunctionGraph │
 │ receive_data  │         │ control_device│
 │ (数据接收)     │         │ (设备控制)     │
 └──────┬───────┘         └───────▲──────┘
        │                         │
        │ 写入数据                 │ HTTP请求
        ↓                         │
 ┌──────────────┐         ┌──────┴──────┐
 │  RDS MySQL    │         │ API Gateway  │
 │  t_env_data   │         │ (HTTPS)      │
 │  t_device_*   │         └──────▲──────┘
 └───────────────┘                │
                                  │ HTTPS
                          ┌───────┴──────┐
                          │ Vue 3前端     │
                          │ (Element Plus)│
                          └──────────────┘
```

---

**✅ 配置检查清单:**

- [ ] IoTDA设备已注册且在线
- [ ] 产品模型已定义(sensor_data/device_control/thresholds)
- [ ] 数据转发规则已创建并启用
- [ ] FunctionGraph函数已部署(receive_iotda_data + control_device)
- [ ] 函数环境变量已配置(DB_* + IOTDA_*)
- [ ] RDS数据库已创建且schema已导入
- [ ] API Gateway已创建且路由已绑定
- [ ] 前端.env.production已配置APIG域名
- [ ] 测试数据上报成功
- [ ] 测试设备控制成功

配置完成后,即可实现ESP32设备与云端的双向通信! 🎉
