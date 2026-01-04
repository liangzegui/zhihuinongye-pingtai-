package com.example.agribackend.utils;

import java.util.regex.Pattern;

/**
 * 密码验证工具类
 */
public class PasswordValidator {

    // 密码允许的字符：字母、数字、常用特殊字符
    private static final Pattern PASSWORD_PATTERN = Pattern
            .compile("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$");

    // 密码最小长度
    public static final int PASSWORD_MIN_LENGTH = 6;

    // 密码最大长度
    public static final int PASSWORD_MAX_LENGTH = 20;

    /**
     * 验证密码格式
     * 
     * @param password 待验证的密码
     * @return 验证结果，null表示通过，否则返回错误信息
     */
    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }

        if (password.length() < PASSWORD_MIN_LENGTH) {
            return "密码长度不能少于" + PASSWORD_MIN_LENGTH + "位";
        }

        if (password.length() > PASSWORD_MAX_LENGTH) {
            return "密码长度不能超过" + PASSWORD_MAX_LENGTH + "位";
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return "密码只能包含字母、数字和特殊字符（!@#$%^&*等）";
        }

        return null; // 验证通过
    }

    /**
     * 检查密码是否有效
     * 
     * @param password 密码
     * @return true=有效, false=无效
     */
    public static boolean isValid(String password) {
        return validate(password) == null;
    }
}
