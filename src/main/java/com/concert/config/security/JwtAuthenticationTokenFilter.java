package com.concert.config.security;

import com.concert.utils.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 认证过滤器
 * 从请求头中提取 Token，解析并设置 Spring Security 上下文
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 从请求头获取 Token
        String authHeader = request.getHeader(jwtUtil.getHeader());

        if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith(jwtUtil.getPrefix() + " ")) {
            // 提取 Token（去除前缀）
            String token = authHeader.substring(jwtUtil.getPrefix().length() + 1);

            // 验证 Token 有效性
            if (jwtUtil.validateToken(token)) {
                // 从 Token 中获取手机号
                String phone = jwtUtil.getPhone(token);

                // 当前上下文中没有认证信息时才进行认证
                if (StringUtils.isNotBlank(phone) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 加载用户信息
                    UserDetails userDetails = userDetailsService.loadUserByUsername(phone);

                    // 验证用户是否可用
                    if (userDetails.isEnabled()) {
                        // 创建认证令牌
                        UsernamePasswordAuthenticationToken authenticationToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 设置到 Security 上下文
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                }
            }
        }

        // 继续过滤器链
        filterChain.doFilter(request, response);
    }
}
