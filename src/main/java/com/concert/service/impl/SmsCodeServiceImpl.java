package com.concert.service.impl;

import com.concert.service.SmsCodeService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @description:    短信验证码服务实现
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class SmsCodeServiceImpl implements SmsCodeService {

    private static final Logger logger = LoggerFactory.getLogger(SmsCodeServiceImpl.class);

    /**
     * 验证码 Redis Key 前缀
     */
    private static final String SMS_CODE_PREFIX = "sms:code:";

    /**
     * 验证码有效期（分钟）
     */
    @Value("${concert.captcha.expire-minutes:5}")
    private long captchaExpireMinutes;

    /**
     * 验证码长度
     */
    private static final int CODE_LENGTH = 6;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean sendCode(String phone) {
        if (StringUtils.isBlank(phone)) {
            logger.warn("手机号为空，无法发送验证码");
            return false;
        }

        // 生成6位随机验证码
        String code = generateCode();

        // 存入 Redis，设置5分钟有效期
        String redisKey = SMS_CODE_PREFIX + phone;
        redisTemplate.opsForValue().set(redisKey, code, captchaExpireMinutes, TimeUnit.MINUTES);

        // TODO: 对接阿里云短信 SDK 发送短信
        // 目前先用日志输出验证码
        logger.info("【演唱会订票系统】验证码发送成功，手机号：{}，验证码：{}，有效期：{}分钟", phone, code, captchaExpireMinutes);

        return true;
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        if (StringUtils.isBlank(phone) || StringUtils.isBlank(code)) {
            logger.warn("手机号或验证码为空，验证失败");
            return false;
        }

        String redisKey = SMS_CODE_PREFIX + phone;
        Object cachedCode = redisTemplate.opsForValue().get(redisKey);

        if (cachedCode == null) {
            logger.warn("验证码已过期或不存在，手机号：{}", phone);
            return false;
        }

        // 验证码比对
        if (code.equals(cachedCode.toString())) {
            // 验证成功后删除验证码，防止重复使用
            redisTemplate.delete(redisKey);
            logger.info("验证码验证成功，手机号：{}", phone);
            return true;
        }

        logger.warn("验证码错误，手机号：{}，输入：{}，正确：{}", phone, code, cachedCode);
        return false;
    }

    /**
     * 生成6位随机数字验证码
     *
     * @return 验证码
     */
    private String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
