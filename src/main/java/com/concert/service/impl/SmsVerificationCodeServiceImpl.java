package com.concert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.SmsVerificationCode;
import com.concert.mapper.SmsVerificationCodeMapper;
import com.concert.service.SmsVerificationCodeService;
import com.concert.utils.SecureRandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @description:    短信验证码服务实现类
 * @author: hzf
 * @date: 2026年04月24日 10:38
 * @version: 1.0
 */
@Slf4j
@Service
public class SmsVerificationCodeServiceImpl
        extends ServiceImpl<SmsVerificationCodeMapper, SmsVerificationCode>
        implements SmsVerificationCodeService {

    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int CODE_LENGTH = 6;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendCode(String phone) {
        if (StringUtils.isBlank(phone)) {
            log.warn("手机号为空，无法发送验证码");
            return false;
        }

        // 1. 生成6位数字验证码（安全随机）
        String code = SecureRandomUtil.generateNumericCode(CODE_LENGTH);

        // 2. 将同一手机号之前未过期的验证码标记为已使用（防止重复发送）
        LambdaQueryWrapper<SmsVerificationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsVerificationCode::getPhone, phone)
                .eq(SmsVerificationCode::getUsed, false)
                .gt(SmsVerificationCode::getExpireTime, LocalDateTime.now());
        SmsVerificationCode oldCode = this.getOne(wrapper);
        if (oldCode != null) {
            oldCode.setUsed(true);
            this.updateById(oldCode);
        }

        // 3. 保存新的验证码
        SmsVerificationCode newCode = new SmsVerificationCode();
        newCode.setPhone(phone);
        newCode.setCode(code);
        newCode.setExpireTime(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        newCode.setUsed(false);
        this.save(newCode);

        // TODO: 调用阿里云短信 SDK 发送短信（目前打印日志模拟）
        log.info("【演唱会订票系统】验证码发送成功，手机号：{}，验证码：{}，有效期：{}分钟",
                phone, code, CODE_EXPIRE_MINUTES);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyCode(String phone, String code) {
        if (StringUtils.isBlank(phone) || StringUtils.isBlank(code)) {
            log.warn("手机号或验证码为空，验证失败");
            return false;
        }

        // 查询未使用且未过期的验证码（取最新一条）
        LambdaQueryWrapper<SmsVerificationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsVerificationCode::getPhone, phone)
                .eq(SmsVerificationCode::getUsed, false)
                .gt(SmsVerificationCode::getExpireTime, LocalDateTime.now())
                .orderByDesc(SmsVerificationCode::getCreateTime);

        SmsVerificationCode record = this.getOne(wrapper);
        if (record == null) {
            log.warn("验证码已过期或不存在，手机号：{}", phone);
            return false;
        }

        if (record.getCode().equals(code)) {
            // 验证成功，立即标记为已使用，防止重复使用
            record.setUsed(true);
            this.updateById(record);
            log.info("验证码验证成功，手机号：{}", phone);
            return true;
        }

        log.warn("验证码错误，手机号：{}，输入：{}，正确：{}", phone, code, record.getCode());
        return false;
    }
}
