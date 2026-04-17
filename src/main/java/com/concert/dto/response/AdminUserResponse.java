package com.concert.dto.response;

import com.concert.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description:    管理端-用户信息响应
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Data
public class AdminUserResponse {

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
     * 角色列表
     */
    private List<String> roles;

    /**
     * 订单数
     */
    private Long orderCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
