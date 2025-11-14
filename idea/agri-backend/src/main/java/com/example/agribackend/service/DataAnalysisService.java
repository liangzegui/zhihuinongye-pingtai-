package com.example.agribackend.service;

import java.util.Map;

public interface DataAnalysisService {
    Map<String, Object> getTemperatureTrend(String timeRange);  // 示例: 温度趋势, 返回Map for Echarts
}