package com.concert.utils;

import com.concert.config.security.LoginUser;
import com.concert.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * @description: 安全工具类，用于获取当前登录用户信息
 * @author: hzf
 * @date: 2026年04月22日 9:47
 * @version: 1.0
 */

public class SecurityUtil {

    /**
     * 获取当前登录用户的ID
     * @return 用户ID
     * @throws UnauthorizedException 如果用户未登录或认证信息无效
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("用户未登录");
        }
        Object principal = auth.getPrincipal();
        if (principal == null) {
            throw new UnauthorizedException("用户信息为空");
        }
        if (principal instanceof LoginUser) {
            return ((LoginUser) principal).getId();
        }
        throw new UnauthorizedException("用户未登录或认证信息类型不支持");
    }
}
