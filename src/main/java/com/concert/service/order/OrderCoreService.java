package com.concert.service.order;

import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.response.OrderResponse;

/**
 * @description: 订单核心流程服务
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public interface OrderCoreService {

    /**
     * 创建订单
     *
     * @param userId  用户ID
     * @param request 创建订单请求
     * @return 订单响应
     */
    OrderResponse createOrder(Long userId, CreateOrderRequest request);
}
