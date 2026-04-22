package com.concert.exception;

import com.concert.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @description:    全局异常处理器
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

//    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理自定义业务异常（BusinessException 及其子类）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(NotFoundException.class)
    public Result<Void> handleNotFoundException(NotFoundException e) {
        log.warn("资源未找到：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理无权限异常
     */
    @ExceptionHandler(ForbiddenException.class)
    public Result<Void> handleForbiddenException(ForbiddenException e) {
        log.warn("无权限操作：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理请求参数校验异常（@Validated / @Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败：{}", message);
        return Result.error(400, message);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数绑定失败：{}", message);
        return Result.error(400, message);
    }

    /**
     * 处理数据库唯一键冲突
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("数据唯一键冲突");
        // 如需记录详细堆栈，可使用 debug 级别
        log.debug("详细异常：", e);
        return Result.error(409, "数据已存在，请勿重复操作");
    }


    /**
     * 处理未授权异常
     */
    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorized(UnauthorizedException e) {
        // 返回 401 和错误信息
        log.warn("未授权访问：{}", e.getMessage());
        return Result.unauthorized(e.getMessage());
    }


    /**
     * 兜底处理所有未捕获的异常
     * 绝不向前端暴露内部堆栈信息
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        String errorId = UUID.randomUUID().toString();
        log.error("系统异常 errorId={}", errorId, e);
        return Result.error(500, "系统繁忙，请稍后重试 [错误ID：" + errorId + "]");
    }
}
