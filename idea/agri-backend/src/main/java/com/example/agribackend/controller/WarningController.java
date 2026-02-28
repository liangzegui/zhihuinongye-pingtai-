package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.dto.WarningLogDTO;
import com.example.agribackend.service.WarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warning")
public class WarningController {
    @Autowired
    private WarningService warningService;

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

    /** 标记为已处理 */
    @PutMapping("/logs/{id}/handle")
    public Result<Void> markAsHandled(@PathVariable Integer id) {
        if (warningService.markAsHandled(id)) {
            return Result.ok("已标记为已处理");
        }
        return Result.error(400, "操作失败，日志不存在");
    }
}