package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description:    用户状态变更请求参数
 * @author: hzf
 * @date: 2026-04-17 15:30
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
