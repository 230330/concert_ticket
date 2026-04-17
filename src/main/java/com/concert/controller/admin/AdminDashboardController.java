package com.concert.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.common.Result;
import com.concert.dto.response.DashboardRevenueResponse;
import com.concert.dto.response.DashboardSalesResponse;
import com.concert.entity.Concert;
import com.concert.entity.Order;
import com.concert.entity.OrderSeat;
import com.concert.entity.Show;
import com.concert.entity.User;
import com.concert.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端-数据看板控制器
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

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

    /**
     * 销售统计概览
     */
    @GetMapping("/sales")
    public Result<DashboardSalesResponse> salesOverview() {
        DashboardSalesResponse response = new DashboardSalesResponse();

        // 总订单数
        response.setTotalOrders(orderService.count());

        // 各状态订单数
        response.setPaidOrders(countByStatus(1));
        response.setCancelledOrders(countByStatus(2));
        response.setRefundedOrders(countByStatus(3));
        response.setCompletedOrders(countByStatus(4));

        // 总销售额（已支付 + 已完成）
        LambdaQueryWrapper<Order> revenueQuery = new LambdaQueryWrapper<>();
        revenueQuery.in(Order::getStatus, 1, 4);
        List<Order> revenueOrders = orderService.list(revenueQuery);
        BigDecimal totalRevenue = revenueOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalRevenue(totalRevenue);

        // 实际收入（已完成）
        LambdaQueryWrapper<Order> actualQuery = new LambdaQueryWrapper<>();
        actualQuery.eq(Order::getStatus, 4);
        List<Order> actualOrders = orderService.list(actualQuery);
        BigDecimal actualRevenue = actualOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setActualRevenue(actualRevenue);

        // 退款金额
        LambdaQueryWrapper<Order> refundQuery = new LambdaQueryWrapper<>();
        refundQuery.eq(Order::getStatus, 3);
        List<Order> refundOrders = orderService.list(refundQuery);
        BigDecimal refundAmount = refundOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setRefundAmount(refundAmount);

        // 总售票数
        response.setTotalTickets(orderSeatService.count());

        // 活跃演唱会数量（未开始+进行中）
        LambdaQueryWrapper<Concert> concertQuery = new LambdaQueryWrapper<>();
        concertQuery.in(Concert::getStatus, 0, 1);
        response.setActiveConcerts(concertService.count(concertQuery));

        // 活跃场次数量（未开售+售票中）
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.in(Show::getStatus, 0, 1);
        response.setActiveShows(showService.count(showQuery));

        // 注册用户数
        response.setTotalUsers(userService.count());

        return Result.success(response);
    }

    /**
     * 收入报表（按日期范围统计）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     */
    @GetMapping("/revenue")
    public Result<List<DashboardRevenueResponse>> revenueReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        List<DashboardRevenueResponse> result = new ArrayList<>();

        // 查询日期范围内已支付和已完成的订单
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.between(Order::getCreateTime, startDateTime, endDateTime)
                .in(Order::getStatus, 1, 3, 4);
        List<Order> orders = orderService.list(orderQuery);

        // 查询日期范围内的退款订单
        List<Order> refundedOrders = orders.stream()
                .filter(o -> o.getStatus() == 3)
                .collect(Collectors.toList());

        // 按日期分组统计
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            // 当日订单
            List<Order> dayOrders = orders.stream()
                    .filter(o -> o.getCreateTime() != null
                            && !o.getCreateTime().isBefore(dayStart)
                            && !o.getCreateTime().isAfter(dayEnd))
                    .collect(Collectors.toList());

            // 当日退款
            List<Order> dayRefunds = refundedOrders.stream()
                    .filter(o -> o.getCreateTime() != null
                            && !o.getCreateTime().isBefore(dayStart)
                            && !o.getCreateTime().isAfter(dayEnd))
                    .collect(Collectors.toList());

            DashboardRevenueResponse dayResp = new DashboardRevenueResponse();
            dayResp.setDate(date.toString());
            dayResp.setOrderCount((long) dayOrders.size());

            BigDecimal dayRevenue = dayOrders.stream()
                    .filter(o -> o.getStatus() == 1 || o.getStatus() == 4)
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dayResp.setRevenue(dayRevenue);

            BigDecimal dayRefundAmount = dayRefunds.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dayResp.setRefundAmount(dayRefundAmount);

            // 当日售票数
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

        return Result.success(result);
    }

    /**
     * 按状态统计订单数
     */
    private Long countByStatus(int status) {
        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<>();
        query.eq(Order::getStatus, status);
        return orderService.count(query);
    }
}
