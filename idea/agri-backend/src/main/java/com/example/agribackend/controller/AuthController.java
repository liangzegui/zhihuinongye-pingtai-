package com.example.agribackend.controller;

import com.example.agribackend.service.CaptchaService;
import com.example.agribackend.service.UserService;
import com.example.agribackend.utils.CaptchaUtils;
import com.example.agribackend.utils.JwtUtils;
import com.example.agribackend.utils.PasswordValidator;
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

    @Autowired
    private CaptchaService captchaService;

    // 生成验证码接口（使用 captchaKey 关联验证码，解决 Session 不同步问题）
    @GetMapping("/captcha")
    public void getCaptcha(
            jakarta.servlet.http.HttpServletResponse response,
            @RequestParam(value = "key", required = false) String captchaKey) throws Exception {
        String captchaText = CaptchaUtils.createCaptchaImage(response);
        // 如果提供了 captchaKey，使用内存缓存存储验证码
        if (captchaKey != null && !captchaKey.isEmpty()) {
            captchaService.storeCaptcha(captchaKey, captchaText);
            logger.info("生成验证码：{}，存储到缓存，key：{}", captchaText, captchaKey);
        } else {
            logger.info("生成验证码：{}（无captchaKey，未存储）", captchaText);
        }
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

    // 注册接口（使用 captchaKey 验证验证码）
    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestBody Map<String, String> registerData) {
        logger.info("开始处理注册请求，参数：{}", registerData);
        Map<String, Object> result = new HashMap<>();
        String username = registerData.get("username");
        String password = registerData.get("password");
        String role = registerData.getOrDefault("role", "user");
        String userCaptcha = registerData.get("captcha");
        String captchaKey = registerData.get("captchaKey"); // 获取验证码关联的 key

        // 1. 验证码非空校验
        if (userCaptcha == null || userCaptcha.trim().isEmpty()) {
            logger.warn("注册请求中验证码为空");
            result.put("code", 400);
            result.put("message", "请输入验证码");
            return result;
        }
        // 2. 获取缓存中的验证码（同时从缓存中移除，验证码只能使用一次）
        String cachedCaptcha = captchaService.getAndRemoveCaptcha(captchaKey);
        if (cachedCaptcha == null) {
            logger.warn("缓存中未找到验证码，判定为已过期，captchaKey：{}", captchaKey);
            result.put("code", 400);
            result.put("message", "验证码已过期，请刷新重试");
            return result;
        }
        // 3. 验证码正确性校验
        if (!cachedCaptcha.equalsIgnoreCase(userCaptcha.trim())) {
            logger.warn("验证码错误，用户输入：{}，缓存中存储：{}", userCaptcha, cachedCaptcha);
            result.put("code", 400);
            result.put("message", "验证码错误");
            return result;
        }
        // 4. 密码格式校验
        String pwdError = PasswordValidator.validate(password);
        if (pwdError != null) {
            logger.warn("密码格式不符合要求：{}", pwdError);
            result.put("code", 400);
            result.put("message", pwdError);
            return result;
        }
        // 5. 执行注册逻辑
        boolean registerSuccess = userService.register(username, password, role);
        if (registerSuccess) {
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

    // 修改密码接口
    @PostMapping("/update-pwd")
    public Map<String, Object> updatePassword(
            @RequestBody Map<String, String> pwdData,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> result = new HashMap<>();

        // 从Token中获取用户名
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            result.put("code", 401);
            result.put("message", "未登录或Token无效");
            return result;
        }

        String token = authHeader.substring(7);
        String username;
        try {
            io.jsonwebtoken.Claims claims = JwtUtils.parseJWT(token);
            username = (String) claims.get("username");
        } catch (Exception e) {
            result.put("code", 401);
            result.put("message", "Token已过期，请重新登录");
            return result;
        }

        if (username == null) {
            result.put("code", 401);
            result.put("message", "Token无效");
            return result;
        }

        String oldPwd = pwdData.get("oldPwd");
        String newPwd = pwdData.get("newPwd");

        // 验证新密码格式
        String pwdError = PasswordValidator.validate(newPwd);
        if (pwdError != null) {
            result.put("code", 400);
            result.put("message", pwdError);
            return result;
        }

        boolean success = userService.updatePassword(username, oldPwd, newPwd);
        if (success) {
            logger.info("用户 [{}] 修改密码成功", username);
            result.put("code", 200);
            result.put("message", "密码修改成功");
        } else {
            logger.warn("用户 [{}] 修改密码失败：旧密码错误", username);
            result.put("code", 400);
            result.put("message", "旧密码错误");
        }
        return result;
    }

    // 获取用户信息接口
    @GetMapping("/user/profile")
    public Map<String, Object> getUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> result = new HashMap<>();

        // 从Token中获取用户名
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            result.put("code", 401);
            result.put("message", "未登录或Token无效");
            return result;
        }

        String token = authHeader.substring(7);
        String username;
        try {
            io.jsonwebtoken.Claims claims = JwtUtils.parseJWT(token);
            username = (String) claims.get("username");
        } catch (Exception e) {
            result.put("code", 401);
            result.put("message", "Token已过期，请重新登录");
            return result;
        }

        if (username == null) {
            result.put("code", 401);
            result.put("message", "Token无效");
            return result;
        }

        com.example.agribackend.entity.User user = userService.getUserByUsername(username);
        if (user != null) {
            result.put("code", 200);
            result.put("message", "获取成功");
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", user.getUsername());
            userData.put("role", user.getRole());
            userData.put("createTime", user.getCreateTime());
            result.put("data", userData);
        } else {
            result.put("code", 404);
            result.put("message", "用户不存在");
        }
        return result;
    }
}