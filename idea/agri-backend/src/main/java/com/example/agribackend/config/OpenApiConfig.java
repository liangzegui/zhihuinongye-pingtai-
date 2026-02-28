package com.example.agribackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger 接口文档配置
 * 访问地址：http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智慧农业监控平台 API 文档")
                        .version("1.0.0")
                        .description("基于 Spring Boot 3 + Vue 3 + ESP32 的 IoT 智慧农业监控系统后端接口文档\n\n"
                                + "## 功能模块\n"
                                + "- **认证模块**: 登录、注册、验证码、修改密码\n"
                                + "- **实时数据**: ESP32 传感器数据采集与推送\n"
                                + "- **历史数据**: 环境数据分页查询与导出\n"
                                + "- **数据分析**: 温湿度/土壤/光照/CO₂ 趋势分析\n"
                                + "- **预警系统**: 阈值规则管理与预警日志\n"
                                + "- **设备控制**: 水泵/风扇/照明远程控制\n"
                                + "- **管理后台**: 用户管理与系统配置")
                        .contact(new Contact()
                                .name("智慧农业团队")
                                .email("admin@agri-platform.com")))
                // 添加全局 JWT 安全认证
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("输入登录接口返回的JWT Token（无需添加Bearer前缀）")));
    }
}
