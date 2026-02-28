package com.example.agribackend.controller;

import com.example.agribackend.common.Result;
import com.example.agribackend.service.CaptchaService;
import com.example.agribackend.service.LoginAttemptService;
import com.example.agribackend.service.UserService;
import com.example.agribackend.utils.CaptchaUtils;
import com.example.agribackend.utils.JwtUtils;
import com.example.agribackend.utils.PasswordValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    // ==================== 验证码 ====================

    @GetMapping("/captcha")
    public void getCaptcha(
            HttpServletResponse response,
            @RequestParam(value = "key", required = false) String captchaKey) throws Exception {
        String captchaText = CaptchaUtils.createCaptchaImage(response);
        if (captchaKey != null && !captchaKey.isEmpty()) {
            captchaService.storeCaptcha(captchaKey, captchaText);
            logger.info("生成验证码：{}，存储到缓存，key：{}", captchaText, captchaKey);
        }
    }

    // ==================== 登录 ====================

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        // 1) 服务端登录锁定检查
        if (loginAttemptService.isLocked(username)) {
            logger.warn("用户 [{}] 账户处于锁定状态，拒绝登录", username);
            return Result.error(423, "登录失败次数过多，账户已锁定，请10分钟后重试");
        }

        // 2) 校验用户名密码
        if (userService.login(username, password)) {
            loginAttemptService.resetAttempts(username);

            com.example.agribackend.entity.User user = userService.getUserByUsername(username);
            String role = user != null ? user.getRole() : "user";

            Map<String, Object> claims = new HashMap<>();
            claims.put("username", username);
            claims.put("role", role);
            String token = JwtUtils.createJWT(claims);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", username);
            data.put("role", role);

            logger.info("用户 [{}] 登录成功", username);
            Result<Map<String, Object>> result = Result.success(data);
            result.setMsg("登录成功");
            return result;
        }

        // 3) 密码错误，记录失败次数
        loginAttemptService.recordFailure(username);
        int remaining = loginAttemptService.getRemainingAttempts(username);
        String msg = remaining > 0
                ? "用户名或密码错误，还可尝试 " + remaining + " 次"
                : "登录失败次数过多，账户已锁定，请10分钟后重试";
        logger.warn("用户 [{}] 登录失败，剩余尝试次数：{}", username, remaining);
        return Result.error(400, msg);
    }

    // ==================== 注册 ====================

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> registerData) {
        String username = registerData.get("username");
        String password = registerData.get("password");
        String role = registerData.getOrDefault("role", "user");
        String userCaptcha = registerData.get("captcha");
        String captchaKey = registerData.get("captchaKey");

        // 1) 验证码非空
        if (userCaptcha == null || userCaptcha.trim().isEmpty()) {
            return Result.error(400, "请输入验证码");
        }
        // 2) 验证码存在性
        String cachedCaptcha = captchaService.getAndRemoveCaptcha(captchaKey);
        if (cachedCaptcha == null) {
            return Result.error(400, "验证码已过期，请刷新重试");
        }
        // 3) 验证码正确性
        if (!cachedCaptcha.equalsIgnoreCase(userCaptcha.trim())) {
            return Result.error(400, "验证码错误");
        }
        // 4) 密码格式校验
        String pwdError = PasswordValidator.validate(password);
        if (pwdError != null) {
            return Result.error(400, pwdError);
        }
        // 5) 注册
        if (userService.register(username, password, role)) {
            logger.info("用户 [{}] 注册成功", username);
            return Result.ok("注册成功");
        }
        return Result.error(400, "用户名已存在");
    }

    // ==================== 修改密码 ====================

    @PostMapping("/update-pwd")
    public Result<Void> updatePassword(
            @RequestBody Map<String, String> pwdData,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Token解析
        String username = resolveUsername(authHeader);
        if (username == null) {
            return Result.error(401, "未登录或Token无效");
        }

        String oldPwd = pwdData.get("oldPwd");
        String newPwd = pwdData.get("newPwd");

        // 新密码格式校验
        String pwdError = PasswordValidator.validate(newPwd);
        if (pwdError != null) {
            return Result.error(400, pwdError);
        }

        if (userService.updatePassword(username, oldPwd, newPwd)) {
            logger.info("用户 [{}] 修改密码成功", username);
            return Result.ok("密码修改成功");
        }
        return Result.error(400, "旧密码错误");
    }

    // ==================== 用户信息 ====================

    @GetMapping("/user/profile")
    public Result<Map<String, Object>> getUserProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String username = resolveUsername(authHeader);
        if (username == null) {
            return Result.error(401, "未登录或Token无效");
        }

        com.example.agribackend.entity.User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", user.getUsername());
        userData.put("role", user.getRole());
        userData.put("createTime", user.getCreateTime());

        return Result.success(userData);
    }

    // ==================== 工具方法 ====================

    /** 从Authorization Header中解析用户名，失败返回null */
    private String resolveUsername(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            io.jsonwebtoken.Claims claims = JwtUtils.parseJWT(authHeader.substring(7));
            return (String) claims.get("username");
        } catch (Exception e) {
            return null;
        }
    }
}