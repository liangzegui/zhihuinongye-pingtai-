package com.example.agribackend.service;

import com.example.agribackend.dto.WarningLogDTO;

import java.util.List;
import java.util.Map;

public interface WarningService {
    /** 定时检查（含所有传感器维度） */
    void checkWarnings();

    /** 获取全部日志（旧接口兼容） */
    List<WarningLogDTO> getWarningLogs();

    /** 分页 + 筛选查询警告日志 */
    Map<String, Object> getWarningLogsPaged(int page, int pageSize,
            String warningType, Integer status, String timeRange);

    /** 标记为已处理 */
    boolean markAsHandled(Integer id);

    /** 批量标记为已处理 */
    int batchMarkAsHandled(List<Integer> ids);

    /** 批量删除警告日志 */
    int batchDelete(List<Integer> ids);

    /** 清空所有已处理的日志 */
    int clearHandled();
}