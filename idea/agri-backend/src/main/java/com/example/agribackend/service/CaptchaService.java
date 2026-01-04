package com.example.agribackend.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 验证码缓存服务
 * 使用 ConcurrentHashMap 存储验证码，通过 captchaKey 关联
 * 验证码有效期为 5 分钟
 */
@Service
public class CaptchaService {

    // 验证码缓存：key -> CaptchaEntry (包含验证码文本和过期时间)
    private final Map<String, CaptchaEntry> captchaCache = new ConcurrentHashMap<>();

    // 验证码有效期（毫秒）：5分钟
    private static final long CAPTCHA_EXPIRE_MS = 5 * 60 * 1000;

    // 定时清理过期验证码
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public CaptchaService() {
        // 每分钟清理一次过期的验证码
        scheduler.scheduleAtFixedRate(this::cleanExpiredCaptcha, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 存储验证码
     * 
     * @param key         验证码Key（前端生成的UUID）
     * @param captchaText 验证码文本
     */
    public void storeCaptcha(String key, String captchaText) {
        long expireTime = System.currentTimeMillis() + CAPTCHA_EXPIRE_MS;
        captchaCache.put(key, new CaptchaEntry(captchaText, expireTime));
    }

    /**
     * 获取并移除验证码（验证码只能使用一次）
     * 
     * @param key 验证码Key
     * @return 验证码文本，如果不存在或已过期返回null
     */
    public String getAndRemoveCaptcha(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        CaptchaEntry entry = captchaCache.remove(key);
        if (entry == null) {
            return null;
        }
        // 检查是否过期
        if (System.currentTimeMillis() > entry.expireTime) {
            return null;
        }
        return entry.captchaText;
    }

    /**
     * 清理过期的验证码
     */
    private void cleanExpiredCaptcha() {
        long now = System.currentTimeMillis();
        captchaCache.entrySet().removeIf(entry -> now > entry.getValue().expireTime);
    }

    /**
     * 验证码条目
     */
    private static class CaptchaEntry {
        final String captchaText;
        final long expireTime;

        CaptchaEntry(String captchaText, long expireTime) {
            this.captchaText = captchaText;
            this.expireTime = expireTime;
        }
    }
}
