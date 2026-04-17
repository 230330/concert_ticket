package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.SysUserRole;
import com.concert.mapper.SysUserRoleMapper;
import com.concert.service.SysUserRoleService;
import org.springframework.stereotype.Service;

/**
 * 用户-角色关联服务实现类
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

}
