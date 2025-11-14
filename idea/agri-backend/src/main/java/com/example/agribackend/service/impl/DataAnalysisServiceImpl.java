package com.example.agribackend.service.impl;

import com.example.agribackend.mapper.EnvDataMapper;
import com.example.agribackend.service.DataAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DataAnalysisServiceImpl implements DataAnalysisService {
    @Autowired
    private EnvDataMapper envDataMapper;

    @Override
    public Map<String, Object> getTemperatureTrend(String timeRange) {
        // 示例逻辑: 查询平均温度, 返回 Echarts 数据
        // 实际用 SQL SELECT AVG(temperature), DATE(collect_time) GROUP BY DATE
        Map<String, Object> result = new HashMap<>();
        result.put("dates", new String[]{"2025-11-01", "2025-11-02"});
        result.put("temps", new Double[]{25.5, 30.0});
        return result;
    }
}