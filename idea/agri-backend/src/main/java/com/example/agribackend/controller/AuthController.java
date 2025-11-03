package com.example.agribackend.controller;

import com.example.agribackend.service.UserService;
import com.example.agribackend.utils.CaptchaUtils;
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

    @Autowired
    private UserService userService;

    // 生成验证码图片接口
    @GetMapping("/captcha")
    public void getCaptcha(jakarta.servlet.http.HttpServletResponse response, HttpSession session) throws Exception {
        String captchaText = CaptchaUtils.createCaptchaImage(response);
        session.setAttribute("sessionCaptcha", captchaText);
        logger.info("生成验证码：{}，并存储到Session", captchaText);
    }

    // 登录接口
    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestBody Map<String, String> loginData,
            HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String username = loginData.get("username");
        String password = loginData.get("password");

        if (userService.login(username, password)) {
            result.put("code", 200);
            result.put("message", "登录成功");
        } else {
            result.put("code", 400);
            result.put("message", "用户名或密码错误");
        }
        return result;
    }

    // 注册接口（返回详细错误原因）
    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestBody Map<String, String> registerData,
            HttpSession session) {
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