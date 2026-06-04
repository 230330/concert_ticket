package com.concert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.dto.response.DashboardRevenueResponse;
import com.concert.dto.response.DashboardSalesResponse;
import com.concert.entity.*;
import com.concert.enums.ConcertStatus;
import com.concert.enums.OrderStatus;
import com.concert.enums.ShowStatus;
import com.concert.mapper.OrderMapper;
import com.concert.mapper.OrderSeatMapper;
import com.concert.service.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 仪表盘服务实现类（SQL聚合查询 + 缓存）
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

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderSeatMapper orderSeatMapper;

    @Override
    @Cacheable(value = "dashboard:sales", unless = "#result == null")
    public DashboardSalesResponse getSalesOverview() {
        DashboardSalesResponse response = new DashboardSalesResponse();

        // 总订单数
        response.setTotalOrders(orderService.count());

        // 各状态订单数
        response.setPaidOrders(countByStatus(OrderStatus.PAID));
        response.setCancelledOrders(countByStatus(OrderStatus.CANCELLED));
        response.setRefundedOrders(countByStatus(OrderStatus.REFUNDED));
        response.setCompletedOrders(countByStatus(OrderStatus.COMPLETED));

        // 总销售额（已支付 + 已完成）- 使用SQL聚合
        BigDecimal totalRevenue = orderMapper.sumAmountByStatuses(
                new int[]{OrderStatus.PAID_VALUE, OrderStatus.COMPLETED_VALUE});
        response.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

        // 实际收入（已完成）
        BigDecimal actualRevenue = orderMapper.sumAmountByStatuses(
                new int[]{OrderStatus.COMPLETED_VALUE});
        response.setActualRevenue(actualRevenue != null ? actualRevenue : BigDecimal.ZERO);

        // 退款金额
        BigDecimal refundAmount = orderMapper.sumAmountByStatuses(
                new int[]{OrderStatus.REFUNDED_VALUE});
        response.setRefundAmount(refundAmount != null ? refundAmount : BigDecimal.ZERO);

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
    @Cacheable(value = "dashboard:revenue", key = "#startDate + ':' + #endDate", unless = "#result == null || #result.isEmpty()")
    public List<DashboardRevenueResponse> getRevenueReport(LocalDate startDate, LocalDate endDate) {
        List<DashboardRevenueResponse> result = new ArrayList<>();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 使用SQL聚合查询替代内存过滤，提升性能
        // 查询收入数据（已支付 + 已完成）
        LambdaQueryWrapper<Order> revenueQuery = new LambdaQueryWrapper<>();
        revenueQuery.between(Order::getCreateTime, startDateTime, endDateTime)
                .in(Order::getStatus, OrderStatus.PAID, OrderStatus.COMPLETED);
        List<Order> revenueOrders = orderService.list(revenueQuery);

        // 查询退款数据
        LambdaQueryWrapper<Order> refundQuery = new LambdaQueryWrapper<>();
        refundQuery.between(Order::getCreateTime, startDateTime, endDateTime)
                .eq(Order::getStatus, OrderStatus.REFUNDED);
        List<Order> refundOrders = orderService.list(refundQuery);

        // 查询所有相关订单（用于统计订单数和售票数）
        LambdaQueryWrapper<Order> allQuery = new LambdaQueryWrapper<>();
        allQuery.between(Order::getCreateTime, startDateTime, endDateTime)
                .in(Order::getStatus, OrderStatus.PAID, OrderStatus.REFUNDED, OrderStatus.COMPLETED);
        List<Order> allOrders = orderService.list(allQuery);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            DashboardRevenueResponse dayResp = new DashboardRevenueResponse();
            dayResp.setDate(date.toString());

            // 当日所有相关订单
            List<Order> dayAllOrders = filterByTimeRange(allOrders, dayStart, dayEnd);
            dayResp.setOrderCount((long) dayAllOrders.size());

            // 当日收入
            BigDecimal dayRevenue = filterByTimeRange(revenueOrders, dayStart, dayEnd).stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dayResp.setRevenue(dayRevenue);

            // 当日退款
            BigDecimal dayRefundAmount = filterByTimeRange(refundOrders, dayStart, dayEnd).stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dayResp.setRefundAmount(dayRefundAmount);

            // 当日售票数
            List<Long> dayOrderIds = dayAllOrders.stream().map(Order::getId).collect(java.util.stream.Collectors.toList());
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

    /**
     * 按时间范围过滤订单
     */
    private List<Order> filterByTimeRange(List<Order> orders, LocalDateTime start, LocalDateTime end) {
        return orders.stream()
                .filter(o -> o.getCreateTime() != null
                        && !o.getCreateTime().isBefore(start)
                        && !o.getCreateTime().isAfter(end))
                .collect(java.util.stream.Collectors.toList());
    }

    private Long countByStatus(OrderStatus status) {
        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<>();
        query.eq(Order::getStatus, status);
        return orderService.count(query);
    }
}
