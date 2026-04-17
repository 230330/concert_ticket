package com.concert.config.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.entity.SysRole;
import com.concert.entity.User;
import com.concert.mapper.UserMapper;
import com.concert.service.SysPermissionService;
import com.concert.service.SysRoleService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserDetailsService 实现类
 * 根据手机号加载用户信息（含 RBAC 权限）
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private SysPermissionService sysPermissionService;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        // 根据手机号查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 加载用户角色
        List<SysRole> roles = sysRoleService.getRolesByUserId(user.getId());
        List<String> roleCodes = roles.isEmpty()
                ? Collections.emptyList()
                : roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());

        // 加载用户权限
        List<String> permissionCodes = sysPermissionService.getPermissionCodesByUserId(user.getId());

        // 构建 LoginUser 对象
        return new LoginUser(
                user.getId(),
                user.getPhone(),
                user.getPassword(),
                user.getStatus(),
                permissionCodes,
                roleCodes
        );
    }
}
