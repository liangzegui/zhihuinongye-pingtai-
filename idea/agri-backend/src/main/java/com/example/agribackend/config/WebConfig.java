package com.example.agribackend.config;

import com.example.agribackend.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Spring MVC配置：注册拦截器，指定拦截/排除规则
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @NonNull
    private final JwtInterceptor jwtInterceptor;

    public WebConfig(@NonNull JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**") // 拦截所有接口
                .excludePathPatterns( // 排除不需要登录的接口（白名单）
                        "/auth/**", // 认证接口（登录、注册、验证码）
                        "/api/auth/**", // 认证接口（带/api前缀）
                        "/ws/**", // WebSocket接口
                        "/error", // 错误页面
                        "/swagger-ui/**", // Swagger UI 资源
                        "/swagger-ui.html", // Swagger 入口页
                        "/v3/api-docs/**", // OpenAPI 文档
                        "/swagger-resources/**" // Swagger 资源
                );
    }
}