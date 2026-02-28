package com.example.agribackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.agribackend.entity.ExceptionConfigEntity;
import com.example.agribackend.mapper.ExceptionConfigMapper;
import com.example.agribackend.service.ExceptionConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 异常配置服务实现
 * 从数据库 t_exception_config 读取配置
 */
@Service
public class ExceptionConfigServiceImpl implements ExceptionConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionConfigServiceImpl.class);

    @Autowired
    private ExceptionConfigMapper configMapper;

    @Override
    public Map<String, String> getAllConfigMap() {
        List<ExceptionConfigEntity> list = configMapper.selectList(null);
        Map<String, String> map = new LinkedHashMap<>();
        for (ExceptionConfigEntity e : list) {
            map.put(e.getConfigKey(), e.getConfigValue());
        }
        return map;
    }

    @Override
    public Map<String, String> getGroupConfig(String group) {
        QueryWrapper<ExceptionConfigEntity> qw = new QueryWrapper<>();
        qw.eq("config_group", group);
        List<ExceptionConfigEntity> list = configMapper.selectList(qw);
        Map<String, String> map = new LinkedHashMap<>();
        for (ExceptionConfigEntity e : list) {
            map.put(e.getConfigKey(), e.getConfigValue());
        }
        return map;
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        try {
            QueryWrapper<ExceptionConfigEntity> qw = new QueryWrapper<>();
            qw.eq("config_key", key);
            ExceptionConfigEntity entity = configMapper.selectOne(qw);
            return entity != null ? entity.getConfigValue() : defaultValue;
        } catch (Exception e) {
            logger.warn("读取异常配置失败 key={}: {}", key, e.getMessage());
            return defaultValue;
        }
    }

    @Override
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String val = getConfigValue(key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(val);
    }

    @Override
    public int getIntConfig(String key, int defaultValue) {
        String val = getConfigValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public double getDoubleConfig(String key, double defaultValue) {
        String val = getConfigValue(key, String.valueOf(defaultValue));
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
