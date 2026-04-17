package com.concert.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.common.Result;
import com.concert.config.security.LoginUser;
import com.concert.dto.request.LoginRequest;
import com.concert.dto.request.RegisterRequest;
import com.concert.dto.request.SendSmsRequest;
import com.concert.dto.request.UserUpdateRequest;
import com.concert.dto.response.LoginResponse;
import com.concert.dto.response.UserInfoResponse;
import com.concert.entity.User;
import com.concert.exception.BusinessException;
import com.concert.service.SmsCodeService;
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
    private SmsCodeService smsCodeService;

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
        boolean codeValid = smsCodeService.verifyCode(request.getPhone(), request.getCode());
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
        user.setUsername(request.getPhone()); // 默认用户名为手机号
        user.setPassword(passwordEncoder.encode(request.getPassword())); // 密码加密
        user.setStatus(1); // 默认启用

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
        boolean sent = smsCodeService.sendCode(request.getPhone());
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

        // 构建更新对象
        User user = new User();
        user.setId(loginUser.getId());
        user.setNickname(request.getNickname());
        user.setAvatar(request.getAvatar());
        user.setEmail(request.getEmail());

        // 更新用户信息
        boolean updated = userService.updateById(user);
        if (updated) {
            return Result.success();
        }
        return Result.error("更新失败，请稍后重试");
    }
}
