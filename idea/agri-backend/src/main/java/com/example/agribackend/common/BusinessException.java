package com.example.agribackend.common;

import lombok.Getter;

/**
 * 自定义业务异常
 * 用于在业务逻辑中抛出可预期的错误，由全局异常处理器统一捕获
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
