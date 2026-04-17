package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 用户状态变更请求（封禁/解封）
 */
@Data
public class UserStatusRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 目标状态：0-封禁，1-解封
     */
    @NotNull(message = "目标状态不能为空")
    private Integer status;

    /**
     * 操作原因
     */
    private String reason;
}
