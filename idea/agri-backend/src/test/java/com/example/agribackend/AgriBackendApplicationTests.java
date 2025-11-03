package com.example.agribackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class AgriBackendApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate; // 注入JdbcTemplate

	@Test
	void testDatabaseConnection() {
		// 执行简单查询（如查询MySQL版本）
		String dbVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
		System.out.println("数据库连接成功！版本为：" + dbVersion);
	}
}
