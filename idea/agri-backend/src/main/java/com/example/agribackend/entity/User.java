package com.example.agribackend.entity;

import java.time.LocalDateTime;

public class User {
    private Integer id;
    private String username;
    private String password;
    private String role;
    private LocalDateTime createTime;

    // 构造方法、Getter/Setter
    public User() {}
    public User(String username, String password, String role, LocalDateTime createTime) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.createTime = createTime;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}