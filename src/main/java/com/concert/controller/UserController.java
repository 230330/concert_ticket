package com.concert.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.common.Result;
import com.concert.config.security.LoginUser;
import com.concert.dto.request.ChangePasswordRequest;
import com.concert.dto.request.LoginRequest;
import com.concert.dto.request.RegisterRequest;
import com.concert.dto.request.SendSmsRequest;
import com.concert.dto.request.UserUpdateRequest;
import com.concert.dto.response.LoginResponse;
import com.concert.dto.response.UserInfoResponse;
import com.concert.entity.SysRole;
import com.concert.entity.User;
import com.concert.enums.UserStatus;
import com.concert.exception.BusinessException;
import com.concert.service.SmsVerificationCodeService;
import com.concert.service.SysRoleService;
import com.concert.service.UserService;
import com.concert.utils.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description:    用户相关控制器
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private SmsVerificationCodeService smsVerificationCodeService;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private AuthenticationManager authenticationManager;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Validated RegisterRequest request) {
        // 1. 验证码校验
        boolean codeValid = smsVerificationCodeService.verifyCode(request.getPhone(), request.getCode());
        if (!codeValid) {
            return Result.error("验证码错误或已过期");
        }

        // 2. 手机号唯一性校验
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, request.getPhone());
        User existUser = userService.getOne(queryWrapper);
        if (existUser != null) {
            return Result.error("该手机号已注册");
        }

        // 3. 创建用户
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // 密码加密
        user.setStatus(UserStatus.NORMAL); // 默认启用

        // 4. 保存用户
        boolean saved = userService.save(user);
        if (saved) {
            return Result.success();
        }
        return Result.error("注册失败，请稍后重试");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
        // 1. 使用 AuthenticationManager 进行认证
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 2. 认证成功，获取用户信息
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 3. 检查用户状态
        if (!loginUser.isEnabled()) {
            throw new BusinessException("账号已被禁用");
        }

        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(loginUser.getId(), loginUser.getPhone());

        // 5. 返回登录响应
        LoginResponse response = new LoginResponse(token, jwtExpiration);
        return Result.success(response);
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/sendSms")
    public Result<Void> sendSms(@RequestBody @Validated SendSmsRequest request) {
        boolean sent = smsVerificationCodeService.sendCode(request.getPhone());
        if (sent) {
            return Result.success();
        }
        return Result.error("验证码发送失败，请稍后重试");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoResponse> getUserInfo() {
        // 从 Security 上下文获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.unauthorized("请先登录");
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 从数据库获取最新用户信息
        User user = userService.getById(loginUser.getId());
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 转换为响应对象
        UserInfoResponse response = new UserInfoResponse();
        BeanUtils.copyProperties(user, response);

        // 查询角色列表
        List<SysRole> roles = sysRoleService.getRolesByUserId(loginUser.getId());
        response.setRoles(roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));

        return Result.success(response);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public Result<Void> updateUserInfo(@RequestBody @Validated UserUpdateRequest request) {
        // 从 Security 上下文获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.unauthorized("请先登录");
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 从数据库获取当前用户信息
        User currentUser = userService.getById(loginUser.getId());
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        // 构建更新对象
        User user = new User();
        user.setId(loginUser.getId());
        user.setAvatar(request.getAvatar());

        // 昵称修改校验
        if (request.getNickname() != null && !request.getNickname().equals(currentUser.getNickname())) {
            // 1. 检查一个月内是否已修改过昵称
            if (currentUser.getNicknameLastModified() != null) {
                LocalDateTime nextAllowedTime = currentUser.getNicknameLastModified().plusMonths(1);
                if (LocalDateTime.now().isBefore(nextAllowedTime)) {
                    return Result.error("昵称修改过于频繁，每月仅可修改一次");
                }
            }

            // 2. 检查昵称是否与其他用户重名
            LambdaQueryWrapper<User> nicknameQuery = new LambdaQueryWrapper<>();
            nicknameQuery.eq(User::getNickname, request.getNickname());
            User existUser = userService.getOne(nicknameQuery);
            if (existUser != null && !existUser.getId().equals(loginUser.getId())) {
                return Result.error("该昵称已被其他用户使用");
            }

            user.setNickname(request.getNickname());
            user.setNicknameLastModified(LocalDateTime.now());
        }

        // 更新用户信息
        boolean updated = userService.updateById(user);
        if (updated) {
            return Result.success();
        }
        return Result.error("更新失败，请稍后重试");
    }

    /**
     * 修改密码
     */
    @PutMapping("/changePassword")
    public Result<Void> changePassword(@RequestBody @Validated ChangePasswordRequest request) {
        // 校验新密码与确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Result.error("新密码与确认密码不一致");
        }

        // 从 Security 上下文获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.unauthorized("请先登录");
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 获取当前用户信息
        User user = userService.getById(loginUser.getId());
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 校验原密码是否正确
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return Result.error("原密码不正确");
        }

        // 校验新密码不能与原密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return Result.error("新密码不能与原密码相同");
        }

        // 更新密码
        User updateUser = new User();
        updateUser.setId(loginUser.getId());
        updateUser.setPassword(passwordEncoder.encode(request.getNewPassword()));

        boolean updated = userService.updateById(updateUser);
        if (updated) {
            return Result.success();
        }
        return Result.error("密码修改失败，请稍后重试");
    }
}
