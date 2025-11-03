package com.example.agribackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class DatabaseTestController {

    @Autowired
    private JdbcTemplate jdbcTemplate; // 注入JdbcTemplate，用于执行数据库操作

    @GetMapping("/db")
    public String testDatabaseConnection() {
        try {
            // 执行简单的数据库查询（如查询MySQL版本）
            String dbVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            return "数据库连接成功！版本：" + dbVersion;
        } catch (Exception e) {
            return "数据库连接失败：" + e.getMessage();
        }
    }
}