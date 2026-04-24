package com.concert.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @description:  通知服务
 * @author: hzf
 * @date: 2026年04月24日 16:26
 * @version: 1.0
 */
@Service
@Slf4j
public class NotificationService {
    @Async
    public void sendPickupCodeSms(String phone, String pickupCode) {
        // 调用阿里云短信 SDK 发送取票码
        // 记录日志，失败时可选重试

        // 测试阶段：仅打印日志，不调用真实短信SDK
        log.info("【测试模式】向手机号 {} 发送取票码：{}", phone, pickupCode);
        // 后续对接阿里云时，再替换为真实短信发送代码
    }
}
