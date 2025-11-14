package com.example.agribackend.service;

import com.example.agribackend.dto.EnvDataDTO;

import java.util.List;

public interface RealTimeService {
    List<EnvDataDTO> getLatestData();
}