package com.example.agribackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 * 用于实时推送ESP32传感器数据到前端
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // 启用简单的内存消息代理
        // /topic 用于广播消息（所有订阅者都会收到）
        // /queue 用于点对点消息
        config.enableSimpleBroker("/topic", "/queue");

        // 应用程序目标前缀（客户端发送消息的前缀）
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // 注册WebSocket端点，前端通过此地址连接
        // withSockJS() 提供向下兼容（不支持WebSocket的浏览器使用轮询）
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 允许跨域
                .withSockJS();

        // 也提供原生WebSocket端点（不使用SockJS）
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }
}
