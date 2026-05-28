package com.concert.controller.admin;

import com.concert.common.Result;
import com.concert.dto.request.UserStatusRequest;
import com.concert.dto.response.AdminUserResponse;
import com.concert.dto.response.PageResponse;
import com.concert.enums.UserStatus;
import com.concert.exception.BusinessException;
import com.concert.service.UserService;
import com.concert.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description:    管理员用户管理
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Tag(name = "管理端-用户管理", description = "管理员用户列表、详情、封禁/解封等接口")
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Resource
    private UserService userService;

    /**
     * 分页查询用户列表
     */
    @Operation(summary = "分页查询用户列表", description = "管理员分页查询用户列表，支持手机号和状态筛选")
    @GetMapping("/list")
    public Result<PageResponse<AdminUserResponse>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {

        int[] params = PageUtil.validate(page, size);
        PageResponse<AdminUserResponse> pageResponse = userService.listUsersForAdmin(params[0], params[1], phone, status);
        return Result.success(pageResponse);
    }

    /**
     * 查询用户详情
     */
    @Operation(summary = "查询用户详情", description = "管理员查询指定用户的详细信息，含角色和订单统计")
    @GetMapping("/{id}")
    public Result<AdminUserResponse> detail(@PathVariable Long id) {
        AdminUserResponse resp = userService.getUserDetailForAdmin(id);
        return Result.success(resp);
    }

    /**
     * 封禁用户
     */
    @Operation(summary = "封禁用户", description = "将用户状态设为禁用，用户将无法登录")
    @PutMapping("/ban")
    public Result<Void> banUser(@RequestBody @Validated UserStatusRequest request) {
        if (request.getStatus() != UserStatus.DISABLED) {
            throw new BusinessException("封禁操作目标状态必须为禁用");
        }
        userService.updateUserStatus(request.getUserId(), request.getStatus());
        return Result.success();
    }

    /**
     * 解封用户
     */
    @Operation(summary = "解封用户", description = "将用户状态恢复为正常，用户可以正常登录")
    @PutMapping("/unban")
    public Result<Void> unbanUser(@RequestBody @Validated UserStatusRequest request) {
        if (request.getStatus() != UserStatus.NORMAL) {
            throw new BusinessException("解封操作目标状态必须为正常");
        }
        userService.updateUserStatus(request.getUserId(), request.getStatus());
        return Result.success();
    }
}
