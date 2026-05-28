package com.concert.service.order;

import com.concert.dto.response.AdminOrderResponse;
import com.concert.dto.response.OrderResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Order;

import java.util.List;

/**
 * @description: 订单查询服务
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public interface OrderQueryService {

    /**
     * 获取订单详情
     *
     * @param orderId 订单ID
     * @return 订单响应
     */
    OrderResponse getOrderDetail(Long orderId);

    /**
     * 获取我的订单列表
     *
     * @param userId 用户ID
     * @param status 订单状态
     * @param page   页码
     * @param size   每页大小
     * @return 订单响应分页
     */
    PageResponse<OrderResponse> getMyOrders(Long userId, Integer status, Integer page, Integer size);

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
     * 管理端-查询订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    AdminOrderResponse getOrderDetailForAdmin(Long orderId);

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
}
