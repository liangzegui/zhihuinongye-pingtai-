package com.example.agribackend.service;

import com.example.agribackend.entity.User;
import com.example.agribackend.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;

    // BCrypt 密码编码器
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 登录验证：使用 BCrypt 校验密码哈希
     */
    public boolean login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) return false;

        // 兼容旧的明文密码：如果密码不是BCrypt格式，用明文比较后自动升级
        String storedPassword = user.getPassword();
        if (!storedPassword.startsWith("$2a$") && !storedPassword.startsWith("$2b$")) {
            if (storedPassword.equals(password)) {
                // 自动升级为 BCrypt 哈希
                String hashedPassword = passwordEncoder.encode(password);
                userMapper.updatePassword(username, hashedPassword);
                logger.info("用户 [{}] 密码已自动升级为BCrypt加密", username);
                return true;
            }
            return false;
        }

        return passwordEncoder.matches(password, storedPassword);
    }

    /**
     * 注册：密码经 BCrypt 加密后存入数据库
     */
    public boolean register(String username, String password, String role) {
        User existingUser = userMapper.findByUsername(username);
        if (existingUser != null) return false;

        String hashedPassword = passwordEncoder.encode(password);
        User newUser = new User(username, hashedPassword, role, LocalDateTime.now());
        return userMapper.insert(newUser) > 0;
    }

    /**
     * 修改密码：用 BCrypt 验证旧密码，加密新密码
     */
    public boolean updatePassword(String username, String oldPassword, String newPassword) {
        User user = userMapper.findByUsername(username);
        if (user == null) return false;

        String storedPassword = user.getPassword();
        boolean oldPwdMatch;

        if (!storedPassword.startsWith("$2a$") && !storedPassword.startsWith("$2b$")) {
            oldPwdMatch = storedPassword.equals(oldPassword);
        } else {
            oldPwdMatch = passwordEncoder.matches(oldPassword, storedPassword);
        }

        if (!oldPwdMatch) return false;

        String hashedNewPassword = passwordEncoder.encode(newPassword);
        return userMapper.updatePassword(username, hashedNewPassword) > 0;
    }

    /**
     * 获取用户信息
     */
    public User getUserByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(Integer id) {
        return userMapper.findById(id);
    }

    /**
     * 查询全部用户
     */
    public List<User> getAllUsers() {
        return userMapper.findAll();
    }

    /**
     * 管理员创建用户
     */
    public boolean createUserByAdmin(String username, String password, String role) {
        User existingUser = userMapper.findByUsername(username);
        if (existingUser != null) {
            return false;
        }
        String hashedPassword = passwordEncoder.encode(password);
        User newUser = new User(username, hashedPassword, role, LocalDateTime.now());
        return userMapper.insert(newUser) > 0;
    }

    /**
     * 管理员更新用户（可更新用户名、角色、密码）
     */
    public boolean updateUserByAdmin(Integer id, String username, String role, String newPassword) {
        User existing = userMapper.findById(id);
        if (existing == null) {
            return false;
        }

        // 用户名唯一性校验（排除自己）
        if (username != null && !username.equals(existing.getUsername())) {
            User sameNameUser = userMapper.findByUsername(username);
            if (sameNameUser != null) {
                return false;
            }
        }

        String finalUsername = (username == null || username.trim().isEmpty()) ? existing.getUsername() : username.trim();
        String finalRole = (role == null || role.trim().isEmpty()) ? existing.getRole() : role.trim();

        int updated = userMapper.updateBasicById(id, finalUsername, finalRole);
        if (updated <= 0) {
            return false;
        }

        // 如有新密码则更新
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            String hashedNewPassword = passwordEncoder.encode(newPassword.trim());
            return userMapper.updatePasswordById(id, hashedNewPassword) > 0;
        }

        return true;
    }

    /**
     * 管理员删除用户
     */
    public boolean deleteUserById(Integer id) {
        return userMapper.deleteById(id) > 0;
    }

    /**
     * 是否管理员
     */
    public boolean isAdmin(String username) {
        User user = userMapper.findByUsername(username);
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }

    /**
     * 统计管理员数量
     */
    public int countAdminUsers() {
        return userMapper.countByRole("admin");
    }
}