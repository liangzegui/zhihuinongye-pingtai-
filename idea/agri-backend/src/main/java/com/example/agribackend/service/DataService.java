package com.example.agribackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.agribackend.entity.EnvDataEntity;

public interface DataService {
    Page<EnvDataEntity> getHistoricalData(int page, int size); //分页查询
}