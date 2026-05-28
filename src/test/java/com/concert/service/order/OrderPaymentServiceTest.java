package com.concert.service.order;

import com.concert.dto.response.OrderResponse;
import com.concert.entity.Order;
import com.concert.entity.User;
import com.concert.enums.OrderStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.ForbiddenException;
import com.concert.exception.NotFoundException;
import com.concert.service.*;
import com.concert.service.order.impl.OrderPaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderPaymentService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private OrderQueryService orderQueryService;

    @InjectMocks
    private OrderPaymentServiceImpl orderPaymentService;

    private Order pendingOrder;
    private User testUser;

    @BeforeEach
    void setUp() {
        pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setUserId(100L);
        pendingOrder.setOrderNo("ORD20260528001");
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setExpireTime(LocalDateTime.now().plusMinutes(15));
        pendingOrder.setTotalAmount(java.math.BigDecimal.valueOf(299));

        testUser = new User();
        testUser.setId(100L);
        testUser.setPhone("13800138000");
    }

    @Nested
    @DisplayName("支付订单 - 正常流程")
    class PayOrderNormal {

        @Test
        @DisplayName("支付待支付订单 - 验证状态更新和取票码生成")
        void testPayOrder_VerifyStatusUpdate() {
            // Given - 模拟到事务同步之前的部分
            when(orderService.getById(1L)).thenReturn(pendingOrder);
            when(userService.getById(100L)).thenReturn(testUser);
            when(orderService.updateById(any(Order.class))).thenReturn(true);

            // When & Then - payOrder 会因 TransactionSynchronizationManager 抛出异常
            // 但我们可以验证在异常之前 orderService.updateById 被正确调用
            // 注意：完整的支付流程需要 Spring 事务上下文，此处仅验证状态更新逻辑
            try {
                orderPaymentService.payOrder(100L, 1L);
            } catch (IllegalStateException e) {
                // TransactionSynchronizationManager 未激活，预期行为
            }

            // 验证订单状态已更新为 PAID
            verify(orderService).updateById(argThat(order ->
                    order.getStatus() == OrderStatus.PAID &&
                    order.getPayTime() != null &&
                    order.getPickupCode() != null
            ));
        }
    }

    @Nested
    @DisplayName("支付订单 - 异常场景")
    class PayOrderException {

        @Test
        @DisplayName("订单不存在抛出NotFoundException")
        void testPayOrder_OrderNotFound_ThrowsException() {
            // Given
            when(orderService.getById(999L)).thenReturn(null);

            // When & Then
            assertThrows(NotFoundException.class, () -> orderPaymentService.payOrder(100L, 999L));
        }

        @Test
        @DisplayName("非本人订单抛出ForbiddenException")
        void testPayOrder_NotOwner_ThrowsException() {
            // Given
            when(orderService.getById(1L)).thenReturn(pendingOrder);

            // When & Then
            assertThrows(ForbiddenException.class, () -> orderPaymentService.payOrder(200L, 1L));
        }

        @Test
        @DisplayName("订单非待支付状态抛出BusinessException")
        void testPayOrder_NotPending_ThrowsException() {
            // Given
            pendingOrder.setStatus(OrderStatus.PAID);
            when(orderService.getById(1L)).thenReturn(pendingOrder);

            // When & Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderPaymentService.payOrder(100L, 1L));
            assertTrue(ex.getMessage().contains("订单状态异常"));
        }

        @Test
        @DisplayName("订单已过期抛出BusinessException")
        void testPayOrder_Expired_ThrowsException() {
            // Given
            pendingOrder.setExpireTime(LocalDateTime.now().minusMinutes(1));
            when(orderService.getById(1L)).thenReturn(pendingOrder);

            // When & Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderPaymentService.payOrder(100L, 1L));
            assertTrue(ex.getMessage().contains("已过期"));
        }
    }
}
