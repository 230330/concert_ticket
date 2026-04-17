package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 退款订单请求
 */
@Data
public class RefundOrderRequest {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}
