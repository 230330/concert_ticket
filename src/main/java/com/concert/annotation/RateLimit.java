package com.concert.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * 基于 Redis 实现滑动窗口限流，防止恶意刷单和接口滥用
 *
 * <p>使用示例：
 * <pre>
 *   &#64;RateLimit(key = "order:create", count = 5, period = 60)
 *   public OrderResponse createOrder(...) { ... }
 * </pre>
 *
 * @author hzf
 * @date 2026/05/28
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流 key 前缀（会自动加上 "rate_limit:" 前缀和用户ID/IP）
     */
    String key() default "";

    /**
     * 时间窗口内允许的最大请求次数
     */
    int count() default 10;

    /**
     * 时间窗口大小（秒）
     */
    int period() default 60;

    /**
     * 限流维度：USER（按用户ID限流）或 IP（按IP限流）
     */
    LimitType limitType() default LimitType.USER;

    /**
     * 限流维度枚举
     */
    enum LimitType {
        /** 按用户ID限流 */
        USER,
        /** 按IP地址限流 */
        IP
    }
}
