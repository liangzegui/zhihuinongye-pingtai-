package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.dto.BatchIdsRequest;
import com.example.agribackend.service.UserService;
import com.example.agribackend.service.WarningService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warning")
public class WarningController {
    @Autowired
    private WarningService warningService;

    @Autowired
    private UserService userService;

    /** 分页 + 筛选查询（前端主入口） */
    @GetMapping("/logs")
    public Result<Map<String, Object>> getWarningLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String warningType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String timeRange) {

        Map<String, Object> data = warningService.getWarningLogsPaged(page, pageSize, warningType, status, timeRange);
        return Result.success(data);
    }

    /** 标记单条为已处理 */
    @PutMapping("/logs/{id}/handle")
    public Result<Void> markAsHandled(@PathVariable Integer id) {
        if (warningService.markAsHandled(id)) {
            return Result.ok("已标记为已处理");
        }
        return Result.error(400, "操作失败，日志不存在");
    }

    /** 批量标记为已处理 */
    @PutMapping("/logs/batch-handle")
    public Result<Map<String, Object>> batchMarkAsHandled(@RequestBody BatchIdsRequest body) {
        List<Integer> ids = body.getIds();
        if (ids == null || ids.isEmpty()) {
            return Result.error(400, "请选择要处理的日志");
        }
        int count = warningService.batchMarkAsHandled(ids);
        Map<String, Object> data = new HashMap<>();
        data.put("handledCount", count);
        return Result.success(data);
    }

    /** 批量删除日志（管理员） */
    @DeleteMapping("/logs/batch-delete")
    public Result<Map<String, Object>> batchDelete(@RequestBody BatchIdsRequest body,
            HttpServletRequest httpRequest) {
        if (!isAdminRequest(httpRequest)) {
            return Result.error(403, "仅管理员可执行此操作");
        }
        List<Integer> ids = body.getIds();
        if (ids == null || ids.isEmpty()) {
            return Result.error(400, "请选择要删除的日志");
        }
        int count = warningService.batchDelete(ids);
        Map<String, Object> data = new HashMap<>();
        data.put("deletedCount", count);
        return Result.success(data);
    }

    /** 清空已处理日志（管理员） */
    @DeleteMapping("/logs/clear-handled")
    public Result<Map<String, Object>> clearHandled(HttpServletRequest httpRequest) {
        if (!isAdminRequest(httpRequest)) {
            return Result.error(403, "仅管理员可执行此操作");
        }
        int count = warningService.clearHandled();
        Map<String, Object> data = new HashMap<>();
        data.put("clearedCount", count);
        return Result.success(data);
    }

    private boolean isAdminRequest(HttpServletRequest request) {
        Object loginUsername = request.getAttribute("loginUsername");
        if (loginUsername == null) {
            return false;
        }
        return userService.isAdmin(String.valueOf(loginUsername));
    }
}