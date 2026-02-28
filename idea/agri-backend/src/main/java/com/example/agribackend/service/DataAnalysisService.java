package com.example.agribackend.service;

import java.util.Map;

public interface DataAnalysisService {
    // 获取温度趋势数据
    Map<String, Object> getTemperatureTrend(String timeRange);

    // 获取湿度趋势数据
    Map<String, Object> getHumidityTrend(String timeRange);
    
    // 获取土壤和光照趋势数据
    Map<String, Object> getSoilTrend(String timeRange);
    
    // 获取CO2趋势数据
    Map<String, Object> getCO2Trend(String timeRange);
    
    // 获取数据统计摘要
    Map<String, Object> getDataSummary(String timeRange);

    // 获取首页仪表盘概览数据
    Map<String, Object> getDashboardOverview();
}