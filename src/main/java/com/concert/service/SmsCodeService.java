package com.concert.service;

/**
 * 短信验证码服务接口
 */
public interface SmsCodeService {

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @return 是否发送成功
     */
    boolean sendCode(String phone);

    /**
     * 验证验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否验证通过
     */
    boolean verifyCode(String phone, String code);
}
