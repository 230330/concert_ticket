package com.concert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concert.entity.SysPermission;

import java.util.List;

/**
 * @description:    系统权限服务接口
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

public interface SysPermissionService extends IService<SysPermission> {

    /**
     * 根据角色ID列表查询权限编码
     *
     * @param roleIds 角色ID列表
     * @return 权限编码列表
     */
    List<String> getPermissionCodesByRoleIds(List<Long> roleIds);

    /**
     * 根据用户ID查询权限编码
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    List<String> getPermissionCodesByUserId(Long userId);
}
