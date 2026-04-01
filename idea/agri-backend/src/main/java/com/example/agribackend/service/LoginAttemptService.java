package com.example.agribackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务端登录尝试管理（内存级，单实例有效）
 * 记录每个用户的登录失败次数，超过阈值后锁定一段时间。
 */
@Service
public class LoginAttemptService {
    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 10 * 60 * 1000L; // 10分钟

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    /** 判断该用户是否处于锁定状态 */
    public boolean isLocked(String username) {
        if (username == null)
            return false;
        AttemptInfo info = attempts.get(username);
        if (info == null)
            return false;
        if (info.lockUntil != null && System.currentTimeMillis() < info.lockUntil) {
            return true;
        }
        // 锁定已过期，自动清除
        if (info.lockUntil != null) {
            attempts.remove(username);
        }
        return false;
    }

    /** 记录一次登录失败 */
    public void recordFailure(String username) {
        if (username == null)
            return;
        AttemptInfo info = attempts.computeIfAbsent(username, k -> new AttemptInfo());
        int currentCount = info.count.incrementAndGet();
        if (currentCount >= MAX_ATTEMPTS) {
            info.lockUntil = System.currentTimeMillis() + LOCK_DURATION_MS;
            logger.warn("用户 [{}] 登录失败达 {} 次，账户锁定 10 分钟", username, currentCount);
        }
    }

    /** 登录成功后重置计数 */
    public void resetAttempts(String username) {
        if (username == null)
            return;
        attempts.remove(username);
    }

    /** 获取剩余允许尝试次数 */
    public int getRemainingAttempts(String username) {
        if (username == null)
            return MAX_ATTEMPTS;
        AttemptInfo info = attempts.get(username);
        if (info == null)
            return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - info.count.get());
    }

    private static class AttemptInfo {
        final AtomicInteger count = new AtomicInteger(0);
        volatile Long lockUntil = null;
    }
}
