package com.example.agribackend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器
 * 统一捕获并处理所有Controller层抛出的异常，返回标准Result格式
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常（可预期的逻辑错误）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        logger.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        logger.warn("参数类型错误: {} - {}", e.getName(), e.getMessage());
        return Result.error(400, "参数类型错误: " + e.getName());
    }

    /**
     * 处理缺少必要请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        logger.warn("缺少必要参数: {}", e.getParameterName());
        return Result.error(400, "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理JWT过期异常
     */
    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
    public Result<Void> handleExpiredJwt(io.jsonwebtoken.ExpiredJwtException e) {
        logger.warn("Token已过期");
        return Result.error(401, "登录已过期，请重新登录");
    }

    /**
     * 处理JWT无效异常
     */
    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public Result<Void> handleJwtException(io.jsonwebtoken.JwtException e) {
        logger.warn("Token无效: {}", e.getMessage());
        return Result.error(401, "Token无效，请重新登录");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("非法参数: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 兜底：处理所有未捕获的系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        logger.error("系统异常", e);
        return Result.error(500, "服务器内部错误，请稍后重试");
    }
}
