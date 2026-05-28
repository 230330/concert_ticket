package com.concert.aspect;

import com.concert.annotation.RateLimit;
import com.concert.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 * 基于 Redis INCR + EXPIRE 实现固定窗口限流
 *
 * @author hzf
 * @date 2026/05/28
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(com.concert.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        // 构建限流 key
        String limitKey = buildLimitKey(rateLimit);

        // 执行限流逻辑
        boolean allowed = checkRateLimit(limitKey, rateLimit.count(), rateLimit.period());

        if (!allowed) {
            logger.warn("接口限流触发：key={}, 限制={}/{}s", limitKey, rateLimit.count(), rateLimit.period());
            throw new BusinessException("操作过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    /**
     * 构建限流 key
     * 格式：rate_limit:{自定义key}:{用户ID或IP}
     */
    private String buildLimitKey(RateLimit rateLimit) {
        String identifier;

        if (rateLimit.limitType() == RateLimit.LimitType.USER) {
            // 从 Security 上下文获取用户ID
            identifier = getCurrentUserId();
        } else {
            // 从请求获取 IP
            identifier = getClientIP();
        }

        String key = rateLimit.key();
        if (key == null || key.isEmpty()) {
            // 默认使用类名.方法名
            key = "default";
        }

        return RATE_LIMIT_KEY_PREFIX + key + ":" + identifier;
    }

    /**
     * Redis 固定窗口限流
     * 使用 INCR + EXPIRE 实现，原子性由 Redis 单线程保证
     */
    private boolean checkRateLimit(String key, int maxCount, int periodSeconds) {
        // 获取当前计数
        Long count = stringRedisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            // 首次请求，设置过期时间
            stringRedisTemplate.expire(key, periodSeconds, TimeUnit.SECONDS);
        }

        return count != null && count <= maxCount;
    }

    /**
     * 获取当前登录用户ID
     */
    private String getCurrentUserId() {
        try {
            org.springframework.security.core.Authentication authentication =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.debug("获取用户ID失败，降级为IP限流", e);
        }
        return getClientIP();
    }

    /**
     * 获取客户端 IP 地址
     */
    private String getClientIP() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
