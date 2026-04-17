package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.SysRolePermission;
import com.concert.mapper.SysRolePermissionMapper;
import com.concert.service.SysRolePermissionService;
import org.springframework.stereotype.Service;

/**
 * 角色-权限关联服务实现类
 */
@Service
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission> implements SysRolePermissionService {

}
