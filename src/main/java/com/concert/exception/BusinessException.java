package com.concert.exception;

/**
 * 业务异常（参数校验失败、状态异常、操作不允许等）
 * 对应 HTTP 400 级别的客户端错误
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
