package com.example.agribackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.agribackend.common.Result;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.entity.User;
import com.example.agribackend.mapper.EnvDataMapper;
import com.example.agribackend.service.DataAutoSaveService;
import com.example.agribackend.service.UserService;
import com.example.agribackend.utils.PasswordValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员控制器
 * 提供：用户管理（增删改查）+ 自动保存管理
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private DataAutoSaveService dataAutoSaveService;

    @Autowired
    private EnvDataMapper envDataMapper;

    // ==================== 用户管理（CRUD） ====================

    /**
     * 管理员概览统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        Map<String, Object> stats = new HashMap<>();

        // 用户统计
        List<User> allUsers = userService.getAllUsers();
        stats.put("userCount", allUsers.size());
        stats.put("adminCount", allUsers.stream().filter(u -> "admin".equalsIgnoreCase(u.getRole())).count());

        // 数据统计
        Long totalData = envDataMapper.selectCount(null);
        stats.put("totalDataCount", totalData);

        // 今日数据量
        LambdaQueryWrapper<EnvDataEntity> todayQw = new LambdaQueryWrapper<>();
        todayQw.ge(EnvDataEntity::getCollectTime, LocalDateTime.now().toLocalDate().atStartOfDay());
        Long todayData = envDataMapper.selectCount(todayQw);
        stats.put("todayDataCount", todayData);

        // 自动保存状态
        stats.put("autoSaveEnabled", dataAutoSaveService.isEnabled());
        stats.put("autoSaveInterval", dataAutoSaveService.getSaveIntervalSeconds());
        stats.put("autoSaveConfiguredBy", dataAutoSaveService.getConfiguredBy());

        // 保存人列表（去重，用于前端筛选下拉）
        LambdaQueryWrapper<EnvDataEntity> distinctQw = new LambdaQueryWrapper<>();
        distinctQw.select(EnvDataEntity::getSaveUsername).groupBy(EnvDataEntity::getSaveUsername);
        List<EnvDataEntity> saverList = envDataMapper.selectList(distinctQw);
        List<String> saverNames = saverList.stream()
                .map(EnvDataEntity::getSaveUsername)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        stats.put("saverNames", saverNames);

        return Result.success(stats);
    }

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> listUsers(HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        List<Map<String, Object>> users = userService.getAllUsers().stream()
                .map(this::toSafeUser)
                .collect(Collectors.toList());
        return Result.success(users);
    }

    @GetMapping("/users/{id}")
    public Result<Map<String, Object>> getUserById(@PathVariable Integer id, HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        User user = userService.getUserById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(toSafeUser(user));
    }

    @PostMapping("/users")
    public Result<Void> createUser(@RequestBody Map<String, String> data, HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        String username = data.get("username");
        String password = data.get("password");
        String role = data.getOrDefault("role", "user");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Result.error(400, "用户名和密码不能为空");
        }

        if (!"admin".equalsIgnoreCase(role) && !"user".equalsIgnoreCase(role)) {
            return Result.error(400, "角色仅支持 admin 或 user");
        }

        String pwdError = PasswordValidator.validate(password);
        if (pwdError != null) {
            return Result.error(400, pwdError);
        }

        boolean success = userService.createUserByAdmin(username.trim(), password.trim(), role.toLowerCase());
        if (!success) {
            return Result.error(400, "创建失败，用户名可能已存在");
        }

        return Result.ok("创建用户成功");
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Integer id,
                                   @RequestBody Map<String, String> data,
                                   HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        User existing = userService.getUserById(id);
        if (existing == null) {
            return Result.error(404, "用户不存在");
        }

        String username = data.get("username");
        String role = data.get("role");
        String password = data.get("password");

        // 防止降权最后一个管理员
        if ("admin".equalsIgnoreCase(existing.getRole())
                && role != null
                && !"admin".equalsIgnoreCase(role)
                && userService.countAdminUsers() <= 1) {
            return Result.error(400, "系统至少需要保留一个管理员");
        }

        if (role != null && !"admin".equalsIgnoreCase(role) && !"user".equalsIgnoreCase(role)) {
            return Result.error(400, "角色仅支持 admin 或 user");
        }

        if (password != null && !password.trim().isEmpty()) {
            String pwdError = PasswordValidator.validate(password);
            if (pwdError != null) {
                return Result.error(400, pwdError);
            }
        }

        boolean success = userService.updateUserByAdmin(
                id,
                username != null ? username.trim() : null,
                role != null ? role.toLowerCase() : null,
                password != null ? password.trim() : null
        );

        if (!success) {
            return Result.error(400, "更新失败，用户名可能已存在");
        }

        return Result.ok("更新用户成功");
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Integer id, HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        User existing = userService.getUserById(id);
        if (existing == null) {
            return Result.error(404, "用户不存在");
        }

        // 不允许删除最后一个管理员
        if ("admin".equalsIgnoreCase(existing.getRole()) && userService.countAdminUsers() <= 1) {
            return Result.error(400, "系统至少需要保留一个管理员");
        }

        boolean success = userService.deleteUserById(id);
        if (!success) {
            return Result.error(400, "删除失败");
        }

        return Result.ok("删除用户成功");
    }

    // ==================== 自动保存管理 ====================

    @GetMapping("/autosave")
    public Result<Map<String, Object>> getAutoSaveConfig(HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        Map<String, Object> config = new HashMap<>();
        config.put("enabled", dataAutoSaveService.isEnabled());
        config.put("intervalSeconds", dataAutoSaveService.getSaveIntervalSeconds());

        long lastSaveTime = dataAutoSaveService.getLastSaveTime();
        if (lastSaveTime > 0) {
            LocalDateTime lastSave = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(lastSaveTime), ZoneId.systemDefault());
            config.put("lastSaveTime", lastSave.toString());
        } else {
            config.put("lastSaveTime", null);
        }

        return Result.success(config);
    }

    @PostMapping("/autosave")
    public Result<Map<String, Object>> updateAutoSaveConfig(@RequestBody Map<String, Object> configData,
                                                             HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        String loginUsername = String.valueOf(request.getAttribute("loginUsername"));

        if (configData.containsKey("enabled")) {
            boolean enabled = Boolean.parseBoolean(String.valueOf(configData.get("enabled")));
            dataAutoSaveService.setEnabled(enabled, loginUsername);
        } else {
            // 即使只改间隔，也更新配置人
            dataAutoSaveService.setConfiguredBy(loginUsername);
        }

        if (configData.containsKey("intervalSeconds")) {
            long interval = Long.parseLong(String.valueOf(configData.get("intervalSeconds")));
            dataAutoSaveService.setSaveIntervalSeconds(interval);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("enabled", dataAutoSaveService.isEnabled());
        result.put("intervalSeconds", dataAutoSaveService.getSaveIntervalSeconds());
        result.put("configuredBy", dataAutoSaveService.getConfiguredBy());
        result.put("message", "配置已更新");
        return Result.success(result);
    }

    @PostMapping("/autosave/trigger")
    public Result<Map<String, Object>> triggerSave(HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        String loginUsername = String.valueOf(request.getAttribute("loginUsername"));
        boolean success = dataAutoSaveService.manualSave(loginUsername);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "数据保存成功" : "数据保存失败：设备离线或无有效数据");
        return Result.success(result);
    }

    // ==================== 保存数据管理（CRUD） ====================

    @GetMapping("/env-data")
    public Result<Page<EnvDataEntity>> listEnvData(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String saveUsername,
            HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        Page<EnvDataEntity> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<EnvDataEntity> qw = new LambdaQueryWrapper<>();

        LocalDateTime start = parseDateTime(startDate);
        LocalDateTime end = parseDateTime(endDate);
        if (start != null) {
            qw.ge(EnvDataEntity::getCollectTime, start);
        }
        if (end != null) {
            qw.le(EnvDataEntity::getCollectTime, end);
        }
        if (saveUsername != null && !saveUsername.trim().isEmpty()) {
            qw.eq(EnvDataEntity::getSaveUsername, saveUsername.trim());
        }

        qw.orderByDesc(EnvDataEntity::getCollectTime);
        return Result.success(envDataMapper.selectPage(pageObj, qw));
    }

    @PostMapping("/env-data")
    public Result<Void> createEnvData(@RequestBody Map<String, Object> data, HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        String loginUsername = String.valueOf(request.getAttribute("loginUsername"));

        EnvDataEntity entity = new EnvDataEntity();
        entity.setSensorId(readInteger(data, "sensorId", 1));
        entity.setTemperature(readDouble(data, "temperature", null));
        entity.setHumidity(readDouble(data, "humidity", null));
        entity.setSoilMoisture(readDouble(data, "soilMoisture", null));
        entity.setSoilAdc(readInteger(data, "soilAdc", null));
        entity.setLightIntensity(readInteger(data, "lightIntensity", null));
        entity.setCo2(readInteger(data, "co2", null));
        entity.setSaveUsername(loginUsername);
        entity.setCollectTime(parseDateTime(String.valueOf(data.getOrDefault("collectTime", ""))));
        if (entity.getCollectTime() == null) {
            entity.setCollectTime(LocalDateTime.now());
        }

        int inserted = envDataMapper.insert(entity);
        if (inserted <= 0) {
            return Result.error(400, "新增保存数据失败");
        }
        return Result.ok("新增保存数据成功");
    }

    @PutMapping("/env-data/{id}")
    public Result<Void> updateEnvData(@PathVariable Long id,
                                      @RequestBody Map<String, Object> data,
                                      HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        String loginUsername = String.valueOf(request.getAttribute("loginUsername"));

        EnvDataEntity existing = envDataMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "数据记录不存在");
        }

        existing.setSensorId(readInteger(data, "sensorId", existing.getSensorId()));
        existing.setTemperature(readDouble(data, "temperature", existing.getTemperature()));
        existing.setHumidity(readDouble(data, "humidity", existing.getHumidity()));
        existing.setSoilMoisture(readDouble(data, "soilMoisture", existing.getSoilMoisture()));
        existing.setSoilAdc(readInteger(data, "soilAdc", existing.getSoilAdc()));
        existing.setLightIntensity(readInteger(data, "lightIntensity", existing.getLightIntensity()));
        existing.setCo2(readInteger(data, "co2", existing.getCo2()));
        existing.setSaveUsername(loginUsername);

        String collectTime = String.valueOf(data.getOrDefault("collectTime", ""));
        LocalDateTime collect = parseDateTime(collectTime);
        if (collect != null) {
            existing.setCollectTime(collect);
        }

        int updated = envDataMapper.updateById(existing);
        if (updated <= 0) {
            return Result.error(400, "更新保存数据失败");
        }
        return Result.ok("更新保存数据成功");
    }

    @DeleteMapping("/env-data/{id}")
    public Result<Void> deleteEnvData(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        EnvDataEntity existing = envDataMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "数据记录不存在");
        }

        int deleted = envDataMapper.deleteById(id);
        if (deleted <= 0) {
            return Result.error(400, "删除保存数据失败");
        }
        return Result.ok("删除保存数据成功");
    }

    @PostMapping("/env-data/batch-delete")
    public Result<Map<String, Object>> batchDeleteEnvData(@RequestBody Map<String, Object> payload,
                                                           HttpServletRequest request) {
        if (!isAdminRequest(request)) {
            return Result.error(403, "仅管理员可访问");
        }

        Object idsObj = payload.get("ids");
        if (idsObj == null) {
            idsObj = payload.get("idList");
        }

        List<?> rawIds;
        if (idsObj instanceof List<?> listObj) {
            rawIds = listObj;
        } else if (idsObj instanceof String str && !str.trim().isEmpty()) {
            rawIds = List.of(str.split(","));
        } else {
            rawIds = null;
        }

        if (rawIds == null || rawIds.isEmpty()) {
            return Result.error(400, "请选择要删除的记录");
        }

        List<Long> ids = rawIds.stream()
                .map(this::safeToLong)
                .filter(v -> v != null)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Result.error(400, "删除参数无效");
        }

        int deleted = envDataMapper.deleteBatchIds(ids);
        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deleted);
        result.put("requestedCount", ids.size());
        return Result.success(result);
    }

    // ==================== 私有方法 ====================

    private boolean isAdminRequest(HttpServletRequest request) {
        Object loginUsername = request.getAttribute("loginUsername");
        if (loginUsername == null) {
            return false;
        }
        return userService.isAdmin(String.valueOf(loginUsername));
    }

    private Map<String, Object> toSafeUser(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("role", user.getRole());
        map.put("createTime", user.getCreateTime());
        return map;
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty() || "null".equalsIgnoreCase(text.trim())) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim().replace(" ", "T"));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer readInteger(Map<String, Object> data, String key, Integer defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Double readDouble(Map<String, Object> data, String key, Double defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Long safeToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
