package com.concert.exception;

/**
 * @description:    无权限异常类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

public class ForbiddenException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(403, message);
    }
}
