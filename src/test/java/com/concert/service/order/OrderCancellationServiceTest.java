package com.concert.service.order;

import com.concert.dto.response.OrderResponse;
import com.concert.entity.Order;
import com.concert.entity.OrderSeat;
import com.concert.entity.Show;
import com.concert.enums.OrderStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.ForbiddenException;
import com.concert.exception.NotFoundException;
import com.concert.mapper.OrderMapper;
import com.concert.mapper.OrderSeatMapper;
import com.concert.mapper.TicketTypeMapper;
import com.concert.service.*;
import com.concert.service.order.impl.OrderCancellationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderCancellationService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderSeatService orderSeatService;

    @Mock
    private TicketTypeService ticketTypeService;

    @Mock
    private ShowService showService;

    @Mock
    private OrderQueryService orderQueryService;

    @Mock
    private OrderSeatMapper orderSeatMapper;

    @Mock
    private TicketTypeMapper ticketTypeMapper;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderCancellationServiceImpl orderCancellationService;

    private Order pendingOrder;
    private Order paidOrder;
    private Show futureShow;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderCancellationService, "refundBeforeHours", 48);

        pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setUserId(100L);
        pendingOrder.setOrderNo("ORD20260528001");
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setShowId(10L);
        pendingOrder.setTotalAmount(BigDecimal.valueOf(299));
        pendingOrder.setSeatInfo("1");

        paidOrder = new Order();
        paidOrder.setId(2L);
        paidOrder.setUserId(100L);
        paidOrder.setOrderNo("ORD20260528002");
        paidOrder.setStatus(OrderStatus.PAID);
        paidOrder.setShowId(10L);
        paidOrder.setTotalAmount(BigDecimal.valueOf(599));
        paidOrder.setSeatInfo("2,3");

        futureShow = new Show();
        futureShow.setId(10L);
        futureShow.setShowTime(LocalDateTime.now().plusDays(7));
    }

    // ==================== 取消订单 ====================

    @Nested
    @DisplayName("取消订单 - cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("取消待支付订单成功")
        void testCancelOrder_Success() {
            // Given
            when(orderService.getById(1L)).thenReturn(pendingOrder);
            when(orderSeatService.list(any())).thenReturn(Collections.emptyList());
            when(orderService.updateById(any(Order.class))).thenReturn(true);

            // When
            orderCancellationService.cancelOrder(100L, 1L);

            // Then
            verify(orderService).updateById(argThat(order ->
                    order.getStatus() == OrderStatus.CANCELLED
            ));
        }

        @Test
        @DisplayName("订单不存在抛出NotFoundException")
        void testCancelOrder_NotFound_ThrowsException() {
            when(orderService.getById(999L)).thenReturn(null);
            assertThrows(NotFoundException.class, () -> orderCancellationService.cancelOrder(100L, 999L));
        }

        @Test
        @DisplayName("非本人订单抛出ForbiddenException")
        void testCancelOrder_NotOwner_ThrowsException() {
            when(orderService.getById(1L)).thenReturn(pendingOrder);
            assertThrows(ForbiddenException.class, () -> orderCancellationService.cancelOrder(200L, 1L));
        }

        @Test
        @DisplayName("订单非待支付状态抛出BusinessException")
        void testCancelOrder_NotPending_ThrowsException() {
            paidOrder.setId(1L);
            when(orderService.getById(1L)).thenReturn(paidOrder);
            assertThrows(BusinessException.class, () -> orderCancellationService.cancelOrder(100L, 1L));
        }

        @Test
        @DisplayName("取消订单时回滚库存和释放座位")
        void testCancelOrder_RollbackStockAndSeats() {
            // Given
            OrderSeat os1 = new OrderSeat();
            os1.setTicketTypeId(1L);
            OrderSeat os2 = new OrderSeat();
            os2.setTicketTypeId(1L);

            when(orderService.getById(1L)).thenReturn(pendingOrder);
            when(orderSeatService.list(any())).thenReturn(Arrays.asList(os1, os2));
            when(ticketTypeService.update(any())).thenReturn(true);
            when(orderSeatService.remove(any())).thenReturn(true);
            when(orderService.updateById(any(Order.class))).thenReturn(true);

            // When
            orderCancellationService.cancelOrder(100L, 1L);

            // Then
            verify(ticketTypeService).update(any());
            verify(orderSeatService).remove(any());
            verify(orderService).updateById(argThat(order ->
                    order.getStatus() == OrderStatus.CANCELLED
            ));
        }
    }

    // ==================== 取消过期订单 ====================

    @Nested
    @DisplayName("取消过期订单 - cancelExpiredOrder")
    class CancelExpiredOrder {

        @Test
        @DisplayName("过期待支付订单被成功取消")
        void testCancelExpiredOrder_Success() {
            when(orderService.getById(1L)).thenReturn(pendingOrder);
            when(orderSeatService.list(any())).thenReturn(Collections.emptyList());
            when(orderService.updateById(any(Order.class))).thenReturn(true);

            orderCancellationService.cancelExpiredOrder(1L);

            verify(orderService).updateById(argThat(order ->
                    order.getStatus() == OrderStatus.CANCELLED
            ));
        }

        @Test
        @DisplayName("非待支付订单不做处理")
        void testCancelExpiredOrder_NotPending_Skip() {
            paidOrder.setId(1L);
            when(orderService.getById(1L)).thenReturn(paidOrder);

            orderCancellationService.cancelExpiredOrder(1L);

            verify(orderService, never()).updateById(any());
        }

        @Test
        @DisplayName("订单不存在不做处理")
        void testCancelExpiredOrder_NotFound_Skip() {
            when(orderService.getById(999L)).thenReturn(null);
            orderCancellationService.cancelExpiredOrder(999L);
            verify(orderService, never()).updateById(any());
        }
    }

    // ==================== 退款订单 ====================

    @Nested
    @DisplayName("退款订单 - refundOrder")
    class RefundOrder {

        @Test
        @DisplayName("退款已支付订单成功")
        void testRefundOrder_Success() {
            when(orderService.getById(2L)).thenReturn(paidOrder);
            when(showService.getById(10L)).thenReturn(futureShow);
            when(orderSeatService.list(any())).thenReturn(Collections.emptyList());
            when(orderService.updateById(any(Order.class))).thenReturn(true);
            when(orderQueryService.getOrderDetail(2L)).thenReturn(new OrderResponse());

            OrderResponse response = orderCancellationService.refundOrder(100L, 2L);

            assertNotNull(response);
            verify(orderService).updateById(argThat(order ->
                    order.getStatus() == OrderStatus.REFUNDED
            ));
        }

        @Test
        @DisplayName("非本人订单抛出ForbiddenException")
        void testRefundOrder_NotOwner_ThrowsException() {
            when(orderService.getById(2L)).thenReturn(paidOrder);
            assertThrows(ForbiddenException.class, () -> orderCancellationService.refundOrder(200L, 2L));
        }

        @Test
        @DisplayName("订单非已支付状态抛出BusinessException")
        void testRefundOrder_NotPaid_ThrowsException() {
            pendingOrder.setId(2L);
            when(orderService.getById(2L)).thenReturn(pendingOrder);
            assertThrows(BusinessException.class, () -> orderCancellationService.refundOrder(100L, 2L));
        }

        @Test
        @DisplayName("超过退款截止时间抛出BusinessException")
        void testRefundOrder_PastDeadline_ThrowsException() {
            // 演出时间距现在不到48小时
            futureShow.setShowTime(LocalDateTime.now().plusHours(24));
            when(orderService.getById(2L)).thenReturn(paidOrder);
            when(showService.getById(10L)).thenReturn(futureShow);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderCancellationService.refundOrder(100L, 2L));
            assertTrue(ex.getMessage().contains("退款截止时间"));
        }

        @Test
        @DisplayName("关联场次不存在抛出NotFoundException")
        void testRefundOrder_ShowNotFound_ThrowsException() {
            when(orderService.getById(2L)).thenReturn(paidOrder);
            when(showService.getById(10L)).thenReturn(null);

            assertThrows(NotFoundException.class, () -> orderCancellationService.refundOrder(100L, 2L));
        }
    }

    // ==================== 管理员退款 ====================

    @Nested
    @DisplayName("管理员退款 - adminRefundOrder")
    class AdminRefundOrder {

        @Test
        @DisplayName("管理员退款已支付订单成功")
        void testAdminRefund_PaidOrder_Success() {
            when(orderService.getById(2L)).thenReturn(paidOrder);
            when(orderSeatService.list(any())).thenReturn(Collections.emptyList());
            when(orderService.updateById(any(Order.class))).thenReturn(true);

            orderCancellationService.adminRefundOrder(2L);

            verify(orderService).updateById(argThat(order ->
                    order.getStatus() == OrderStatus.REFUNDED
            ));
        }

        @Test
        @DisplayName("管理员退款已完成订单成功")
        void testAdminRefund_CompletedOrder_Success() {
            Order completedOrder = new Order();
            completedOrder.setId(3L);
            completedOrder.setStatus(OrderStatus.COMPLETED);
            completedOrder.setShowId(10L);
            completedOrder.setSeatInfo("1");

            when(orderService.getById(3L)).thenReturn(completedOrder);
            when(orderSeatService.list(any())).thenReturn(Collections.emptyList());
            when(orderService.updateById(any(Order.class))).thenReturn(true);

            orderCancellationService.adminRefundOrder(3L);

            verify(orderService).updateById(argThat(order ->
                    order.getStatus() == OrderStatus.REFUNDED
            ));
        }

        @Test
        @DisplayName("订单不存在抛出NotFoundException")
        void testAdminRefund_NotFound_ThrowsException() {
            when(orderService.getById(999L)).thenReturn(null);
            assertThrows(NotFoundException.class, () -> orderCancellationService.adminRefundOrder(999L));
        }

        @Test
        @DisplayName("待支付订单不可退款抛出BusinessException")
        void testAdminRefund_PendingOrder_ThrowsException() {
            pendingOrder.setId(2L);
            when(orderService.getById(2L)).thenReturn(pendingOrder);
            assertThrows(BusinessException.class, () -> orderCancellationService.adminRefundOrder(2L));
        }
    }

    // ==================== 批量取消过期订单 ====================

    @Nested
    @DisplayName("批量取消过期订单 - batchCancelExpiredOrders")
    class BatchCancelExpiredOrders {

        @Test
        @DisplayName("空列表直接返回0")
        void testBatchCancel_EmptyList_ReturnsZero() {
            int result = orderCancellationService.batchCancelExpiredOrders(Collections.emptyList());
            assertEquals(0, result);
        }

        @Test
        @DisplayName("无符合条件订单返回0")
        void testBatchCancel_NoMatchingOrders_ReturnsZero() {
            when(orderService.list(any())).thenReturn(Collections.emptyList());

            int result = orderCancellationService.batchCancelExpiredOrders(Arrays.asList(1L, 2L));
            assertEquals(0, result);
        }
    }
}
