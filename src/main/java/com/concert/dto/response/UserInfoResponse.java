package com.concert.dto.response;

import com.concert.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:    用户信息响应
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Data
public class UserInfoResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 状态
     */
    private UserStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
