package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.response.AdminOrderResponse;
import com.concert.dto.response.OrderResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Order;
import com.concert.mapper.OrderMapper;
import com.concert.service.OrderService;
import com.concert.service.order.OrderCancellationService;
import com.concert.service.order.OrderCoreService;
import com.concert.service.order.OrderPaymentService;
import com.concert.service.order.OrderQueryService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: 订单服务实现类（门面模式，委托给子服务）
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Resource
    @Lazy
    private OrderCoreService orderCoreService;

    @Resource
    @Lazy
    private OrderPaymentService orderPaymentService;

    @Resource
    @Lazy
    private OrderCancellationService orderCancellationService;

    @Resource
    @Lazy
    private OrderQueryService orderQueryService;

    @Override
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        return orderCoreService.createOrder(userId, request);
    }

    @Override
    public OrderResponse payOrder(Long userId, Long orderId) {
        return orderPaymentService.payOrder(userId, orderId);
    }

    @Override
    public void cancelOrder(Long userId, Long orderId) {
        orderCancellationService.cancelOrder(userId, orderId);
    }

    @Override
    public void cancelExpiredOrder(Long orderId) {
        orderCancellationService.cancelExpiredOrder(orderId);
    }

    @Override
    public OrderResponse getOrderDetail(Long orderId) {
        return orderQueryService.getOrderDetail(orderId);
    }

    @Override
    public PageResponse<OrderResponse> getMyOrders(Long userId, Integer status, Integer page, Integer size) {
        return orderQueryService.getMyOrders(userId, status, page, size);
    }

    @Override
    public OrderResponse refundOrder(Long userId, Long orderId) {
        return orderCancellationService.refundOrder(userId, orderId);
    }

    @Override
    public void completeFinishedOrders() {
        orderCancellationService.completeFinishedOrders();
    }

    @Override
    public int batchCancelExpiredOrders(List<Long> orderIds) {
        return orderCancellationService.batchCancelExpiredOrders(orderIds);
    }

    @Override
    public int batchCompleteFinishedOrders(int maxCompleteBatchSize) {
        return orderCancellationService.batchCompleteFinishedOrders(maxCompleteBatchSize);
    }

    @Override
    public List<Order> getPaidOrdersWithTicketCodeByPhone(String phone) {
        return orderQueryService.getPaidOrdersWithTicketCodeByPhone(phone);
    }

    @Override
    public boolean verifyTicketCode(String ticketCode) {
        return orderQueryService.verifyTicketCode(ticketCode);
    }

    @Override
    public PageResponse<AdminOrderResponse> listOrdersForAdmin(int page, int size, Integer status, String orderNo, Long userId) {
        return orderQueryService.listOrdersForAdmin(page, size, status, orderNo, userId);
    }

    @Override
    public AdminOrderResponse getOrderDetailForAdmin(Long orderId) {
        return orderQueryService.getOrderDetailForAdmin(orderId);
    }

    @Override
    public void adminRefundOrder(Long orderId) {
        orderCancellationService.adminRefundOrder(orderId);
    }
}
