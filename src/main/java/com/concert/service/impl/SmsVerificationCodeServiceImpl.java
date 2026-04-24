package com.concert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.SmsVerificationCode;
import com.concert.mapper.SmsVerificationCodeMapper;
import com.concert.service.SmsVerificationCodeService;
import com.concert.utils.SecureRandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @description:    短信验证码服务实现类
 * @author: hzf
 * @date: 2026年04月24日 10:38
 * @version: 1.0
 */
@Slf4j
public class SmsVerificationCodeServiceImpl extends ServiceImpl<SmsVerificationCodeMapper, SmsVerificationCode>
        implements SmsVerificationCodeService {
    /**
     * 验证码有效期（分钟）
     */
    private static final int CODE_EXPIRE_MINUTES = 5;
    /**
     * 验证码长度
     */
    private static final int CODE_LENGTH = 6;

    /**
     * 发送验证码
     * @param phone 手机号
     * @return 验证码（用于调试，实际生产可不返回）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String sendCode(String phone) {
        //使用安全随机数生成6位数字验证码
        String code = SecureRandomUtil.generateNumericCode(CODE_LENGTH);
        // 将之前未过期的验证码标记为过期（可选，防止过多脏数据）
        LambdaQueryWrapper<SmsVerificationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsVerificationCode::getPhone, phone)
                .eq(SmsVerificationCode::getUsed, false)
                .gt(SmsVerificationCode::getExpireTime, LocalDateTime.now());
        SmsVerificationCode smsVerificationCode = this.getOne(wrapper);
        // 如果存在未过期的验证码，则标记为过期，防止重复发送
        if (smsVerificationCode != null) {
            smsVerificationCode.setUsed(true);
            this.updateById(smsVerificationCode);
        }

        // 存入新验证码
        SmsVerificationCode newCode = new SmsVerificationCode();
        newCode.setPhone(phone);
        newCode.setCode(code);
        newCode.setExpireTime(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        newCode.setUsed(false);
        this.save(newCode);

        // 实际项目需要调用阿里云短信接口，这里仅打印日志
        log.info("发送验证码到 {}：{}，有效期 {} 分钟", phone, code, CODE_EXPIRE_MINUTES);

        // 返回验证码（方便调试，生产环境可去掉）
        return code;
    }

    /**
     * 验证验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否验证通过
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyCode(String phone, String code) {
        //查询数据库中最新的一条未使用的验证码
        LambdaQueryWrapper<SmsVerificationCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SmsVerificationCode::getPhone, phone)
                .eq(SmsVerificationCode::getUsed, false)
                .gt(SmsVerificationCode::getExpireTime, LocalDateTime.now())
                .orderByDesc(SmsVerificationCode::getCreateTime);

        SmsVerificationCode smsVerificationCode = this.getOne(wrapper);
        // 如果存在未过期的验证码，则验证通过
        if (smsVerificationCode != null && smsVerificationCode.getCode().equals(code)) {
            return true;
        }
        //标记为使用，防止重复使用
        smsVerificationCode.setUsed(true);
        return this.updateById(smsVerificationCode);
    }
}
