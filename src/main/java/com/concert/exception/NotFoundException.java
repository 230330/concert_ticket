package com.concert.exception;

/**
 * @description:    未找到异常
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

public class NotFoundException extends BaseException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(404, message);
    }

    public NotFoundException(String resource, Long id) {
        super(404, resource + "不存在（ID: " + id + "）");
    }
}
