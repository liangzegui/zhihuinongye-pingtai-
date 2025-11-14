package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.dto.EnvDataDTO;
import com.example.agribackend.service.RealTimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/realtime")
public class RealTimeController {
    @Autowired
    private RealTimeService realTimeService;

    @GetMapping
    public Result<List<EnvDataDTO>> getData() {
        return Result.success(realTimeService.getLatestData());
    }
}