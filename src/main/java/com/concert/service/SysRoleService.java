package com.concert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concert.entity.SysRole;

import java.util.List;

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
}
