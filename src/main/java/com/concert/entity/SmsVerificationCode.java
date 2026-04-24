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
    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String code;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     *  0未使用 1已使用
     */
    private Boolean used;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
