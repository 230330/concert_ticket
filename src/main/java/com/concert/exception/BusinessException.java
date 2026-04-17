package com.concert.exception;

/**
 * @description:    业务异常类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

public class BusinessException extends BaseException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(400, message);
    }

    public BusinessException(int code, String message) {
        super(code, message);
    }

    public BusinessException(String message, Throwable cause) {
        super(400, message, cause);
    }
}
