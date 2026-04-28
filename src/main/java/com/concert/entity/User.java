package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.concert.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:    系统用户角色表实体类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Data
@TableName("user")
public class User {

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;


    /**
     * 昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像URL
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 状态
     */
    @TableField("status")
    private UserStatus status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 角色  这里不使用该字段来判断角色权限
     */
    @TableField("role")
    private String role;
}
