package com.example.agribackend.service;

import com.example.agribackend.entity.User;
import com.example.agribackend.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 * 使用 Mockito 模拟数据库层，专注测试业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private User testUser;

    @BeforeEach
    void setUp() {
        // 准备测试用户（BCrypt加密密码）
        testUser = new User("testUser", encoder.encode("Test@1234"), "user", LocalDateTime.now());
        testUser.setId(1);
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("正确密码登录成功")
    void loginWithCorrectPassword() {
        when(userMapper.findByUsername("testUser")).thenReturn(testUser);
        assertTrue(userService.login("testUser", "Test@1234"));
    }

    @Test
    @DisplayName("错误密码登录失败")
    void loginWithWrongPassword() {
        when(userMapper.findByUsername("testUser")).thenReturn(testUser);
        assertFalse(userService.login("testUser", "WrongPassword"));
    }

    @Test
    @DisplayName("不存在的用户登录失败")
    void loginWithNonExistentUser() {
        when(userMapper.findByUsername("nobody")).thenReturn(null);
        assertFalse(userService.login("nobody", "anyPass"));
    }

    @Test
    @DisplayName("明文密码自动升级为BCrypt")
    void loginWithPlaintextPasswordAutoUpgrade() {
        // 模拟旧版明文密码用户
        User legacyUser = new User("legacy", "plaintext123", "user", LocalDateTime.now());
        when(userMapper.findByUsername("legacy")).thenReturn(legacyUser);
        when(userMapper.updatePassword(eq("legacy"), anyString())).thenReturn(1);

        assertTrue(userService.login("legacy", "plaintext123"));
        // 验证密码被升级（调用了updatePassword）
        verify(userMapper).updatePassword(eq("legacy"), argThat(hash ->
                hash.startsWith("$2a$") || hash.startsWith("$2b$")));
    }

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册新用户成功")
    void registerNewUser() {
        when(userMapper.findByUsername("newUser")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        assertTrue(userService.register("newUser", "New@1234", "user"));
        // 验证存入数据库的密码是BCrypt加密的
        verify(userMapper).insert(argThat(user ->
                user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$")));
    }

    @Test
    @DisplayName("重复用户名注册失败")
    void registerDuplicateUsername() {
        when(userMapper.findByUsername("testUser")).thenReturn(testUser);

        assertFalse(userService.register("testUser", "Test@1234", "user"));
        verify(userMapper, never()).insert(any());
    }

    // ==================== 修改密码测试 ====================

    @Test
    @DisplayName("修改密码 - 旧密码正确")
    void updatePasswordSuccess() {
        when(userMapper.findByUsername("testUser")).thenReturn(testUser);
        when(userMapper.updatePassword(eq("testUser"), anyString())).thenReturn(1);

        assertTrue(userService.updatePassword("testUser", "Test@1234", "NewPass@5678"));
    }

    @Test
    @DisplayName("修改密码 - 旧密码错误")
    void updatePasswordWrongOldPassword() {
        when(userMapper.findByUsername("testUser")).thenReturn(testUser);

        assertFalse(userService.updatePassword("testUser", "WrongOld", "NewPass@5678"));
        verify(userMapper, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("修改密码 - 用户不存在")
    void updatePasswordUserNotFound() {
        when(userMapper.findByUsername("nobody")).thenReturn(null);

        assertFalse(userService.updatePassword("nobody", "old", "new"));
    }

    // ==================== 角色判断测试 ====================

    @Test
    @DisplayName("判断管理员角色")
    void isAdmin() {
        User admin = new User("admin", "xxx", "admin", LocalDateTime.now());
        when(userMapper.findByUsername("admin")).thenReturn(admin);

        assertTrue(userService.isAdmin("admin"));
    }

    @Test
    @DisplayName("普通用户非管理员")
    void isNotAdmin() {
        when(userMapper.findByUsername("testUser")).thenReturn(testUser);

        assertFalse(userService.isAdmin("testUser"));
    }

    @Test
    @DisplayName("不存在的用户非管理员")
    void isAdminUserNotFound() {
        when(userMapper.findByUsername("nobody")).thenReturn(null);

        assertFalse(userService.isAdmin("nobody"));
    }

    // ==================== 管理员操作测试 ====================

    @Test
    @DisplayName("管理员创建用户成功")
    void createUserByAdminSuccess() {
        when(userMapper.findByUsername("newUser")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        assertTrue(userService.createUserByAdmin("newUser", "Pass@1234", "user"));
    }

    @Test
    @DisplayName("管理员创建已存在用户失败")
    void createUserByAdminDuplicate() {
        when(userMapper.findByUsername("testUser")).thenReturn(testUser);

        assertFalse(userService.createUserByAdmin("testUser", "Pass@1234", "user"));
    }

    @Test
    @DisplayName("管理员删除用户成功")
    void deleteUserByIdSuccess() {
        when(userMapper.deleteById(1)).thenReturn(1);

        assertTrue(userService.deleteUserById(1));
    }

    @Test
    @DisplayName("统计管理员数量")
    void countAdminUsers() {
        when(userMapper.countByRole("admin")).thenReturn(2);

        assertEquals(2, userService.countAdminUsers());
    }
}
