package com.example.agribackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.mapper.EnvDataMapper;
import com.example.agribackend.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DataServiceImpl implements DataService {
    @Autowired
    private EnvDataMapper envDataMapper;

    @Override
    public Page<EnvDataEntity> getHistoricalData(int page, int size, LocalDateTime startDate, LocalDateTime endDate,
            String sortOrder) {
        Page<EnvDataEntity> pageObj = new Page<>(page, size);

        // 构建查询条件
        LambdaQueryWrapper<EnvDataEntity> queryWrapper = new LambdaQueryWrapper<>();

        // 如果提供了时间范围，添加时间过滤条件
        if (startDate != null) {
            queryWrapper.ge(EnvDataEntity::getCollectTime, startDate);
        }
        if (endDate != null) {
            queryWrapper.le(EnvDataEntity::getCollectTime, endDate);
        }

        // 根据排序参数决定排序方向
        if ("asc".equalsIgnoreCase(sortOrder)) {
            queryWrapper.orderByAsc(EnvDataEntity::getCollectTime);
        } else {
            queryWrapper.orderByDesc(EnvDataEntity::getCollectTime);
        }

        return envDataMapper.selectPage(pageObj, queryWrapper);
    }
}