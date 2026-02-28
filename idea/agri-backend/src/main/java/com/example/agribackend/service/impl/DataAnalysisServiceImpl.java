package com.example.agribackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.entity.WarningLogEntity;
import com.example.agribackend.mapper.EnvDataMapper;
import com.example.agribackend.mapper.WarningLogMapper;
import com.example.agribackend.service.DataAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DataAnalysisServiceImpl implements DataAnalysisService {
    @Autowired
    private EnvDataMapper envDataMapper;
    @Autowired
    private WarningLogMapper warningLogMapper;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private LocalDateTime getStartTime(String timeRange) {
        if (timeRange == null || timeRange.equals("all")) return null;
        LocalDateTime now = LocalDateTime.now();
        return switch (timeRange) {
            case "1h"    -> now.minusHours(1);
            case "6h"    -> now.minusHours(6);
            case "12h"   -> now.minusHours(12);
            case "24h"   -> now.minusDays(1);
            case "7day"  -> now.minusDays(7);
            case "30day" -> now.minusDays(30);
            case "90day" -> now.minusDays(90);
            default      -> null;
        };
    }
    
    private List<EnvDataEntity> getDataInRange(String timeRange) {
        LocalDateTime startTime = getStartTime(timeRange);
        LambdaQueryWrapper<EnvDataEntity> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) wrapper.ge(EnvDataEntity::getCollectTime, startTime);
        wrapper.orderByAsc(EnvDataEntity::getCollectTime);
        return envDataMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getTemperatureTrend(String timeRange) {
        List<EnvDataEntity> dataList = getDataInRange(timeRange);
        List<String> timestamps = new ArrayList<>();
        List<Double> temperatures = new ArrayList<>();
        List<Double> humidities = new ArrayList<>();
        for (EnvDataEntity d : dataList) {
            if (d.getCollectTime() != null) {
                timestamps.add(d.getCollectTime().format(FORMATTER));
                temperatures.add(d.getTemperature() != null ? d.getTemperature() : 0.0);
                humidities.add(d.getHumidity() != null ? d.getHumidity() : 0.0);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("timestamps", timestamps);
        result.put("data", temperatures);
        result.put("humidity", humidities);
        return result;
    }

    @Override
    public Map<String, Object> getHumidityTrend(String timeRange) {
        List<EnvDataEntity> dataList = getDataInRange(timeRange);
        List<String> timestamps = new ArrayList<>();
        List<Double> humidities = new ArrayList<>();
        for (EnvDataEntity d : dataList) {
            if (d.getCollectTime() != null) {
                timestamps.add(d.getCollectTime().format(FORMATTER));
                humidities.add(d.getHumidity() != null ? d.getHumidity() : 0.0);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("timestamps", timestamps);
        result.put("data", humidities);
        return result;
    }

    @Override
    public Map<String, Object> getSoilTrend(String timeRange) {
        List<EnvDataEntity> dataList = getDataInRange(timeRange);
        List<String> timestamps = new ArrayList<>();
        List<Integer> soilAdcValues = new ArrayList<>();
        List<Integer> lightValues = new ArrayList<>();
        for (EnvDataEntity d : dataList) {
            if (d.getCollectTime() != null) {
                timestamps.add(d.getCollectTime().format(FORMATTER));
                soilAdcValues.add(d.getSoilAdc() != null ? d.getSoilAdc() : 0);
                lightValues.add(d.getLightIntensity() != null ? d.getLightIntensity() : 0);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("timestamps", timestamps);
        result.put("data", soilAdcValues);
        result.put("light", lightValues);
        return result;
    }

    @Override
    public Map<String, Object> getCO2Trend(String timeRange) {
        List<EnvDataEntity> dataList = getDataInRange(timeRange);
        List<String> timestamps = new ArrayList<>();
        List<Integer> co2Values = new ArrayList<>();
        for (EnvDataEntity d : dataList) {
            if (d.getCollectTime() != null) {
                timestamps.add(d.getCollectTime().format(FORMATTER));
                co2Values.add(d.getCo2() != null ? d.getCo2() : 0);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("timestamps", timestamps);
        result.put("data", co2Values);
        return result;
    }

    @Override
    public Map<String, Object> getDataSummary(String timeRange) {
        List<EnvDataEntity> dataList = getDataInRange(timeRange);
        if (dataList.isEmpty()) return getEmptySummary();

        double avgTemp  = dataList.stream().filter(d -> d.getTemperature()    != null).mapToDouble(EnvDataEntity::getTemperature).average().orElse(0);
        double avgHumi  = dataList.stream().filter(d -> d.getHumidity()       != null).mapToDouble(EnvDataEntity::getHumidity).average().orElse(0);
        double avgLight = dataList.stream().filter(d -> d.getLightIntensity() != null).mapToInt(EnvDataEntity::getLightIntensity).average().orElse(0);
        double avgCO2   = dataList.stream().filter(d -> d.getCo2()           != null).mapToInt(EnvDataEntity::getCo2).average().orElse(0);

        int halfSize = dataList.size() / 2;
        if (halfSize > 0) {
            List<EnvDataEntity> first  = dataList.subList(0, halfSize);
            List<EnvDataEntity> second = dataList.subList(halfSize, dataList.size());

            double tempTrend  = trend(first, second, d -> d.getTemperature()    != null ? d.getTemperature() : 0.0);
            double humiTrend  = trend(first, second, d -> d.getHumidity()       != null ? d.getHumidity() : 0.0);
            double lightTrend = trend(first, second, d -> d.getLightIntensity() != null ? (double) d.getLightIntensity() : 0.0);
            double co2Trend   = trend(first, second, d -> d.getCo2()           != null ? (double) d.getCo2() : 0.0);

            Map<String, Object> r = new HashMap<>();
            r.put("avgTemp",    Math.round(avgTemp  * 10.0) / 10.0);
            r.put("avgHumi",    Math.round(avgHumi  * 10.0) / 10.0);
            r.put("avgLight",   (int) avgLight);
            r.put("avgCO2",     (int) avgCO2);
            r.put("tempTrend",  Math.round(tempTrend  * 10.0) / 10.0);
            r.put("humiTrend",  Math.round(humiTrend  * 10.0) / 10.0);
            r.put("lightTrend", Math.round(lightTrend * 10.0) / 10.0);
            r.put("co2Trend",   Math.round(co2Trend   * 10.0) / 10.0);
            r.put("dataCount",  dataList.size());
            return r;
        }
        return getEmptySummary();
    }

    @Override
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> overview = new HashMap<>();

        // 最新一条环境数据
        LambdaQueryWrapper<EnvDataEntity> latestWrap = new LambdaQueryWrapper<>();
        latestWrap.orderByDesc(EnvDataEntity::getCollectTime).last("LIMIT 1");
        EnvDataEntity latest = envDataMapper.selectOne(latestWrap);

        if (latest != null) {
            overview.put("temperature",    latest.getTemperature());
            overview.put("humidity",       latest.getHumidity());
            overview.put("soilAdc",        latest.getSoilAdc());
            overview.put("lightIntensity", latest.getLightIntensity());
            overview.put("co2",            latest.getCo2());
            overview.put("collectTime",    latest.getCollectTime());
        }

        // 今日数据条数
        LambdaQueryWrapper<EnvDataEntity> todayWrap = new LambdaQueryWrapper<>();
        todayWrap.ge(EnvDataEntity::getCollectTime, LocalDateTime.now().toLocalDate().atStartOfDay());
        overview.put("todayDataCount", envDataMapper.selectCount(todayWrap));

        // 未处理预警数
        QueryWrapper<WarningLogEntity> warnWrap = new QueryWrapper<>();
        warnWrap.eq("status", 0);
        overview.put("unhandledWarnings", warningLogMapper.selectCount(warnWrap));

        // 总数据量
        overview.put("totalDataCount", envDataMapper.selectCount(null));

        return overview;
    }

    // ==================== 工具方法 ====================

    @FunctionalInterface
    private interface ValueExtractor { double extract(EnvDataEntity e); }

    private double trend(List<EnvDataEntity> first, List<EnvDataEntity> second, ValueExtractor ex) {
        double a1 = first.stream().mapToDouble(ex::extract).average().orElse(0);
        double a2 = second.stream().mapToDouble(ex::extract).average().orElse(0);
        return a1 == 0 ? 0 : ((a2 - a1) / a1) * 100;
    }

    private Map<String, Object> getEmptySummary() {
        Map<String, Object> r = new HashMap<>();
        r.put("avgTemp", 0); r.put("avgHumi", 0); r.put("avgLight", 0); r.put("avgCO2", 0);
        r.put("tempTrend", 0); r.put("humiTrend", 0); r.put("lightTrend", 0); r.put("co2Trend", 0);
        r.put("dataCount", 0);
        return r;
    }
}