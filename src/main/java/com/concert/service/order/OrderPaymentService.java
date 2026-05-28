package com.concert.service.order;

import com.concert.dto.response.OrderResponse;

/**
 * @description: 订单支付服务
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public interface OrderPaymentService {

    /**
     * 支付订单
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     * @return 订单响应
     */
    OrderResponse payOrder(Long userId, Long orderId);
}
