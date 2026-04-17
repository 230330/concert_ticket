package com.concert.exception;

/**
 * 无权限异常（操作他人资源等）
 * 对应 HTTP 403
 */
public class ForbiddenException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(403, message);
    }
}
