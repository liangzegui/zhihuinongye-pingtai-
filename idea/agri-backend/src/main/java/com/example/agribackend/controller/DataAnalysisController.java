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

    /** 温度趋势（含湿度附带） */
    @GetMapping("/temperature-trend")
    public Result<Map<String, Object>> getTemperatureTrend(
            @RequestParam(defaultValue = "7day") String timeRange) {
        return Result.success(dataAnalysisService.getTemperatureTrend(timeRange));
    }

    /** 湿度趋势（独立） */
    @GetMapping("/humidity-trend")
    public Result<Map<String, Object>> getHumidityTrend(
            @RequestParam(defaultValue = "7day") String timeRange) {
        return Result.success(dataAnalysisService.getHumidityTrend(timeRange));
    }

    /** 土壤 & 光照趋势 */
    @GetMapping("/soil-trend")
    public Result<Map<String, Object>> getSoilTrend(
            @RequestParam(defaultValue = "7day") String timeRange) {
        return Result.success(dataAnalysisService.getSoilTrend(timeRange));
    }

    /** CO₂趋势 */
    @GetMapping("/co2-trend")
    public Result<Map<String, Object>> getCO2Trend(
            @RequestParam(defaultValue = "7day") String timeRange) {
        return Result.success(dataAnalysisService.getCO2Trend(timeRange));
    }

    /** 数据统计摘要 */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getDataSummary(
            @RequestParam(defaultValue = "7day") String timeRange) {
        return Result.success(dataAnalysisService.getDataSummary(timeRange));
    }

    /** 首页仪表盘概览 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {
        return Result.success(dataAnalysisService.getDashboardOverview());
    }
}