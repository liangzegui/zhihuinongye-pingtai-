package com.example.agribackend.service;

import com.example.agribackend.dto.WarningLogDTO;

import java.util.List;

public interface WarningService {
    void checkWarnings();  // 定时检查
    List<WarningLogDTO> getWarningLogs();  // 获取日志
}