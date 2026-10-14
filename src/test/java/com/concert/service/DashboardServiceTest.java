package com.concert.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.concert.dto.response.DashboardRevenueResponse;
import com.concert.dto.response.DashboardSalesResponse;
import com.concert.entity.*;
import com.concert.enums.ConcertStatus;
import com.concert.enums.OrderStatus;
import com.concert.enums.ShowStatus;
import com.concert.mapper.OrderMapper;
import com.concert.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DashboardService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderSeatService orderSeatService;

    @Mock
    private ConcertService concertService;

    @Mock
    private ShowService showService;

    @Mock
    private UserService userService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Nested
    @DisplayName("销售统计概览 - getSalesOverview")
    class GetSalesOverview {

        @Test
        @DisplayName("正常统计数据正确")
        void testGetSalesOverview_Success() {
            // Given
            when(orderService.count()).thenReturn(100L);
            when(orderService.count(any())).thenReturn(30L); // 各状态计数
            when(orderSeatService.count()).thenReturn(200L);
            when(concertService.count(any())).thenReturn(5L);
            when(showService.count(any())).thenReturn(10L);
            when(userService.count()).thenReturn(500L);
            // 收入指标改为 SQL 聚合，按传入的状态组合区分返回值
            when(orderMapper.sumAmountByStatuses(any(int[].class))).thenAnswer(invocation -> {
                int[] statuses = invocation.getArgument(0);
                if (statuses.length == 2) {
                    return BigDecimal.valueOf(2990);   // 已支付 + 已完成
                }
                if (statuses[0] == OrderStatus.COMPLETED_VALUE) {
                    return BigDecimal.valueOf(1990);   // 已完成
                }
                return BigDecimal.valueOf(100);        // 已退款
            });

            // When
            DashboardSalesResponse response = dashboardService.getSalesOverview();

            // Then
            assertNotNull(response);
            assertEquals(100L, response.getTotalOrders());
            assertEquals(200L, response.getTotalTickets());
            assertEquals(500L, response.getTotalUsers());
            assertEquals(5L, response.getActiveConcerts());
            assertEquals(10L, response.getActiveShows());
            assertEquals(BigDecimal.valueOf(2990), response.getTotalRevenue());
            assertEquals(BigDecimal.valueOf(1990), response.getActualRevenue());
            assertEquals(BigDecimal.valueOf(100), response.getRefundAmount());
        }

        @Test
        @DisplayName("无订单时返回零值")
        void testGetSalesOverview_NoOrders() {
            // Given
            when(orderService.count()).thenReturn(0L);
            when(orderService.count(any())).thenReturn(0L);
            when(orderSeatService.count()).thenReturn(0L);
            when(concertService.count(any())).thenReturn(0L);
            when(showService.count(any())).thenReturn(0L);
            when(userService.count()).thenReturn(0L);
            // 未打桩时 mapper 返回 null，用于覆盖实现中的 null 归一化为 0 分支

            // When
            DashboardSalesResponse response = dashboardService.getSalesOverview();

            // Then
            assertNotNull(response);
            assertEquals(0L, response.getTotalOrders());
            assertEquals(BigDecimal.ZERO, response.getTotalRevenue());
            assertEquals(BigDecimal.ZERO, response.getActualRevenue());
            assertEquals(BigDecimal.ZERO, response.getRefundAmount());
        }
    }

    @Nested
    @DisplayName("收入报表 - getRevenueReport")
    class GetRevenueReport {

        @Test
        @DisplayName("单日收入报表数据正确")
        void testGetRevenueReport_SingleDay() {
            // Given
            LocalDate today = LocalDate.now();
            Order paidOrder = new Order();
            paidOrder.setId(1L);
            paidOrder.setStatus(OrderStatus.PAID);
            paidOrder.setTotalAmount(BigDecimal.valueOf(299));
            paidOrder.setCreateTime(today.atStartOfDay().plusHours(10));

            when(orderService.list(any(Wrapper.class))).thenReturn(Arrays.asList(paidOrder));
            when(orderSeatService.count(any())).thenReturn(1L);

            // When
            List<DashboardRevenueResponse> result = dashboardService.getRevenueReport(today, today);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(today.toString(), result.get(0).getDate());
            assertEquals(1L, result.get(0).getOrderCount());
            assertEquals(BigDecimal.valueOf(299), result.get(0).getRevenue());
        }

        @Test
        @DisplayName("日期范围内无订单返回零值")
        void testGetRevenueReport_NoOrdersInRange() {
            // Given
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 1, 1);
            when(orderService.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

            // When
            List<DashboardRevenueResponse> result = dashboardService.getRevenueReport(start, end);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(0L, result.get(0).getOrderCount());
            assertEquals(BigDecimal.ZERO, result.get(0).getRevenue());
            assertEquals(0L, result.get(0).getTicketCount());
        }

        @Test
        @DisplayName("退款金额计算正确")
        void testGetRevenueReport_RefundAmount() {
            // Given
            LocalDate today = LocalDate.now();
            Order refundOrder = new Order();
            refundOrder.setId(1L);
            refundOrder.setStatus(OrderStatus.REFUNDED);
            refundOrder.setTotalAmount(BigDecimal.valueOf(100));
            refundOrder.setCreateTime(today.atStartOfDay().plusHours(10));

            // getRevenueReport 依次查询收入、退款、当日订单三组数据，mock 按调用顺序返回
            when(orderService.list(any(Wrapper.class)))
                    .thenReturn(Collections.emptyList())
                    .thenReturn(Arrays.asList(refundOrder));

            // When
            List<DashboardRevenueResponse> result = dashboardService.getRevenueReport(today, today);

            // Then
            assertEquals(BigDecimal.valueOf(100), result.get(0).getRefundAmount());
            assertEquals(BigDecimal.ZERO, result.get(0).getRevenue()); // 退款订单不计入收入
        }
    }

}
