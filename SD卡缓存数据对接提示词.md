**需求**：ESP32 单片机新增了 SD 卡离线缓存功能和相关 HTTP 接口，需要对前后端进行对接。

---

## 一、离线缓存数据回传

当本地前后端系统与 ESP32 断开连接期间（连续3次超过20秒没轮询 `/data`），ESP32 会自动将传感器数据缓存到 SD 卡。当系统重新连接后，ESP32 的 `/data` 接口响应中会包含 `hasCachedData: true` 字段。此时需要调用 `GET /cachedData` 获取断连期间的所有历史数据，并保存到数据库。

**`GET /cachedData` 返回格式**：
```json
{
  "cached": [
    {"ts":"2026-04-06 14:20:10","temp":28,"humi":55,"soil":2900,"lightLux":1200,"eco2":450,"tvoc":10,"pump":false,"fan":false,"light":false,"manual":false},
    {"ts":"2026-04-06 14:20:20","temp":28,"humi":56,"soil":2850,"lightLux":1180,"eco2":440,"tvoc":8,"pump":false,"fan":false,"light":false,"manual":false}
  ]
}
```
每条记录的 `ts` 字段是该数据在 ESP32 上的实际采集时间，存入数据库时应将 `ts` 作为 `collectTime`（采集时间）。

**后端修改**：

1. **`Esp32BridgeServiceImpl.java`**：新增一个方法 `fetchCachedData()`，功能是调用 ESP32 的 `/cachedData` 接口，解析返回的 JSON 数组，将每条数据转为 `EnvDataEntity`（字段映射：`temp→temperature`, `humi→humidity`, `soil→soilAdc`, `lightLux→lightIntensity`, `eco2→co2`, `ts→collectTime`），批量保存到数据库。土壤百分比 `soilMoisture` 按现有的 ADC→百分比换算公式计算。

2. **`DataAutoSaveService.java`** 或 **`RealTimeController.java`**：在获取 `/data` 实时数据后，检查响应中的 `hasCachedData` 字段。如果为 `true`，自动调用 `fetchCachedData()` 拉取并保存所有缓存数据。只需在第一次检测到时触发一次，避免重复拉取。

**前端修改**：

3. **`RealTime.vue`**（可选）：在实时数据中检测到 `hasCachedData: true` 时，弹出一个通知提示"已接收断连期间的 N 条缓存数据"。

**注意事项**：
- ESP32 的 `/cachedData` 被调用一次后缓存文件就会被删除，所以不能重复调用
- 缓存数据可能有几十到几百条，需要批量插入数据库
- `saveUsername` 字段可以设置为 `"system-cached"` 标识这是自动恢复的缓存数据
- ESP32 会自动过滤异常数据（所有传感器值为0时不缓存），所以回传的数据都是有效数据

---

## 二、离线缓存间隔设置（放在系统设置页面）

ESP32 新增了两个 HTTP 接口用于管理离线缓存间隔：

**`GET /getCacheInterval` 返回格式**：
```json
{
  "cacheInterval": 10,          // 当前缓存间隔（秒）
  "sdCardAvailable": true,      // SD卡是否可用
  "webClientConnected": true,   // 网页端是否在线
  "hasCachedData": false,       // 是否有未取走的缓存数据
  "cachedFileSize": 0           // 缓存文件大小（字节）
}
```

**`GET /setCacheInterval?interval=10` 设置缓存间隔**：
- 参数 `interval`：缓存间隔秒数，范围 **5-3600**（5秒到1小时）
- 超出范围返回 400 错误
- 设置后 ESP32 会持久化保存到 Flash，重启不丢失

**前端修改**：

在 **系统设置页面** 中新增"离线缓存设置"区域（与现有的"单片机连接配置"、"数据保存"等设置并列），要求如下：

1. **页面布局**：
   - 标题："离线缓存设置"，带 SD 卡图标
   - 显示 SD 卡状态（可用/不可用）
   - 显示当前缓存间隔，提供输入框可修改（范围 5-3600 秒）
   - 显示当前网页端连接状态（在线/离线）
   - 显示缓存数据状态（无缓存 / 有缓存 N 字节）
   - 一个"保存设置"按钮
   - 一个"刷新状态"按钮

2. **设置逻辑要严谨**：
   - 输入框限制范围 5-3600，前端校验不合法输入，给出明确提示
   - 页面加载时通过 `GET /getCacheInterval` 获取当前值
   - 如果 ESP32 未连接（请求失败），所有控件禁用，显示"ESP32 未连接"
   - 如果 SD 卡不可用（`sdCardAvailable: false`），显示警告提示

3. **后端代理**：
   - 后端需要新增两个代理接口，将请求转发到 ESP32：
     - `GET /api/config/cacheInterval` → 转发到 ESP32 的 `/getCacheInterval`
     - `POST /api/config/cacheInterval` body: `{"interval": 10}` → 转发到 ESP32 的 `/setCacheInterval?interval=10`
   - 在 `ConfigController.java` 或相应的控制器中添加
