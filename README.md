# 🌾 智慧农业环境监控平台

基于 **Spring Boot + Vue 3 + ESP32** 的全栈 IoT 智慧农业系统，支持实时环境数据采集、可视化分析、智能预警和远程设备控制。

## 系统架构

```
┌──────────────┐   MQTT/8883    ┌──────────────┐
│    ESP32     │ ◄────────────► │  华为云 IoTDA │
│   传感器节点  │                │   物联网平台   │
└──────┬───────┘                └───────┬──────┘
       │ HTTP (直连)             HTTP Webhook │
       ▼                                     ▼
┌─────────────────────────────────────────────────┐
│           Spring Boot 后端 (:8080)               │
│  REST API + WebSocket STOMP + IoTDA 北向API       │
└────────┬──────────────────────────┬──────────────┘
         │ MySQL                    │ WebSocket/REST
         ▼                         ▼
┌──────────────┐          ┌──────────────────┐
│   MySQL DB   │          │  Vue 3 前端(:8081)│
│   agri_db    │          │  Element Plus     │
└──────────────┘          │  ECharts 可视化   │
                          └──────────────────┘
```

**数据流向：**
1. ESP32 采集传感器数据 → 通过 HTTP 直连后端 或 华为云 IoTDA Webhook 转发
2. 后端处理存库 → 通过 WebSocket STOMP 推送给前端实时展示
3. 前端发起设备控制 → 后端通过 HTTP 转发至 ESP32 执行

## 技术栈

| 组件 | 技术 |
|------|------|
| **后端** | Java 21 · Spring Boot 3.5 · MyBatis-Plus · MySQL 8.0 · WebSocket STOMP |
| **前端** | Vue 3 · Element Plus · ECharts · SockJS + STOMP · Axios |
| **固件** | ESP32 · PlatformIO · DHT22 · BH1750 · MQ-135 · AsyncWebServer |
| **云平台** | 华为云 IoTDA（可选，支持纯直连模式） |

## 快速启动

### 环境要求

- Java 21+、Maven 3.8+
- Node.js 16+、npm 8+
- MySQL 8.0+（数据库名：`agri_db`）

### 方式一：一键启动（推荐）

双击根目录的 **`启动项目.bat`** 即可自动完成：
> 启动 MySQL → 启动后端 → 启动前端 → 自动打开浏览器

PowerShell 用户也可执行：
```powershell
.\启动项目.ps1        # 启动
.\启动项目.ps1 -Stop  # 停止全部服务
```

### 方式二：手动启动

```bash
# 1. 初始化数据库
mysql -u root -p < huawei-cloud/database/schema.sql

# 2. 启动后端
cd idea/agri-backend
./mvnw spring-boot:run        # Linux/Mac
.\mvnw.cmd spring-boot:run    # Windows

# 3. 启动前端
cd vue/agri-frontend
npm install   # 仅首次
npm run serve
```

### 方式三：Docker 部署

