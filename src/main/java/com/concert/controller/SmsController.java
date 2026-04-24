package com.concert.controller;

import com.concert.common.Result;
import com.concert.service.SmsVerificationCodeService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @description:    短信相关控制器
 * @author: hzf
 * @date: 2026年04月24日 11:23
 * @version: 1.0
 */
@RestController
@RequestMapping("/api/sms")
public class SmsController {
    @Resource
    private SmsVerificationCodeService smsService;

    /**
     * 发送验证码
     */
    @PostMapping("/send")
    public Result<Boolean> sendCode(@RequestParam String phone) {
        // 校验手机号格式（简单校验）
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error("手机号格式错误");
        }
        boolean result = smsService.sendCode(phone);
        // 生产环境不要返回验证码，这里仅为调试
        return Result.success("验证码已发送", result);
    }

    /**
     * 验证验证码（通常不在前端直接调用，而是注册/登录时内部使用）
     */
    @PostMapping("/verify")
    public Result<Boolean> verifyCode(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        String code = params.get("code");
        boolean valid = smsService.verifyCode(phone, code);
        return Result.success(valid);
    }
}
