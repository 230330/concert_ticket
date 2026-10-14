package com.concert.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.dto.request.ChangePasswordRequest;
import com.concert.dto.request.LoginRequest;
import com.concert.dto.request.RegisterRequest;
import com.concert.dto.response.AdminUserResponse;
import com.concert.dto.response.LoginResponse;
import com.concert.dto.response.PageResponse;
import com.concert.dto.response.UserInfoResponse;
import com.concert.entity.SysRole;
import com.concert.entity.User;
import com.concert.enums.UserStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.NotFoundException;
import com.concert.service.impl.UserServiceImpl;
import com.concert.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 * 使用 @Spy 因为 UserServiceImpl 继承 ServiceImpl，getById/getOne/save 等方法依赖 baseMapper
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private SysRoleService sysRoleService;

    @Mock
    private OrderService orderService;

    @Mock
    private SmsVerificationCodeService smsVerificationCodeService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setPhone("13800138000");
        testUser.setPassword("encoded_password");
        testUser.setStatus(UserStatus.NORMAL);
        testUser.setNickname("测试用户");
    }

    @Nested
    @DisplayName("用户注册")
    class Register {

        @Test
        @DisplayName("注册成功")
        void testRegister_Success() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setPhone("13800138000");
            request.setPassword("password123");
            request.setCode("123456");

            when(smsVerificationCodeService.verifyCode("13800138000", "123456")).thenReturn(true);
            doReturn(null).when(userService).getOne(any());
            when(passwordEncoder.encode("password123")).thenReturn("encoded_pwd");
            doReturn(true).when(userService).save(any(User.class));

            // When
            userService.register(request);

            // Then
            verify(userService).save(any(User.class));
        }

        @Test
        @DisplayName("验证码错误抛出BusinessException")
        void testRegister_InvalidCode_ThrowsException() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setPhone("13800138000");
            request.setPassword("password123");
            request.setCode("wrong_code");

            when(smsVerificationCodeService.verifyCode("13800138000", "wrong_code")).thenReturn(false);

            // When & Then
            assertThrows(BusinessException.class, () -> userService.register(request));
            verify(userService, never()).save(any());
        }

        @Test
        @DisplayName("手机号已注册抛出BusinessException")
        void testRegister_PhoneExists_ThrowsException() {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setPhone("13800138000");
            request.setPassword("password123");
            request.setCode("123456");

            when(smsVerificationCodeService.verifyCode("13800138000", "123456")).thenReturn(true);
            doReturn(testUser).when(userService).getOne(any());

            // When & Then
            assertThrows(BusinessException.class, () -> userService.register(request));
            verify(userService, never()).save(any());
        }
    }

    @Nested
    @DisplayName("用户登录")
    class Login {

        @Test
        @DisplayName("登录成功返回Token")
        void testLogin_Success() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPhone("13800138000");
            request.setPassword("password123");

            // 登录流程会先读取 Redis 中的失败计数，此处模拟"无历史失败记录"
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

            Authentication authentication = mock(Authentication.class);
            com.concert.config.security.LoginUser loginUser = mock(com.concert.config.security.LoginUser.class);
            when(loginUser.getId()).thenReturn(1L);
            when(loginUser.getPhone()).thenReturn("13800138000");
            when(authentication.getPrincipal()).thenReturn(loginUser);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtUtil.generateToken(1L, "13800138000")).thenReturn("test_token");

            // When
            LoginResponse response = userService.login(request);

            // Then
            assertNotNull(response);
            assertEquals("test_token", response.getToken());
            assertEquals("Bearer", response.getTokenType());
        }

        @Test
        @DisplayName("密码错误抛出BusinessException")
        void testLogin_WrongPassword_ThrowsException() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPhone("13800138000");
            request.setPassword("wrong_password");

            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When & Then
            BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(request));
            assertTrue(ex.getMessage().contains("用户名或密码错误"));
        }

        @Test
        @DisplayName("账号被禁用抛出BusinessException")
        void testLogin_DisabledAccount_ThrowsException() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setPhone("13800138000");
            request.setPassword("password123");

            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new DisabledException("Account disabled"));

            // When & Then
            BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(request));
            assertTrue(ex.getMessage().contains("账号已被禁用"));
        }
    }

    @Nested
    @DisplayName("修改密码")
    class ChangePassword {

        @Test
        @DisplayName("新密码与确认密码不一致抛出BusinessException")
        void testChangePassword_Mismatch_ThrowsException() {
            // Given
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("oldPwd123");
            request.setNewPassword("newPwd456");
            request.setConfirmPassword("differentPwd");

            // When & Then
            assertThrows(BusinessException.class, () -> userService.changePassword(1L, request));
        }

        @Test
        @DisplayName("原密码错误抛出BusinessException")
        void testChangePassword_WrongOldPassword_ThrowsException() {
            // Given
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("wrongOldPwd");
            request.setNewPassword("newPwd456");
            request.setConfirmPassword("newPwd456");

            doReturn(testUser).when(userService).getById(1L);
            when(passwordEncoder.matches("wrongOldPwd", "encoded_password")).thenReturn(false);

            // When & Then
            assertThrows(BusinessException.class, () -> userService.changePassword(1L, request));
        }

        @Test
        @DisplayName("新密码与原密码相同抛出BusinessException")
        void testChangePassword_SameAsOld_ThrowsException() {
            // Given
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("samePwd123");
            request.setNewPassword("samePwd123");
            request.setConfirmPassword("samePwd123");

            doReturn(testUser).when(userService).getById(1L);
            when(passwordEncoder.matches("samePwd123", "encoded_password")).thenReturn(true);

            // When & Then
            assertThrows(BusinessException.class, () -> userService.changePassword(1L, request));
        }
    }

    @Nested
    @DisplayName("获取用户信息")
    class GetUserInfo {

        @Test
        @DisplayName("获取用户信息成功")
        void testGetUserInfo_Success() {
            // Given
            doReturn(testUser).when(userService).getById(1L);

            SysRole role = new SysRole();
            role.setId(1L);
            role.setRoleCode("USER");
            when(sysRoleService.getRolesByUserId(1L)).thenReturn(Collections.singletonList(role));

            // When
            UserInfoResponse response = userService.getUserInfo(1L);

            // Then
            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals(1, response.getRoles().size());
            assertEquals("USER", response.getRoles().get(0));
        }

        @Test
        @DisplayName("用户不存在抛出NotFoundException")
        void testGetUserInfo_NotFound_ThrowsException() {
            // Given
            doReturn(null).when(userService).getById(999L);

            // When & Then
            assertThrows(NotFoundException.class, () -> userService.getUserInfo(999L));
        }
    }

    @Nested
    @DisplayName("更新用户状态")
    class UpdateUserStatus {

        @Test
        @DisplayName("封禁用户成功")
        void testUpdateUserStatus_Ban_Success() {
            // Given
            doReturn(testUser).when(userService).getById(1L);
            doReturn(true).when(userService).updateById(any(User.class));

            // When
            userService.updateUserStatus(1L, UserStatus.DISABLED);

            // Then
            verify(userService).updateById(any(User.class));
        }

        @Test
        @DisplayName("用户不存在抛出NotFoundException")
        void testUpdateUserStatus_NotFound_ThrowsException() {
            // Given
            doReturn(null).when(userService).getById(999L);

            // When & Then
            assertThrows(NotFoundException.class, () -> userService.updateUserStatus(999L, UserStatus.DISABLED));
        }
    }

    @Nested
    @DisplayName("管理端用户列表")
    class ListUsersForAdmin {

        @Test
        @DisplayName("角色与订单数按批量接口查询，不逐用户查询")
        void testListUsersForAdmin_BatchesRoleAndOrderQueries() {
            // Given
            User first = new User();
            first.setId(1L);
            first.setPhone("13800000001");
            User second = new User();
            second.setId(2L);
            second.setPhone("13800000002");

            doAnswer(invocation -> {
                Page<User> page = invocation.getArgument(0);
                page.setRecords(Arrays.asList(first, second));
                page.setTotal(2L);
                return page;
            }).when(userService).page(any(), any());

            when(sysRoleService.getRoleCodesByUserIds(any()))
                    .thenReturn(Collections.singletonMap(1L, Collections.singletonList("ADMIN")));
            when(orderService.countByUserIds(any()))
                    .thenReturn(Collections.singletonMap(1L, 3L));

            // When
            PageResponse<AdminUserResponse> response = userService.listUsersForAdmin(1, 10, null, null);

            // Then
            assertEquals(2, response.getRecords().size());
            AdminUserResponse firstResp = response.getRecords().get(0);
            assertEquals(Collections.singletonList("ADMIN"), firstResp.getRoles());
            assertEquals(3L, firstResp.getOrderCount());

            // 第二个用户没有角色与订单，取默认值
            AdminUserResponse secondResp = response.getRecords().get(1);
            assertEquals(Collections.emptyList(), secondResp.getRoles());
            assertEquals(0L, secondResp.getOrderCount());

            // 批量接口各调用一次，且不再出现逐用户的角色/订单查询
            verify(sysRoleService, times(1)).getRoleCodesByUserIds(any());
            verify(orderService, times(1)).countByUserIds(any());
            verify(sysRoleService, never()).getRolesByUserId(any());
            verify(orderService, never()).count(any());
        }
    }
}
