package com.concert.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.Result;
import com.concert.dto.request.UserStatusRequest;
import com.concert.dto.response.AdminUserResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.SysRole;
import com.concert.entity.User;
import com.concert.enums.UserStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.NotFoundException;
import com.concert.service.OrderService;
import com.concert.service.SysRoleService;
import com.concert.service.UserService;
import com.concert.utils.PageUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description:    管理员用户管理
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Resource
    private UserService userService;

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private OrderService orderService;

    /**
     * 分页查询用户列表
     *
     * @param page   页码
     * @param size   每页条数
     * @param phone  手机号搜索
     * @param status 状态筛选
     */
    @GetMapping("/list")
    public Result<PageResponse<AdminUserResponse>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {

        int[] params = PageUtil.validate(page, size);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (phone != null && !phone.trim().isEmpty()) {
            queryWrapper.like(User::getPhone, phone.trim());
        }
        if (status != null) {
            queryWrapper.eq(User::getStatus, status);
        }
        queryWrapper.orderByDesc(User::getId);

        Page<User> userPage = new Page<>(params[0], params[1]);
        userService.page(userPage, queryWrapper);

        List<User> users = userPage.getRecords();
        if (users.isEmpty()) {
            return Result.success(new PageResponse<>(userPage.getCurrent(), userPage.getSize(),
                    userPage.getTotal(), Collections.emptyList()));
        }

        List<AdminUserResponse> responseList = users.stream().map(user -> {
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
            LambdaQueryWrapper<com.concert.entity.Order> orderQuery = new LambdaQueryWrapper<>();
            orderQuery.eq(com.concert.entity.Order::getUserId, user.getId());
            resp.setOrderCount(orderService.count(orderQuery));

            return resp;
        }).collect(Collectors.toList());

        PageResponse<AdminUserResponse> pageResponse = new PageResponse<>(
                userPage.getCurrent(), userPage.getSize(), userPage.getTotal(), responseList);
        return Result.success(pageResponse);
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     */
    @GetMapping("/{id}")
    public Result<AdminUserResponse> detail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

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
        LambdaQueryWrapper<com.concert.entity.Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.eq(com.concert.entity.Order::getUserId, user.getId());
        resp.setOrderCount(orderService.count(orderQuery));

        return Result.success(resp);
    }

    /**
     * 封禁用户
     */
    @PutMapping("/ban")
    public Result<Void> banUser(@RequestBody @Validated UserStatusRequest request) {
        if (request.getStatus() != UserStatus.DISABLED) {
            throw new BusinessException("封禁操作目标状态必须为禁用");
        }
        updateUserStatus(request);
        return Result.success();
    }

    /**
     * 解封用户
     */
    @PutMapping("/unban")
    public Result<Void> unbanUser(@RequestBody @Validated UserStatusRequest request) {
        if (request.getStatus() != UserStatus.NORMAL) {
            throw new BusinessException("解封操作目标状态必须为正常");
        }
        updateUserStatus(request);
        return Result.success();
    }

    /**
     * 更新用户状态
     */
    private void updateUserStatus(UserStatusRequest request) {
        User user = userService.getById(request.getUserId());
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }

        user.setStatus(request.getStatus());
        userService.updateById(user);
    }
}
