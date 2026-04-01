package com.example.agribackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.agribackend.common.Result;
import com.example.agribackend.dto.BatchIdsRequest;
import com.example.agribackend.entity.ControlHistoryEntity;
import com.example.agribackend.mapper.ControlHistoryMapper;
import com.example.agribackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备控制历史记录接口
 * 查看设备的操作记录（谁在什么时间控制了什么设备）
 */
@RestController
@RequestMapping("/api/control-history")
@Tag(name = "设备控制历史", description = "查询设备控制操作记录")
public class ControlHistoryController {

    @Autowired
    private ControlHistoryMapper controlHistoryMapper;

    @Autowired
    private UserService userService;

    /**
     * 分页查询控制历史
     */
    @GetMapping
    @Operation(summary = "分页查询控制历史记录")
    public Result<Map<String, Object>> getControlHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String controlType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String timeRange) {

        Page<ControlHistoryEntity> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<ControlHistoryEntity> qw = new LambdaQueryWrapper<>();

        // 控制类型筛选
        if (controlType != null && !controlType.isEmpty()) {
            qw.eq(ControlHistoryEntity::getControlType, controlType);
        }
        // 操作者筛选
        if (operator != null && !operator.isEmpty()) {
            qw.like(ControlHistoryEntity::getOperator, operator);
        }
        // 时间范围筛选
        if (timeRange != null && !timeRange.isEmpty()) {
            LocalDateTime start = resolveStartTime(timeRange);
            if (start != null) {
                qw.ge(ControlHistoryEntity::getCreateTime, start);
            }
        }
        qw.orderByDesc(ControlHistoryEntity::getCreateTime);

        Page<ControlHistoryEntity> result = controlHistoryMapper.selectPage(pageObj, qw);

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    /**
     * 获取控制统计概览
     */
    @GetMapping("/stats")
    @Operation(summary = "获取控制操作统计")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 总操作次数
        Long totalCount = controlHistoryMapper.selectCount(null);
        stats.put("totalCount", totalCount);

        // 今日操作次数
        LambdaQueryWrapper<ControlHistoryEntity> todayQw = new LambdaQueryWrapper<>();
        todayQw.ge(ControlHistoryEntity::getCreateTime, LocalDateTime.of(LocalDate.now(), LocalTime.MIN));
        Long todayCount = controlHistoryMapper.selectCount(todayQw);
        stats.put("todayCount", todayCount);

        // 成功率
        LambdaQueryWrapper<ControlHistoryEntity> successQw = new LambdaQueryWrapper<>();
        successQw.eq(ControlHistoryEntity::getResult, "success");
        Long successCount = controlHistoryMapper.selectCount(successQw);
        stats.put("successCount", successCount);
        stats.put("successRate", totalCount > 0 ? Math.round(successCount * 100.0 / totalCount) : 100);

        return Result.success(stats);
    }

    // ==================== 管理员操作 ====================

    /** 批量删除控制记录 */
    @DeleteMapping("/batch-delete")
    @Operation(summary = "批量删除控制历史记录")
    public Result<Map<String, Object>> batchDelete(@RequestBody BatchIdsRequest body,
            HttpServletRequest httpRequest) {
        if (!isAdminRequest(httpRequest)) {
            return Result.error(403, "仅管理员可执行此操作");
        }
        List<Integer> ids = body.getIds();
        if (ids == null || ids.isEmpty()) {
            return Result.error(400, "请选择要删除的记录");
        }
        int count = controlHistoryMapper.deleteByIds(ids);
        Map<String, Object> data = new HashMap<>();
        data.put("deletedCount", count);
        return Result.success(data);
    }

    /** 清空全部控制记录 */
    @DeleteMapping("/clear-all")
    @Operation(summary = "清空全部控制历史记录")
    public Result<Map<String, Object>> clearAll(HttpServletRequest httpRequest) {
        if (!isAdminRequest(httpRequest)) {
            return Result.error(403, "仅管理员可执行此操作");
        }
        int count = Math.toIntExact(controlHistoryMapper.selectCount(null));
        controlHistoryMapper.delete(null);
        Map<String, Object> data = new HashMap<>();
        data.put("clearedCount", count);
        return Result.success(data);
    }

    // ==================== 工具方法 ====================

    private boolean isAdminRequest(HttpServletRequest request) {
        Object loginUsername = request.getAttribute("loginUsername");
        if (loginUsername == null) {
            return false;
        }
        return userService.isAdmin(String.valueOf(loginUsername));
    }

    private LocalDateTime resolveStartTime(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange) {
            case "today" -> LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            case "week" -> now.minusDays(7);
            case "month" -> now.minusDays(30);
            default -> null;
        };
    }
}
