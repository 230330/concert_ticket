package com.concert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concert.entity.SmsVerificationCode;

/**
 * @description:    短信验证码服务接口
 * @author: hzf
 * @date: 2026年04月24日 10:37
 * @version: 1.0
 */
public interface SmsVerificationCodeService  extends IService<SmsVerificationCode> {
    /**
     * 发送验证码
     * @param phone 手机号
     * @return 验证码（用于调试，实际生产可不返回）
     */
    String sendCode(String phone);

    /**
     * 验证验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String phone, String code);
}
