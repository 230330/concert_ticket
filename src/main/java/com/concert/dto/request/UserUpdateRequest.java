package com.concert.dto.request;

import lombok.Data;

/**
 * 用户信息更新请求
 */
@Data
public class UserUpdateRequest {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;
}
