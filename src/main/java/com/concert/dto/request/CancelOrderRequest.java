package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 取消订单请求
 */
@Data
public class CancelOrderRequest {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}
