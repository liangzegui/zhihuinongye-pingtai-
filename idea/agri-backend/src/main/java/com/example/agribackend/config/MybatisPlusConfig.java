package com.example.agribackend.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置类
 * 配置分页插件等
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis Plus 拦截器
     * 添加分页插件以支持分页查询和正确计算 total
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件，指定数据库类型为 MySQL
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置最大单页限制数量，需要覆盖图表一次性拉取的上限
        paginationInterceptor.setMaxLimit(5000L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