```bash
docker-compose up -d
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:8081 |
| 后端 API | http://localhost:8080 |

默认账号：`admin` / `admin123`

> 端口可在 `project.config.json` 中修改，前后端均会自动读取。

---

## 项目结构

```
├── project.config.json          # 共享端口配置（前后端自动读取）
├── .env.example                 # 环境变量模板
├── docker-compose.yml           # Docker 一键部署
├── 启动项目.bat / .ps1          # 一键启动脚本
│
├── idea/agri-backend/           # ========== 后端 ==========
│   ├── src/main/java/.../
│   │   ├── controller/          # 接口层
│   │   │   ├── AuthController          # 登录/注册/验证码
│   │   │   ├── RealTimeController      # 实时数据
│   │   │   ├── DataController          # 历史数据查询
│   │   │   ├── DataAnalysisController  # 趋势分析
│   │   │   ├── DeviceControlController # 设备控制
│   │   │   ├── WarningController       # 预警日志
│   │   │   ├── WarningRuleController   # 预警规则配置
│   │   │   ├── AdminController         # 管理员管理
│   │   │   ├── ControlHistoryController# 控制历史记录
│   │   │   ├── IoTDAWebhookController  # 华为云数据接收
│   │   │   └── SystemConfigController  # 系统配置
│   │   ├── service/             # 业务层
│   │   ├── mapper/              # 数据访问层（MyBatis-Plus）
│   │   ├── entity/              # 数据库实体
│   │   ├── config/              # 配置类（CORS/WebSocket/JWT）
│   │   └── utils/               # 工具类（JWT/验证码/密码校验）
│   └── src/main/resources/
│       ├── application.properties  # 主配置文件
│       └── mapper/*.xml            # MyBatis XML 映射
│
├── vue/agri-frontend/           # ========== 前端 ==========
│   ├── src/
│   │   ├── views/               # 页面组件
│   │   │   ├── Login.vue / Register.vue   # 登录/注册
│   │   │   ├── Home.vue                   # 农场监控仪表盘
│   │   │   ├── RealTime.vue               # 实时环境数据
│   │   │   ├── HistoricalData.vue         # 历史数据查询
│   │   │   ├── DataAnalysis.vue           # 数据趋势分析
│   │   │   ├── WarningLogs.vue            # 预警日志
│   │   │   ├── Settings.vue               # 系统设置
│   │   │   ├── PersonalInfo.vue           # 个人信息
│   │   │   ├── AdminManage.vue            # 管理员中心
│   │   │   └── ControlHistory.vue         # 设备控制记录
│   │   ├── components/          # 可复用组件
│   │   │   ├── DeviceControl.vue          # 设备控制面板
│   │   │   ├── DeviceStatus.vue           # 设备状态监控
│   │   │   ├── WarningRuleConfig.vue      # 预警规则配置
│   │   │   └── Esp32Config.vue            # ESP32连接配置
│   │   ├── api/                 # API 请求封装
│   │   ├── utils/               # 工具（axios/token/websocket）
│   │   └── router/              # 路由配置 + 权限守卫
│   └── vue.config.js            # 代理配置（自动读取 project.config.json）
│
├── ESP32 demo/                  # ========== 固件 ==========
│   ├── src/main.cpp             # 主程序入口
│   └── platformio.ini           # PlatformIO 构建配置
│
├── huawei-cloud/                # ========== 华为云配置 ==========
│   ├── database/schema.sql      # 完整建表语句
│   ├── IoTDA配置指南.md          # FunctionGraph 方案配置
│   └── 部署指南.md               # 华为云无服务器部署
│
└── deploy/                      # ========== 部署 ==========
    ├── 学生云服务器部署指南.md     # 传统服务器部署方案
    ├── deploy.sh                # 一键部署脚本
    └── nginx.conf               # Nginx 配置
```

---

## 数据库设计

数据库名：`agri_db`，建表语句：[huawei-cloud/database/schema.sql](huawei-cloud/database/schema.sql)

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `t_user` | 用户账号 | username, password(BCrypt) |
| `t_env_data` | 环境采集数据 | temperature, humidity, soil_moisture, light_intensity, co2 |
| `t_device_status` | 设备状态与阈值 | pump/fan/light_state, manual_mode, 各阈值 |
| `t_warning_log` | 预警日志 | warning_type, warning_level, is_handled |
| `t_warning_rule` | 预警阈值规则 | sensor_type, min_value, max_value |
| `t_control_history` | 设备控制记录 | control_type, control_value, operator |
| `t_exception_config` | 异常检测配置 | config_key, config_value, config_group |

---

## API 接口概览

### 认证（无需 Token）
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录（返回 JWT Token） |
| POST | `/auth/register` | 注册 |
| GET  | `/auth/captcha` | 获取图形验证码 |

### 环境数据
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/realtime` | 获取实时环境数据 |
| GET | `/api/data/history` | 分页查询历史数据 |
| GET | `/api/analysis/trend` | 趋势分析数据 |
| GET | `/api/analysis/statistics` | 统计数据 |

### 设备控制
| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/api/device/status` | 获取设备状态 |
| POST | `/api/device/control` | 发送控制指令 |
| POST | `/api/device/threshold` | 更新阈值设置 |
| GET  | `/api/control-history` | 设备控制历史 |

### 预警系统
| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/api/warning/logs` | 预警日志列表 |
| PUT  | `/api/warning/handle/{id}` | 处理预警 |
| GET  | `/api/warning-rules` | 预警规则列表 |
| POST | `/api/warning-rules` | 新增预警规则 |

### WebSocket 订阅
| 端点 | 订阅地址 | 说明 |
|------|----------|------|
| `/ws` | `/topic/sensor-data` | 实时传感器数据推送 |
| `/ws` | `/topic/device-status` | 设备状态变更推送 |

> 所有 `/api/*` 接口需要在 Header 中携带 `Authorization: Bearer <token>`

---

## 开发约定

### 后端
- **统一响应格式**：所有接口返回 `Result<T>`，使用 `Result.success(data)` / `Result.error(msg)`
- **分层结构**：Controller → Service 接口 → impl/ 实现类 → Mapper
- **JWT 认证**：`JwtInterceptor` 拦截，白名单 `/auth/**`、`/iotda/**`、`/ws/**`
- **实体映射**：表名 `t_env_data` → 实体 `EnvDataEntity`，下划线自动转驼峰
- **密码加密**：BCrypt，登录限流 5 次/10 分钟

### 前端
- **API 封装**：`src/api/` 按模块拆分，统一使用 `src/utils/request.js` 的 axios 实例
- **代理规则**：`/api/auth/*` → 后端 `/auth/*`（重写路径），其他 `/api/*` 直接转发
- **Token 管理**：`localStorage` key 为 `agri_platform_token`
- **路由守卫**：未设 `meta.noAuth` 的路由均需 Token
- **WebSocket**：SockJS + STOMP 封装，自动重连 5 次

### 通用
- 代码注释和 UI 文本均为**中文**
- 敏感配置通过 `${ENV_VAR:default}` 环境变量覆盖（参考 `.env.example`）

---

## 环境变量

复制 `.env.example` 为 `.env` 填入实际值。关键变量：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DATABASE_URL` | MySQL 连接串 | `jdbc:mysql://localhost:3306/agri_db` |
| `DATABASE_USERNAME` | 数据库用户 | `root` |
| `DATABASE_PASSWORD` | 数据库密码 | — |
| `IOTDA_ENDPOINT` | 华为云 IoTDA 地址 | — |
| `IOTDA_PROJECT_ID` | 华为云项目 ID | — |
| `IOTDA_AK` / `IOTDA_SK` | 华为云访问密钥 | — |
| `IOTDA_DEVICE_ID` | ESP32 设备 ID | — |
| `ESP32_BASE_URL` | ESP32 局域网地址 | `http://192.168.2.92` |

---

## 部署方案

本项目提供三种部署方案，详见各自文档：

| 方案 | 适合场景 | 文档 |
|------|----------|------|
| **Docker Compose** | 本地快速部署 | `docker-compose.yml` |
| **学生云服务器** | 传统部署（单机） | [deploy/学生云服务器部署指南.md](deploy/学生云服务器部署指南.md) |
| **华为云无服务器** | FunctionGraph + APIG | [huawei-cloud/部署指南.md](huawei-cloud/部署指南.md) |

华为云 IoTDA 详细配置参见 [huawei-cloud/IoTDA配置指南.md](huawei-cloud/IoTDA配置指南.md)。

---

## 功能页面

| 路由 | 页面 | 功能说明 |
|------|------|----------|
| `/login` | 登录 | 用户名密码 + 图形验证码 |
| `/register` | 注册 | 新用户注册 |
| `/home` | 农场仪表盘 | 系统主控制台 |
| `/realtime` | 实时监控 | 温湿度仪表盘 + 土壤/光照/CO₂ + 设备控制面板，5秒自动刷新 |
| `/historical` | 历史数据 | 多维趋势图表 + 数据表格 + 分页 + 时间范围筛选 |
| `/analysis` | 数据分析 | ECharts 数据可视化分析 |
| `/warning` | 预警日志 | 预警记录查看与处理 |
| `/settings` | 系统设置 | 预警规则 / ESP32 连接 / 异常检测 / 数据自动保存 |
| `/control-history` | 控制记录 | 设备控制操作历史 |
| `/profile` | 个人信息 | 修改密码、个人资料 |
| `/admin` | 管理员中心 | 用户管理（需管理员权限） |

---

## 许可证

MIT License

## 构建项目

克隆仓库并使用 Maven 构建：

```bash
git clone https://github.com/your-username/agri-backend.git
cd agri-backend
mvn clean install
