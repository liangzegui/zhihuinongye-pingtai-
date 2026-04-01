package com.example.agribackend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {
    // JWT密钥：支持通过环境变量 JWT_SECRET 覆盖，部署时务必设置自定义密钥
    private static final String SECRET_KEY_STR = System.getenv().getOrDefault(
            "JWT_SECRET", "agri-backend-2025-secret-key-20251109-secure");
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STR.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000; // Token有效期7天

    /**
     * 生成JWT Token
     */
    public static String createJWT(Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * 解析JWT Token
     */
    public static Claims parseJWT(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}