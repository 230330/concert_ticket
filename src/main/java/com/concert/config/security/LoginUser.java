package com.concert.config.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: 登录用户信息
 * @author: hzf
 * @date: 2026-04-17 15:26
 */
@Data
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户状态：0-禁用，1-正常
     */
    private Integer status;

    /**
     * 权限编码列表
     */
    private List<String> permissions;

    /**
     * 角色编码列表
     */
    private List<String> roles;

    public LoginUser() {
    }

    public LoginUser(Long id, String phone, String password, Integer status, List<String> permissions, List<String> roles) {
        this.id = id;
        this.phone = phone;
        this.password = password;
        this.status = status;
        this.permissions = permissions;
        this.roles = roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 添加权限编码（如 concert:add）
        if (permissions != null) {
            authorities.addAll(permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList()));
        }
        // 添加角色编码（如 ROLE_ADMIN）
        if (roles != null) {
            authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList()));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        // 使用手机号作为用户名
        return this.phone;
    }

    /**
     * 账户是否未过期
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账户是否未锁定
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 凭证是否未过期
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 是否可用（状态为1时可用）
     */
    @Override
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }

    /**
     * 判断用户是否拥有指定角色
     *
     * @param roleCode 角色编码
     * @return 是否拥有
     */
    public boolean hasRole(String roleCode) {
        return roles != null && roles.contains(roleCode);
    }

    /**
     * 判断用户是否拥有指定权限
     *
     * @param permissionCode 权限编码
     * @return 是否拥有
     */
    public boolean hasPermission(String permissionCode) {
        return permissions != null && permissions.contains(permissionCode);
    }
}
