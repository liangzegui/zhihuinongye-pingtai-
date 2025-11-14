package com.example.agribackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.example.agribackend.dto.EnvDataDTO;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.mapper.EnvDataMapper;
import com.example.agribackend.service.RealTimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RealTimeServiceImpl implements RealTimeService {
    @Autowired
    private EnvDataMapper envDataMapper;

    @Override
    public List<EnvDataDTO> getLatestData() {
        List<EnvDataEntity> list = envDataMapper.getLatestDataBySensor();
        return BeanUtil.copyToList(list, EnvDataDTO.class);
    }
}