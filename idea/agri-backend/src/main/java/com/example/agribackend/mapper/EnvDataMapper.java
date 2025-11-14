package com.example.agribackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.agribackend.entity.EnvDataEntity;

import java.util.List;

public interface EnvDataMapper extends BaseMapper<EnvDataEntity> {
    List<EnvDataEntity> getLatestDataBySensor();
}