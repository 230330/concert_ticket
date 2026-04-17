package com.concert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.SysPermission;
import com.concert.entity.SysRole;
import com.concert.entity.SysRolePermission;
import com.concert.entity.SysUserRole;
import com.concert.mapper.SysPermissionMapper;
import com.concert.mapper.SysRolePermissionMapper;
import com.concert.mapper.SysUserRoleMapper;
import com.concert.service.SysPermissionService;
import com.concert.service.SysRoleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: 权限服务实现类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysRoleService sysRoleService;

    @Override
    public List<String> getPermissionCodesByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 查询角色-权限关联
        LambdaQueryWrapper<SysRolePermission> rpQuery = new LambdaQueryWrapper<>();
        rpQuery.in(SysRolePermission::getRoleId, roleIds);
        List<SysRolePermission> rolePermissions = sysRolePermissionMapper.selectList(rpQuery);

        if (rolePermissions.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询权限信息
        List<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<SysPermission> permQuery = new LambdaQueryWrapper<>();
        permQuery.in(SysPermission::getId, permissionIds)
                .eq(SysPermission::getStatus, 1);
        List<SysPermission> permissions = this.list(permQuery);

        return permissions.stream()
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        // 1. 获取用户的角色列表
        List<SysRole> roles = sysRoleService.getRolesByUserId(userId);
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取角色对应的权限编码
        List<Long> roleIds = roles.stream()
                .map(SysRole::getId)
                .collect(Collectors.toList());

        return getPermissionCodesByRoleIds(roleIds);
    }
}
