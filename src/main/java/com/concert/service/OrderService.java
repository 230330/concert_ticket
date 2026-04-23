package com.concert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.response.OrderResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Order;

import java.util.List;

/**
 * @description:    订单服务接口
 * @author: hzf
 * @date: 2026-04-17 15:30
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

    /**
     * 退款订单
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     * @return 订单响应
     */
    OrderResponse refundOrder(Long userId, Long orderId);

    /**
     * 自动完成已结束场次的订单（系统定时调用）
     * 将演出时间已过且状态为"已支付"的订单标记为"已完成"
     */
    void completeFinishedOrders();

    /**
     * 批量取消过期订单（系统调用）
     *
     * @param orderIds 订单ID列表
     * @return 取消的订单数量
     */
    int batchCancelExpiredOrders(List<Long> orderIds);

    /**
     * 批量完成已结束场次的订单（系统调用）
     *
     * @param maxCompleteBatchSize 每次完成的最大订单数量
     * @return 完成的订单数量
     */
    int batchCompleteFinishedOrders(int maxCompleteBatchSize);
}
