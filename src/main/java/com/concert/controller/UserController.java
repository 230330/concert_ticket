package com.concert.controller;

import com.concert.common.Result;
import com.concert.dto.request.ChangePasswordRequest;
import com.concert.dto.request.LoginRequest;
import com.concert.dto.request.RegisterRequest;
import com.concert.dto.request.ResetPasswordRequest;
import com.concert.dto.request.SendSmsRequest;
import com.concert.dto.request.UserUpdateRequest;
import com.concert.dto.response.LoginResponse;
import com.concert.dto.response.UserInfoResponse;
import com.concert.service.SmsVerificationCodeService;
import com.concert.service.UserService;
import com.concert.utils.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description:    用户相关控制器
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Tag(name = "用户管理", description = "用户注册、登录、密码管理等接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private SmsVerificationCodeService smsVerificationCodeService;

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "通过手机号+验证码注册新用户")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Validated RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "通过手机号+密码登录，返回JWT Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Validated LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    /**
     * 发送短信验证码
     */
    @Operation(summary = "发送注册验证码", description = "向指定手机号发送注册用短信验证码")
    @PostMapping("/sendSms")
    public Result<Void> sendSms(@RequestBody @Validated SendSmsRequest request) {
        boolean sent = smsVerificationCodeService.sendCode(request.getPhone());
        if (sent) {
            return Result.success();
        }
        return Result.error("验证码发送失败，请稍后重试");
    }

    /**
     * 忘记密码-发送重置密码验证码
     */
    @Operation(summary = "发送重置密码验证码", description = "向指定手机号发送重置密码用短信验证码")
    @PostMapping("/sendResetSms")
    public Result<Void> sendResetSms(@RequestBody @Validated SendSmsRequest request) {
        boolean sent = smsVerificationCodeService.sendCode(request.getPhone());
        if (sent) {
            return Result.success();
        }
        return Result.error("验证码发送失败，请稍后重试");
    }

    /**
     * 忘记密码-重置密码
     */
    @Operation(summary = "重置密码", description = "通过手机号+验证码重置密码，无需登录")
    @PostMapping("/resetPassword")
    public Result<Void> resetPassword(@RequestBody @Validated ResetPasswordRequest request) {
        userService.resetPassword(request);
        return Result.success();
    }

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的个人信息和角色列表，需要登录")
    @GetMapping("/info")
    public Result<UserInfoResponse> getUserInfo() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        UserInfoResponse response = userService.getUserInfo(userId);
        return Result.success(response);
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的头像和昵称，昵称每月仅可修改一次")
    @PutMapping("/update")
    public Result<Void> updateUserInfo(@RequestBody @Validated UserUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        userService.updateUserInfo(userId, request);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "已登录用户修改密码，需提供原密码和新密码")
    @PutMapping("/changePassword")
    public Result<Void> changePassword(@RequestBody @Validated ChangePasswordRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        userService.changePassword(userId, request);
        return Result.success();
    }
}
