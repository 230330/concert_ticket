package com.concert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.response.AdminOrderResponse;
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

    /**
     * 根据手机号查询已支付且有取票码的订单
     *
     * @param phone 手机号
     * @return 订单列表
     */
    List<Order> getPaidOrdersWithTicketCodeByPhone(String phone);

    /**
     * 核销取票码
     *
     * @param ticketCode 取票码
     * @return 是否核销成功
     */
    boolean verifyTicketCode(String ticketCode);

    // ==================== 管理端方法 ====================

    /**
     * 管理端-分页查询订单列表
     *
     * @param page    页码
     * @param size    每页条数
     * @param status  订单状态筛选
     * @param orderNo 订单编号搜索
     * @param userId  用户ID筛选
     * @return 订单分页列表
     */
    PageResponse<AdminOrderResponse> listOrdersForAdmin(int page, int size, Integer status, String orderNo, Long userId);

    /**
     * 管理端-查询订单详情（含用户信息和座位详情）
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    AdminOrderResponse getOrderDetailForAdmin(Long orderId);

    /**
     * 管理端-退款（不受退款时限限制）
     *
     * @param orderId 订单ID
     */
    void adminRefundOrder(Long orderId);
}
