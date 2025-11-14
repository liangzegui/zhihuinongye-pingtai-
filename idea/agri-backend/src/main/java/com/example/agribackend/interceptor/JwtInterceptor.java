package com.example.agribackend.interceptor;

import com.example.agribackend.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(JwtInterceptor.class);
    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tokenHeader = request.getHeader(TOKEN_HEADER);
        if (tokenHeader == null || !tokenHeader.startsWith(TOKEN_PREFIX)) {
            logger.warn("请求头缺少有效Token：{}", tokenHeader);
            returnJson(response, "{\"code\":401,\"message\":\"未登录，请先登录\"}");
            return false;
        }

        String token = tokenHeader.substring(TOKEN_PREFIX.length()).trim();
        try {
            // 🔴 使用JwtUtils.parseJWT解析（自动使用安全密钥验证）
            Claims claims = JwtUtils.parseJWT(token);
            String username = claims.get("username", String.class);
            logger.info("Token验证通过，用户名：{}", username);
            request.setAttribute("loginUsername", username);
            return true;
        } catch (JwtException e) {
            logger.error("Token无效：{}，错误：{}", token, e.getMessage());
            returnJson(response, "{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            return false;
        }
    }

    private void returnJson(HttpServletResponse response, String json) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.print(json);
        writer.flush();
        writer.close();
    }
}