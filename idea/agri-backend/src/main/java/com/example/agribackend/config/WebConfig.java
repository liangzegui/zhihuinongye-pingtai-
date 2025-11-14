package com.example.agribackend.config;

import com.example.agribackend.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Spring MVC配置：注册拦截器，指定拦截/排除规则
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**") // 拦截所有接口
                .excludePathPatterns( // 排除不需要登录的接口（白名单）
                        "/auth/login",    // 登录接口
                        "/auth/register", // 注册接口
                        "/auth/captcha"   // 验证码接口
                );
    }
}