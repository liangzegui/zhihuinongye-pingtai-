# Copilot Instructions — 智慧农业监控平台

## 架构概览

全栈 IoT 智慧农业平台，三个独立组件通过 REST/WebSocket/HTTP 通信：

- **后端** (`idea/agri-backend/`): Spring Boot 3.5 + Java 21 + MyBatis-Plus + MySQL
- **前端** (`vue/agri-frontend/`): Vue 3 + Element Plus + ECharts + STOMP WebSocket
- **固件** (`ESP32 demo/`): ESP32 Arduino/PlatformIO，通过 HTTP 直连后端上报

共享端口配置在根目录 `project.config.json`（后端 8080，前端 8081），前后端均读取此文件。

## 数据流

1. **ESP32 → 后端**: 直连 HTTP (`Esp32BridgeService`)
2. **后端 → 前端**: REST API (`/api/*`) + WebSocket STOMP (`/ws` 端点, 订阅 `/topic/sensor-data`, `/topic/device-status`)
3. **设备控制**: 前端 → `DeviceControlController` → `Esp32BridgeService` HTTP 转发 → ESP32

## 构建与启动

```bash
# 后端（需要 MySQL agri_db 运行中）
cd idea/agri-backend && ./mvnw.cmd spring-boot:run   # Windows
cd idea/agri-backend && ./mvnw spring-boot:run        # Linux/Mac

# 前端
cd vue/agri-frontend && npm install && npm run serve
```

也可使用根目录 `启动项目.bat`（Windows）或 `启动项目.ps1`（PowerShell）一键启动。

## 后端约定

- **统一响应**: 所有 Controller 返回 `Result<T>`（见 `common/Result.java`），使用 `Result.success(data)` / `Result.error(msg)`
- **分层结构**: Controller → Service 接口 → `impl/` 实现类 → Mapper（MyBatis-Plus `BaseMapper<Entity>`）
- **JWT 认证**: `JwtInterceptor` 拦截所有路径，白名单: `/auth/**`, `/api/auth/**`, `/ws/**`, 静态资源。Token 从 `Authorization: Bearer xxx` 头获取
- **实体命名**: 表名 `t_env_data` 对应 `EnvDataEntity`，字段下划线自动转驼峰
- **Mapper**: 优先用 MyBatis-Plus 注解；复杂查询用 XML（见 `resources/mapper/EnvDataMapper.xml`）
- **密码**: BCrypt 加密（`UserService`），登录限流 5 次/10 分钟（`LoginAttemptService`）
- **环境变量**: `application.properties` 中所有敏感值均支持 `${ENV_VAR:default}` 格式覆盖

## 前端约定

- **API 层**: `src/api/` 按业务模块拆分（`auth.js`, `data.js`, `device.js`, `warning.js`, `config.js`），全部使用 `src/utils/request.js` 的 axios 实例
- **代理规则**: `/api/auth/*` → 后端 `/auth/*`（重写路径），其他 `/api/*` 直接转发（见 `vue.config.js`）
- **Token 存储**: `localStorage` key 为 `agri_platform_token`，工具方法见 `src/utils/token.js`
- **路由守卫**: 未设置 `meta.noAuth` 的路由均需 Token，无 Token 重定向到 `/login`
- **UI 组件库**: Element Plus，图表用 ECharts / vue-echarts
- **WebSocket**: `src/utils/websocket.js` 封装 SockJS + STOMP，自动重连 5 次

## 数据库表（MySQL `agri_db`）

| 表 | 用途 |
|----|------|
| `t_user` | 用户账号（BCrypt 密码） |
| `t_env_data` | 环境采集数据（温度/湿度/土壤/光照/CO₂） |
| `t_sensor` | 传感器元信息 |
| `t_warning_log` | 预警日志（status: 0=未处理, 1=已处理） |
| `t_warning_rule` | 预警阈值规则 |
| `t_device_status` | 设备最新状态与阈值配置 |

## ESP32 固件要点

- 入口 `ESP32 demo/src/main.cpp`，PlatformIO 构建
- 本地 AsyncWebServer（HTTP API 供后端直连）
- 自动控制阈值（温度/CO₂/土壤/光照）可通过网页或后端 API 动态调整
- WiFiManager 配网，支持 DHCP 和静态 IP

## 注意事项

- 代码注释和 UI 文本均为**中文**，新增代码应保持中文注释风格
- `User.java` 实体手写 getter/setter（未用 Lombok），其余实体用 `@Data`
