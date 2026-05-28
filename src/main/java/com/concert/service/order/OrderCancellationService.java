package com.concert.service.order;

import com.concert.dto.response.OrderResponse;
import com.concert.entity.Order;

import java.util.List;

/**
 * @description: 订单取消退款服务
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public interface OrderCancellationService {

    /**
     * 用户取消订单
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
     * 用户退款订单
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     * @return 订单响应
     */
    OrderResponse refundOrder(Long userId, Long orderId);

    /**
     * 管理员退款（不受退款时限限制）
     *
     * @param orderId 订单ID
     */
    void adminRefundOrder(Long orderId);

    /**
     * 批量取消过期订单
     *
     * @param orderIds 订单ID列表
     * @return 取消的订单数量
     */
    int batchCancelExpiredOrders(List<Long> orderIds);

    /**
     * 自动完成已结束场次的订单
     */
    void completeFinishedOrders();

    /**
     * 批量完成已结束场次的订单
     *
     * @param batchSize 每次完成的最大数量
     * @return 完成的订单数量
     */
    int batchCompleteFinishedOrders(int batchSize);
}
