package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 支付订单请求
 */
@Data
public class PayOrderRequest {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}
