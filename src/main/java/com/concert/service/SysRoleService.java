package com.concert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concert.entity.SysRole;

import java.util.List;
import java.util.Collection;
import java.util.Map;

/**
 * @description:    系统角色服务接口
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

public interface SysRoleService extends IService<SysRole> {

    /**
     * 根据用户ID查询角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> getRolesByUserId(Long userId);

    /**
     * 根据角色编码查询角色
     *
     * @param roleCode 角色编码
     * @return 角色信息
     */
    SysRole getByRoleCode(String roleCode);

    /**
     * 批量查询多个用户的角色编码（列表场景使用，避免逐个用户查询）
     * 统计口径与 getRolesByUserId 一致：只返回状态正常的角色
     *
     * @param userIds 用户ID集合
     * @return 用户ID -> 角色编码列表，没有角色的用户不会出现在结果中
     */
    Map<Long, List<String>> getRoleCodesByUserIds(Collection<Long> userIds);
}
