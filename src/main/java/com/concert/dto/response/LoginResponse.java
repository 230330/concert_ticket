package com.concert.dto.response;

import lombok.Data;

/**
 * @description:    登录响应
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Data
public class LoginResponse {

    /**
     * JWT Token
     */
    private String token;

    /**
     * Token 类型
     */
    private String tokenType = "Bearer";

    /**
     * 过期时间（毫秒）
     */
    private Long expiresIn;

    public LoginResponse() {
    }

    public LoginResponse(String token, Long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }
}
