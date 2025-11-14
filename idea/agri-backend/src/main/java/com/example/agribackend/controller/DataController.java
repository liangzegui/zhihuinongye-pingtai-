package com.example.agribackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.agribackend.common.Result;
import com.example.agribackend.entity.EnvDataEntity;
import com.example.agribackend.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data")
public class DataController {
    @Autowired
    private DataService dataService;

    @GetMapping("/historical")
    public Result<Page<EnvDataEntity>> getHistoricalData(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return Result.success(dataService.getHistoricalData(page, size));
    }
}