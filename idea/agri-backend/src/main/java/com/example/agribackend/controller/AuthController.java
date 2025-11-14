package com.example.agribackend.controller;

import com.example.agribackend.service.UserService;
import com.example.agribackend.utils.CaptchaUtils;
import com.example.agribackend.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    // 🔴 同步修改：与JwtUtils的SECRET_KEY_STR完全一致
    private static final String JWT_SECRET = "agri-backend-2025-secret-key-20251109-secure";
    private static final long JWT_EXPIRY = 7 * 24 * 60 * 60 * 1000;

    @Autowired
    private UserService userService;

    // 生成验证码接口（原有逻辑不变）
    @GetMapping("/captcha")
    public void getCaptcha(jakarta.servlet.http.HttpServletResponse response, HttpSession session) throws Exception {
        String captchaText = CaptchaUtils.createCaptchaImage(response);
        session.setAttribute("sessionCaptcha", captchaText);
        logger.info("生成验证码：{}，存储到Session", captchaText);
    }

    // 登录接口（修改Token生成逻辑，调用JwtUtils.createJWT）
    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestBody Map<String, String> loginData,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String username = loginData.get("username");
        String password = loginData.get("password");

        if (userService.login(username, password)) {
            // 生成JWT Token（存入用户名）
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", username);
            String token = JwtUtils.createJWT(claims);

            result.put("code", 200);
            result.put("message", "登录成功");
            result.put("token", token);
            result.put("username", username);
            logger.info("用户 [{}] 登录成功，生成Token：{}", username, token);
        } else {
            result.put("code", 400);
            result.put("message", "用户名或密码错误");
            logger.warn("用户 [{}] 登录失败：用户名或密码错误", username);
        }
        return result;
    }

    // 注册接口（原有逻辑不变，无需修改）
    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestBody Map<String, String> registerData,
            HttpSession session) {
        // 你的原有注册代码...（无需修改）
        logger.info("开始处理注册请求，参数：{}", registerData);
        Map<String, Object> result = new HashMap<>();
        String username = registerData.get("username");
        String password = registerData.get("password");
        String role = registerData.getOrDefault("role", "user");
        String userCaptcha = registerData.get("captcha");

        // 1. 验证码非空校验
        if (userCaptcha == null || userCaptcha.trim().isEmpty()) {
            logger.warn("注册请求中验证码为空");
            result.put("code", 400);
            result.put("message", "请输入验证码");
            return result;
        }
        // 2. 验证码过期校验
        String sessionCaptcha = (String) session.getAttribute("sessionCaptcha");
        if (sessionCaptcha == null) {
            logger.warn("Session中未找到验证码，判定为已过期");
            result.put("code", 400);
            result.put("message", "验证码已过期，请刷新重试");
            return result;
        }
        // 3. 验证码正确性校验
        if (!sessionCaptcha.equalsIgnoreCase(userCaptcha.trim())) {
            logger.warn("验证码错误，用户输入：{}，Session中存储：{}", userCaptcha, sessionCaptcha);
            result.put("code", 400);
            result.put("message", "验证码错误");
            return result;
        }
        // 4. 密码长度校验（新增：确保密码至少6位）
        if (password == null || password.length() < 6) {
            logger.warn("密码长度不足，输入长度：{}", password != null ? password.length() : 0);
            result.put("code", 400);
            result.put("message", "密码长度不能少于6位");
            return result;
        }
        // 5. 执行注册逻辑
        boolean registerSuccess = userService.register(username, password, role);
        if (registerSuccess) {
            session.removeAttribute("sessionCaptcha");
            logger.info("用户 [{}] 以角色 [{}] 注册成功", username, role);
            result.put("code", 200);
            result.put("message", "注册成功");
        } else {
            logger.warn("用户名 [{}] 已存在", username);
            result.put("code", 400);
            result.put("message", "用户名已存在");
        }
        return result;
    }
}