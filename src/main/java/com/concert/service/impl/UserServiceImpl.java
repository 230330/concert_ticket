package com.concert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.dto.request.ChangePasswordRequest;
import com.concert.dto.request.LoginRequest;
import com.concert.dto.request.RegisterRequest;
import com.concert.dto.request.ResetPasswordRequest;
import com.concert.dto.request.UserUpdateRequest;
import com.concert.dto.response.AdminUserResponse;
import com.concert.dto.response.LoginResponse;
import com.concert.dto.response.PageResponse;
import com.concert.dto.response.UserInfoResponse;
import com.concert.entity.Order;
import com.concert.entity.SysRole;
import com.concert.entity.User;
import com.concert.enums.UserStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.NotFoundException;
import com.concert.mapper.UserMapper;
import com.concert.service.OrderService;
import com.concert.service.SmsVerificationCodeService;
import com.concert.service.SysRoleService;
import com.concert.service.UserService;
import com.concert.utils.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description:    用户服务实现类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private OrderService orderService;

    @Resource
    private SmsVerificationCodeService smsVerificationCodeService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Override
    public void register(RegisterRequest request) {
        // 1. 验证码校验
        boolean codeValid = smsVerificationCodeService.verifyCode(request.getPhone(), request.getCode());
        if (!codeValid) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 2. 手机号唯一性校验
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, request.getPhone());
        User existUser = this.getOne(queryWrapper);
        if (existUser != null) {
            throw new BusinessException("该手机号已注册");
        }

        // 3. 创建用户
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.NORMAL);
        user.setAvatar("/default-avatar.svg");

        // 4. 保存用户
        boolean saved = this.save(user);
        if (!saved) {
            throw new BusinessException("注册失败，请稍后重试");
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 使用 AuthenticationManager 进行认证
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPassword());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (BadCredentialsException e) {
            throw new BusinessException("用户名或密码错误");
        } catch (DisabledException e) {
            throw new BusinessException("账号已被禁用");
        } catch (AuthenticationException e) {
            throw new BusinessException("认证失败，请重新登录");
        }

        // 2. 认证成功，获取用户信息
        com.concert.config.security.LoginUser loginUser =
                (com.concert.config.security.LoginUser) authentication.getPrincipal();

        // 3. 生成 JWT Token
        String token = jwtUtil.generateToken(loginUser.getId(), loginUser.getPhone());

        // 4. 返回登录响应
        return new LoginResponse(token, jwtExpiration);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // 1. 校验新密码与确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("新密码与确认密码不一致");
        }

        // 2. 验证码校验
        boolean codeValid = smsVerificationCodeService.verifyCode(request.getPhone(), request.getCode());
        if (!codeValid) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 3. 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, request.getPhone());
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new NotFoundException("该手机号未注册");
        }

        // 4. 校验用户状态
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException("该账号已被禁用，请联系管理员");
        }

        // 5. 校验新密码不能与原密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        // 6. 更新密码
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(passwordEncoder.encode(request.getNewPassword()));

        boolean updated = this.updateById(updateUser);
        if (!updated) {
            throw new BusinessException("密码重置失败，请稍后重试");
        }
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 1. 校验新密码与确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("新密码与确认密码不一致");
        }

        // 2. 获取当前用户信息
        User user = this.getById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }

        // 3. 校验原密码是否正确
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }

        // 4. 校验新密码不能与原密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        // 5. 更新密码
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(passwordEncoder.encode(request.getNewPassword()));

        boolean updated = this.updateById(updateUser);
        if (!updated) {
            throw new BusinessException("密码修改失败，请稍后重试");
        }
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }

        // 转换为响应对象
        UserInfoResponse response = new UserInfoResponse();
        BeanUtils.copyProperties(user, response);

        // 查询角色列表
        List<SysRole> roles = sysRoleService.getRolesByUserId(userId);
        response.setRoles(roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));

        return response;
    }

    @Override
    public void updateUserInfo(Long userId, UserUpdateRequest request) {
        // 从数据库获取当前用户信息
        User currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new NotFoundException("用户不存在");
        }

        // 构建更新对象
        User user = new User();
        user.setId(userId);
        user.setAvatar(request.getAvatar());

        // 昵称修改校验
        if (request.getNickname() != null && !request.getNickname().equals(currentUser.getNickname())) {
            // 1. 检查一个月内是否已修改过昵称
            if (currentUser.getNicknameLastModified() != null) {
                LocalDateTime nextAllowedTime = currentUser.getNicknameLastModified().plusMonths(1);
                if (LocalDateTime.now().isBefore(nextAllowedTime)) {
                    throw new BusinessException("昵称修改过于频繁，每月仅可修改一次");
                }
            }

            // 2. 检查昵称是否与其他用户重名
            LambdaQueryWrapper<User> nicknameQuery = new LambdaQueryWrapper<>();
            nicknameQuery.eq(User::getNickname, request.getNickname());
            User existUser = this.getOne(nicknameQuery);
            if (existUser != null && !existUser.getId().equals(userId)) {
                throw new BusinessException("该昵称已被其他用户使用");
            }

            user.setNickname(request.getNickname());
            user.setNicknameLastModified(LocalDateTime.now());
        }

        boolean updated = this.updateById(user);
        if (!updated) {
            throw new BusinessException("更新失败，请稍后重试");
        }
    }

    @Override
    public PageResponse<AdminUserResponse> listUsersForAdmin(int page, int size, String phone, Integer status) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (phone != null && !phone.trim().isEmpty()) {
            queryWrapper.like(User::getPhone, phone.trim());
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }
        queryWrapper.orderByDesc(User::getId);

        Page<User> userPage = new Page<>(page, size);
        this.page(userPage, queryWrapper);

        List<User> users = userPage.getRecords();
        if (users.isEmpty()) {
            return new PageResponse<>(userPage.getCurrent(), userPage.getSize(),
                    userPage.getTotal(), Collections.emptyList());
        }

        List<AdminUserResponse> responseList = users.stream()
                .map(this::convertToAdminUserResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(userPage.getCurrent(), userPage.getSize(),
                userPage.getTotal(), responseList);
    }

    @Override
    public AdminUserResponse getUserDetailForAdmin(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return convertToAdminUserResponse(user);
    }

    @Override
    public void updateUserStatus(Long userId, UserStatus status) {
        User user = this.getById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        user.setStatus(status);
        this.updateById(user);
    }

    /**
     * 将 User 实体转换为 AdminUserResponse（含角色和订单统计）
     */
    private AdminUserResponse convertToAdminUserResponse(User user) {
        AdminUserResponse resp = new AdminUserResponse();
        resp.setId(user.getId());
        resp.setPhone(user.getPhone());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getAvatar());
        resp.setStatus(user.getStatus());
        resp.setCreateTime(user.getCreateTime());
        resp.setUpdateTime(user.getUpdateTime());

        // 查询角色
        List<SysRole> roles = sysRoleService.getRolesByUserId(user.getId());
        resp.setRoles(roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));

        // 查询订单数
        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.eq(Order::getUserId, user.getId());
        resp.setOrderCount(orderService.count(orderQuery));

        return resp;
    }
}
