package com.concert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.SysRole;
import com.concert.entity.SysUserRole;
import com.concert.mapper.SysRoleMapper;
import com.concert.mapper.SysUserRoleMapper;
import com.concert.service.SysRoleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description:    角色服务实现类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        // 1. 查询用户-角色关联
        LambdaQueryWrapper<SysUserRole> urQuery = new LambdaQueryWrapper<>();
        urQuery.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(urQuery);

        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询角色信息（仅正常状态）
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<SysRole> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1);
        return this.list(roleQuery);
    }

    @Override
    public SysRole getByRoleCode(String roleCode) {
        LambdaQueryWrapper<SysRole> query = new LambdaQueryWrapper<>();
        query.eq(SysRole::getRoleCode, roleCode);
        return this.getOne(query);
    }
}
