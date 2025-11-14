package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.dto.WarningLogDTO;
import com.example.agribackend.service.WarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warning")
public class WarningController {
    @Autowired
    private WarningService warningService;

    @GetMapping("/logs")
    public Result<List<WarningLogDTO>> getWarningLogs() {
        return Result.success(warningService.getWarningLogs());
    }
}