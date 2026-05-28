package com.concert.service;

import com.concert.dto.response.DashboardRevenueResponse;
import com.concert.dto.response.DashboardSalesResponse;
import com.concert.entity.*;
import com.concert.enums.ConcertStatus;
import com.concert.enums.OrderStatus;
import com.concert.enums.ShowStatus;
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
            when(orderService.list(any())).thenReturn(createPaidOrders());
            when(orderSeatService.count()).thenReturn(200L);
            when(concertService.count(any())).thenReturn(5L);
            when(showService.count(any())).thenReturn(10L);
            when(userService.count()).thenReturn(500L);

            // When
            DashboardSalesResponse response = dashboardService.getSalesOverview();

            // Then
            assertNotNull(response);
            assertEquals(100L, response.getTotalOrders());
            assertEquals(200L, response.getTotalTickets());
            assertEquals(500L, response.getTotalUsers());
            assertEquals(5L, response.getActiveConcerts());
            assertEquals(10L, response.getActiveShows());
        }

        @Test
        @DisplayName("无订单时返回零值")
        void testGetSalesOverview_NoOrders() {
            // Given
            when(orderService.count()).thenReturn(0L);
            when(orderService.count(any())).thenReturn(0L);
            when(orderService.list(any())).thenReturn(Collections.emptyList());
            when(orderSeatService.count()).thenReturn(0L);
            when(concertService.count(any())).thenReturn(0L);
            when(showService.count(any())).thenReturn(0L);
            when(userService.count()).thenReturn(0L);

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

            when(orderService.list(any())).thenReturn(Arrays.asList(paidOrder));
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
            when(orderService.list(any())).thenReturn(Collections.emptyList());

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

            when(orderService.list(any())).thenReturn(Arrays.asList(refundOrder));

            // When
            List<DashboardRevenueResponse> result = dashboardService.getRevenueReport(today, today);

            // Then
            assertEquals(BigDecimal.valueOf(100), result.get(0).getRefundAmount());
            assertEquals(BigDecimal.ZERO, result.get(0).getRevenue()); // 退款订单不计入收入
        }
    }

    private List<Order> createPaidOrders() {
        Order o1 = new Order();
        o1.setId(1L);
        o1.setStatus(OrderStatus.PAID);
        o1.setTotalAmount(BigDecimal.valueOf(299));

        Order o2 = new Order();
        o2.setId(2L);
        o2.setStatus(OrderStatus.COMPLETED);
        o2.setTotalAmount(BigDecimal.valueOf(599));

        return Arrays.asList(o1, o2);
    }
}
