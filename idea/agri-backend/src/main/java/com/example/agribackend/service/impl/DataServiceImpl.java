package com.example.agribackend.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.mapper.EnvDataMapper;
import com.example.agribackend.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataServiceImpl implements DataService {
    @Autowired
    private EnvDataMapper envDataMapper;

    @Override
    public Page<EnvDataEntity> getHistoricalData(int page, int size) {
        Page<EnvDataEntity> pageObj = new Page<>(page, size);
        return envDataMapper.selectPage(pageObj, null);  //全查，之后加条件
    }
}