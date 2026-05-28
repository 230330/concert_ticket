package com.concert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.dto.response.DashboardRevenueResponse;
import com.concert.dto.response.DashboardSalesResponse;
import com.concert.entity.*;
import com.concert.enums.ConcertStatus;
import com.concert.enums.OrderStatus;
import com.concert.enums.ShowStatus;
import com.concert.service.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: 仪表盘服务实现类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private OrderService orderService;

    @Resource
    private OrderSeatService orderSeatService;

    @Resource
    private ConcertService concertService;

    @Resource
    private ShowService showService;

    @Resource
    private UserService userService;

    @Override
    public DashboardSalesResponse getSalesOverview() {
        DashboardSalesResponse response = new DashboardSalesResponse();

        // 总订单数
        response.setTotalOrders(orderService.count());

        // 各状态订单数
        response.setPaidOrders(countByStatus(OrderStatus.PAID));
        response.setCancelledOrders(countByStatus(OrderStatus.CANCELLED));
        response.setRefundedOrders(countByStatus(OrderStatus.REFUNDED));
        response.setCompletedOrders(countByStatus(OrderStatus.COMPLETED));

        // 总销售额（已支付 + 已完成）
        LambdaQueryWrapper<Order> revenueQuery = new LambdaQueryWrapper<>();
        revenueQuery.in(Order::getStatus, OrderStatus.PAID, OrderStatus.COMPLETED);
        List<Order> revenueOrders = orderService.list(revenueQuery);
        BigDecimal totalRevenue = revenueOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalRevenue(totalRevenue);

        // 实际收入（已完成）
        LambdaQueryWrapper<Order> actualQuery = new LambdaQueryWrapper<>();
        actualQuery.eq(Order::getStatus, OrderStatus.COMPLETED);
        List<Order> actualOrders = orderService.list(actualQuery);
        BigDecimal actualRevenue = actualOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setActualRevenue(actualRevenue);

        // 退款金额
        LambdaQueryWrapper<Order> refundQuery = new LambdaQueryWrapper<>();
        refundQuery.eq(Order::getStatus, OrderStatus.REFUNDED);
        List<Order> refundOrders = orderService.list(refundQuery);
        BigDecimal refundAmount = refundOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setRefundAmount(refundAmount);

        // 总售票数
        response.setTotalTickets(orderSeatService.count());

        // 活跃演唱会数量
        LambdaQueryWrapper<Concert> concertQuery = new LambdaQueryWrapper<>();
        concertQuery.in(Concert::getStatus, ConcertStatus.NOT_STARTED, ConcertStatus.IN_PROGRESS);
        response.setActiveConcerts(concertService.count(concertQuery));

        // 活跃场次数量
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.in(Show::getStatus, ShowStatus.NOT_ON_SALE, ShowStatus.ON_SALE);
        response.setActiveShows(showService.count(showQuery));

        // 注册用户数
        response.setTotalUsers(userService.count());

        return response;
    }

    @Override
    public List<DashboardRevenueResponse> getRevenueReport(LocalDate startDate, LocalDate endDate) {
        List<DashboardRevenueResponse> result = new ArrayList<>();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.between(Order::getCreateTime, startDateTime, endDateTime)
                .in(Order::getStatus, OrderStatus.PAID, OrderStatus.REFUNDED, OrderStatus.COMPLETED);
        List<Order> orders = orderService.list(orderQuery);

        List<Order> refundedOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.REFUNDED)
                .collect(Collectors.toList());

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            List<Order> dayOrders = orders.stream()
                    .filter(o -> o.getCreateTime() != null
                            && !o.getCreateTime().isBefore(dayStart)
                            && !o.getCreateTime().isAfter(dayEnd))
                    .collect(Collectors.toList());

            List<Order> dayRefunds = refundedOrders.stream()
                    .filter(o -> o.getCreateTime() != null
                            && !o.getCreateTime().isBefore(dayStart)
                            && !o.getCreateTime().isAfter(dayEnd))
                    .collect(Collectors.toList());

            DashboardRevenueResponse dayResp = new DashboardRevenueResponse();
            dayResp.setDate(date.toString());
            dayResp.setOrderCount((long) dayOrders.size());

            BigDecimal dayRevenue = dayOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.COMPLETED)
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dayResp.setRevenue(dayRevenue);

            BigDecimal dayRefundAmount = dayRefunds.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dayResp.setRefundAmount(dayRefundAmount);

            List<Long> dayOrderIds = dayOrders.stream().map(Order::getId).collect(Collectors.toList());
            if (!dayOrderIds.isEmpty()) {
                LambdaQueryWrapper<OrderSeat> osQuery = new LambdaQueryWrapper<>();
                osQuery.in(OrderSeat::getOrderId, dayOrderIds);
                dayResp.setTicketCount(orderSeatService.count(osQuery));
            } else {
                dayResp.setTicketCount(0L);
            }

            result.add(dayResp);
        }

        return result;
    }

    private Long countByStatus(OrderStatus status) {
        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<>();
        query.eq(Order::getStatus, status);
        return orderService.count(query);
    }
}
