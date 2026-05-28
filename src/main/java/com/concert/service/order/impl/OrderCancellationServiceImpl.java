package com.concert.service.order.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.concert.dto.response.OrderResponse;
import com.concert.entity.*;
import com.concert.enums.OrderStatus;
import com.concert.enums.ShowStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.ForbiddenException;
import com.concert.exception.NotFoundException;
import com.concert.mapper.OrderMapper;
import com.concert.mapper.OrderSeatMapper;
import com.concert.mapper.TicketTypeMapper;
import com.concert.service.*;
import com.concert.service.order.OrderCancellationService;
import com.concert.service.order.OrderQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description: 订单取消退款服务实现
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Service
public class OrderCancellationServiceImpl implements OrderCancellationService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCancellationServiceImpl.class);

    @Value("${concert.order.refund-before-hours:48}")
    private int refundBeforeHours;

    @Resource
    private OrderService orderService;

    @Resource
    private OrderSeatService orderSeatService;

    @Resource
    private TicketTypeService ticketTypeService;

    @Resource
    private ShowService showService;

    @Resource
    private OrderQueryService orderQueryService;

    @Resource
    private OrderSeatMapper orderSeatMapper;

    @Resource
    private TicketTypeMapper ticketTypeMapper;

    @Resource
    private OrderMapper orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作此订单");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("订单状态异常，无法取消");
        }

        doCancel(order);
        logger.info("订单取消成功，订单号：{}，用户ID：{}", order.getOrderNo(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredOrder(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        doCancel(order);
        logger.info("过期订单自动取消，订单号：{}", order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse refundOrder(Long userId, Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }

        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作此订单");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException("订单状态异常，无法退款");
        }

        // 校验退款时限
        Show show = showService.getById(order.getShowId());
        if (show == null) {
            throw new NotFoundException("关联场次不存在");
        }

        LocalDateTime refundDeadline = show.getShowTime().minusHours(refundBeforeHours);
        if (LocalDateTime.now().isAfter(refundDeadline)) {
            throw new BusinessException("已超过退款截止时间（演出前" + refundBeforeHours + "小时），无法退款");
        }

        doRefund(order);

        logger.info("订单退款成功，订单号：{}，用户ID：{}，退款金额：{}", order.getOrderNo(), userId, order.getTotalAmount());
        return orderQueryService.getOrderDetail(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminRefundOrder(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }

        // 只有已支付或已完成的订单可以退款
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("订单状态异常，无法退款");
        }

        doRefund(order);
        logger.info("管理员退款成功，订单号：{}", order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCancelExpiredOrders(List<Long> orderIds) {
        if (CollectionUtil.isEmpty(orderIds)) {
            return 0;
        }

        List<Order> orders = orderService.list(new LambdaQueryWrapper<Order>()
                .in(Order::getId, orderIds)
                .eq(Order::getStatus, OrderStatus.PENDING)
                .lt(Order::getExpireTime, LocalDateTime.now()));

        if (orders.isEmpty()) {
            return 0;
        }

        List<Long> validOrderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        // 删除座位记录
        int deletedSeats = orderSeatMapper.delete(new LambdaQueryWrapper<OrderSeat>()
                .in(OrderSeat::getOrderId, validOrderIds));
        logger.debug("批量取消订单：删除 order_seat 记录 {} 条", deletedSeats);

        // 回滚库存
        Map<Long, Integer> rollbackMap = new HashMap<>();
        for (Order order : orders) {
            int seatCount = order.getSeatInfo().split(",").length;
            rollbackMap.merge(order.getTicketTypeId(), seatCount, Integer::sum);
        }
        for (Map.Entry<Long, Integer> entry : rollbackMap.entrySet()) {
            ticketTypeMapper.updateSoldStockDecrement(entry.getKey(), entry.getValue());
        }

        // 更新订单状态
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .in(Order::getId, validOrderIds)
                .set(Order::getStatus, OrderStatus.CANCELLED)
                .set(Order::getUpdateTime, LocalDateTime.now()));

        logger.info("批量取消订单完成，成功取消 {} 个订单，回滚座位 {} 个", updated, deletedSeats);
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeFinishedOrders() {
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.lt(Show::getShowTime, LocalDateTime.now())
                .ne(Show::getStatus, ShowStatus.CANCELLED);
        List<Show> finishedShows = showService.list(showQuery);

        if (finishedShows.isEmpty()) {
            return;
        }

        List<Long> finishedShowIds = finishedShows.stream()
                .map(Show::getId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.in(Order::getShowId, finishedShowIds)
                .eq(Order::getStatus, OrderStatus.PAID);
        List<Order> ordersToComplete = orderService.list(orderQuery);

        if (ordersToComplete.isEmpty()) {
            return;
        }

        logger.info("发现 {} 个已结束场次的已支付订单，开始标记为已完成...", ordersToComplete.size());

        int successCount = 0;
        for (Order order : ordersToComplete) {
            try {
                order.setStatus(OrderStatus.COMPLETED);
                orderService.updateById(order);
                successCount++;
            } catch (Exception e) {
                logger.error("标记订单为已完成失败，订单ID：{}，错误：{}", order.getId(), e.getMessage());
            }
        }

        for (Show show : finishedShows) {
            if (show.getStatus() != ShowStatus.ENDED) {
                show.setStatus(ShowStatus.ENDED);
                showService.updateById(show);
            }
        }

        logger.info("订单自动完成处理完毕，成功：{}/{}", successCount, ordersToComplete.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCompleteFinishedOrders(int batchSize) {
        List<Long> orderIds = orderMapper.selectNeedCompleteOrderIds(batchSize);
        if (orderIds.isEmpty()) {
            return 0;
        }

        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .in(Order::getId, orderIds)
                .set(Order::getStatus, OrderStatus.COMPLETED)
                .set(Order::getUpdateTime, LocalDateTime.now()));
        logger.info("批量自动完成订单 {} 个", updated);
        return updated;
    }

    /**
     * 执行取消订单逻辑：回滚库存 + 释放座位 + 更新状态
     */
    private void doCancel(Order order) {
        LambdaQueryWrapper<OrderSeat> osQuery = new LambdaQueryWrapper<>();
        osQuery.eq(OrderSeat::getOrderId, order.getId());
        List<OrderSeat> orderSeats = orderSeatService.list(osQuery);

        if (!orderSeats.isEmpty()) {
            Map<Long, Long> ticketTypeCountMap = orderSeats.stream()
                    .collect(Collectors.groupingBy(OrderSeat::getTicketTypeId, Collectors.counting()));

            for (Map.Entry<Long, Long> entry : ticketTypeCountMap.entrySet()) {
                LambdaUpdateWrapper<TicketType> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(TicketType::getId, entry.getKey())
                        .setSql("available_stock = available_stock + " + entry.getValue());
                ticketTypeService.update(updateWrapper);
            }

            LambdaQueryWrapper<OrderSeat> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(OrderSeat::getOrderId, order.getId());
            orderSeatService.remove(deleteWrapper);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderService.updateById(order);
    }

    /**
     * 执行退款逻辑：回滚库存 + 释放座位 + 更新状态
     */
    private void doRefund(Order order) {
        LambdaQueryWrapper<OrderSeat> osQuery = new LambdaQueryWrapper<>();
        osQuery.eq(OrderSeat::getOrderId, order.getId());
        List<OrderSeat> orderSeats = orderSeatService.list(osQuery);

        if (!orderSeats.isEmpty()) {
            Map<Long, Long> ticketTypeCountMap = orderSeats.stream()
                    .collect(Collectors.groupingBy(OrderSeat::getTicketTypeId, Collectors.counting()));

            for (Map.Entry<Long, Long> entry : ticketTypeCountMap.entrySet()) {
                LambdaUpdateWrapper<TicketType> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(TicketType::getId, entry.getKey())
                        .setSql("available_stock = available_stock + " + entry.getValue());
                ticketTypeService.update(updateWrapper);
                logger.info("退款释放座位，订单号：{}，票档数量明细：{}", order.getOrderNo(), ticketTypeCountMap);
            }

            LambdaQueryWrapper<OrderSeat> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(OrderSeat::getOrderId, order.getId());
            orderSeatService.remove(deleteWrapper);
        }

        order.setStatus(OrderStatus.REFUNDED);
        orderService.updateById(order);
    }
}
