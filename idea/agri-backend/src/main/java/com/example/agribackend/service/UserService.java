package com.example.agribackend.service;

import com.example.agribackend.entity.User;
import com.example.agribackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    // 登录验证：用户名+密码匹配则返回true
    public boolean login(String username, String password) {
        User user = userMapper.findByUsername(username);
        return user != null && user.getPassword().equals(password);
    }

    // 注册：用户名未存在则插入新用户，返回true
    public boolean register(String username, String password, String role) {
        User existingUser = userMapper.findByUsername(username);
        if (existingUser != null) return false; // 用户名已存在

        User newUser = new User(username, password, role, LocalDateTime.now());
        return userMapper.insert(newUser) > 0;
    }
}