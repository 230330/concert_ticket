package com.concert.exception;

/**
 * 资源未找到异常（查询的实体不存在）
 * 对应 HTTP 404
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
