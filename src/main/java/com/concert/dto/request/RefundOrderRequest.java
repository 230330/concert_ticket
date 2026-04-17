package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description:    退款订单请求参数
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Data
public class RefundOrderRequest {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}
