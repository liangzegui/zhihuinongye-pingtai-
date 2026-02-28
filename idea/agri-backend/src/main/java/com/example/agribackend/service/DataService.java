package com.example.agribackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.agribackend.entity.EnvDataEntity;

import java.time.LocalDateTime;

public interface DataService {
    Page<EnvDataEntity> getHistoricalData(int page, int size, LocalDateTime startDate, LocalDateTime endDate); //分页查询，支持时间范围
}