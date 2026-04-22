package com.concert.exception;

/**
 * @description: 未授权异常
 * @author: hzf
 * @date: 2026年04月22日 9:50
 * @version: 1.0
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(401, message);
    }
}
