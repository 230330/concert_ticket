package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 短信实体类
 * @author: hzf
 * @date: 2026年04月24日 10:11
 * @version: 1.0
 */
@Data
@TableName("sms_verification_code")
public class SmsVerificationCode {
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 手机号
     */
    private String phone;
    /**
     * 验证码
     */

    private String code;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     *  0未使用 1已使用
     */
    private Boolean used;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
