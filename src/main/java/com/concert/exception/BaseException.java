package com.concert.exception;

/**
 * @description:    基础异常类，所有自定义业务异常的父类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

public class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final int code;

    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
