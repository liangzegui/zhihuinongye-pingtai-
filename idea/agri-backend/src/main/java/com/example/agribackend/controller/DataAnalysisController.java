package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.service.DataAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class DataAnalysisController {
    @Autowired
    private DataAnalysisService dataAnalysisService;

    @GetMapping("/temperature-trend")
    public Result<Map<String, Object>> getTemperatureTrend(@RequestParam String timeRange) {
        return Result.success(dataAnalysisService.getTemperatureTrend(timeRange));
    }
}