package com.concert.config.security;

import com.concert.entity.SysRole;
import com.concert.entity.User;
import com.concert.enums.UserStatus;
import com.concert.mapper.UserMapper;
import com.concert.service.SysPermissionService;
import com.concert.service.SysRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserDetailsServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private SysRoleService sysRoleService;

    @Mock
    private SysPermissionService sysPermissionService;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setPassword("encoded");
        user.setStatus(UserStatus.NORMAL);
    }

    @Test
    @DisplayName("加载用户时角色只查一次，权限复用同一批角色")
    void testLoadUserByUsername_ReusesRolesForPermissions() {
        // Given
        SysRole adminRole = new SysRole();
        adminRole.setId(10L);
        adminRole.setRoleCode("ADMIN");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(sysRoleService.getRolesByUserId(1L)).thenReturn(Collections.singletonList(adminRole));
        when(sysPermissionService.getPermissionCodesByRoleIds(any()))
                .thenReturn(Arrays.asList("concert:add", "order:refund"));

        // When
        LoginUser loginUser = (LoginUser) userDetailsService.loadUserByUsername("13800138000");

        // Then：角色与权限都正确装载
        assertEquals(1L, loginUser.getId());
        assertEquals(Collections.singletonList("ADMIN"), loginUser.getRoles());
        assertEquals(Arrays.asList("concert:add", "order:refund"), loginUser.getPermissions());

        // 角色只查一次，且权限按已查出的角色ID计算
        verify(sysRoleService).getRolesByUserId(1L);
        ArgumentCaptor<List<Long>> roleIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sysPermissionService).getPermissionCodesByRoleIds(roleIdsCaptor.capture());
        assertEquals(Collections.singletonList(10L), roleIdsCaptor.getValue());

        // 不再走会重复查角色的旧入口
        verify(sysPermissionService, never()).getPermissionCodesByUserId(any());
    }

    @Test
    @DisplayName("用户无角色时权限为空且不抛异常")
    void testLoadUserByUsername_NoRoles_ReturnsEmptyPermissions() {
        // Given
        when(userMapper.selectOne(any())).thenReturn(user);
        when(sysRoleService.getRolesByUserId(1L)).thenReturn(Collections.emptyList());
        when(sysPermissionService.getPermissionCodesByRoleIds(any())).thenReturn(Collections.emptyList());

        // When
        LoginUser loginUser = (LoginUser) userDetailsService.loadUserByUsername("13800138000");

        // Then
        assertEquals(Collections.emptyList(), loginUser.getRoles());
        assertEquals(Collections.emptyList(), loginUser.getPermissions());
    }
}
