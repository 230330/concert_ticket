package com.concert.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.concert.dto.request.ChangePasswordRequest;
import com.concert.dto.request.LoginRequest;
import com.concert.dto.request.RegisterRequest;
import com.concert.dto.request.ResetPasswordRequest;
import com.concert.dto.request.UserUpdateRequest;
import com.concert.dto.response.AdminUserResponse;
import com.concert.dto.response.LoginResponse;
import com.concert.dto.response.PageResponse;
import com.concert.dto.response.UserInfoResponse;
import com.concert.entity.User;
import com.concert.enums.UserStatus;

/**
 * @description:    用户服务接口
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param request 注册请求
     */
    void register(RegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（含Token）
     */
    LoginResponse login(LoginRequest request);

    /**
     * 重置密码（忘记密码）
     *
     * @param request 重置密码请求
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * 修改密码（已登录）
     *
     * @param userId  用户ID
     * @param request 修改密码请求
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户ID
     * @return 用户信息响应
     */
    UserInfoResponse getUserInfo(Long userId);

    /**
     * 更新用户信息
     *
     * @param userId  用户ID
     * @param request 更新请求
     */
    void updateUserInfo(Long userId, UserUpdateRequest request);

    /**
     * 管理端-分页查询用户列表
     *
     * @param page   页码
     * @param size   每页条数
     * @param phone  手机号搜索
     * @param status 状态筛选
     * @return 用户分页列表
     */
    PageResponse<AdminUserResponse> listUsersForAdmin(int page, int size, String phone, Integer status);

    /**
     * 管理端-查询用户详情
     *
     * @param userId 用户ID
     * @return 用户详情响应
     */
    AdminUserResponse getUserDetailForAdmin(Long userId);

    /**
     * 管理端-更新用户状态（封禁/解封）
     *
     * @param userId 用户ID
     * @param status 目标状态
     */
    void updateUserStatus(Long userId, UserStatus status);
}
