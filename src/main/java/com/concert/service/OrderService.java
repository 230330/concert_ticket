package com.concert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.response.OrderResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Order;

/**
 * 订单服务接口
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     *
     * @param userId  用户ID
     * @param request 创建订单请求
     * @return 订单响应
     */
    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    /**
     * 支付订单
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     * @return 订单响应
     */
    OrderResponse payOrder(Long userId, Long orderId);

    /**
     * 取消订单
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     */
    void cancelOrder(Long userId, Long orderId);

    /**
     * 取消过期订单（系统调用）
     *
     * @param orderId 订单ID
     */
    void cancelExpiredOrder(Long orderId);

    /**
     * 获取订单详情
     *
     * @param orderId 订单ID
     * @return 订单响应
     */
    OrderResponse getOrderDetail(Long orderId);

    /**
     * 获取我的订单
     *
     * @param userId 用户ID
     * @param status 订单状态
     * @param page   页码
     * @param size   每页大小
     * @return 订单响应分页
     */
    PageResponse<OrderResponse> getMyOrders(Long userId, Integer status, Integer page, Integer size);
}
