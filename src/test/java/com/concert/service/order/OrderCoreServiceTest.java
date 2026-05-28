package com.concert.service.order;

import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.response.OrderResponse;
import com.concert.entity.*;
import com.concert.enums.OrderStatus;
import com.concert.enums.ShowStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.NotFoundException;
import com.concert.service.*;
import com.concert.service.order.impl.OrderCoreServiceImpl;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderCoreService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrderCoreServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ShowService showService;

    @Mock
    private TicketTypeService ticketTypeService;

    @Mock
    private SeatService seatService;

    @Mock
    private OrderSeatService orderSeatService;

    @Mock
    private OrderQueryService orderQueryService;

    @InjectMocks
    private OrderCoreServiceImpl orderCoreService;

    private Show validShow;
    private TicketType validTicketType;
    private Seat validSeat;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderCoreService, "orderExpireMinutes", 15);
        ReflectionTestUtils.setField(orderCoreService, "maxTicketsPerOrder", 4);

        validShow = new Show();
        validShow.setId(1L);
        validShow.setStatus(ShowStatus.ON_SALE);

        validTicketType = new TicketType();
        validTicketType.setId(1L);
        validTicketType.setShowId(1L);
        validTicketType.setPrice(new BigDecimal("299.00"));
        validTicketType.setAvailableStock(100);
        validTicketType.setAreaId(1L);

        validSeat = new Seat();
        validSeat.setId(1L);
        validSeat.setAreaId(1L);
    }

    @Nested
    @DisplayName("创建订单 - 正常流程")
    class CreateOrderNormal {

        @Test
        @DisplayName("创建单个座位订单成功")
        void testCreateOrder_SingleSeat_Success() {
            // Given
            CreateOrderRequest request = new CreateOrderRequest();
            request.setShowId(1L);
            request.setTicketTypeId(1L);
            request.setSeatIds(Collections.singletonList(1L));

            when(showService.getById(1L)).thenReturn(validShow);
            when(ticketTypeService.getById(1L)).thenReturn(validTicketType);
            when(seatService.listByIds(anyList())).thenReturn(Collections.singletonList(validSeat));
            when(orderService.save(any(Order.class))).thenReturn(true);
            when(orderSeatService.saveBatch(anyList())).thenReturn(true);
            when(ticketTypeService.update(any())).thenReturn(true);

            OrderResponse expectedResponse = new OrderResponse();
            expectedResponse.setId(1L);
            expectedResponse.setStatus(OrderStatus.PENDING);
            when(orderQueryService.getOrderDetail(any())).thenReturn(expectedResponse);

            // When
            OrderResponse response = orderCoreService.createOrder(1L, request);

            // Then
            assertNotNull(response);
            assertEquals(OrderStatus.PENDING, response.getStatus());
            verify(orderService).save(any(Order.class));
            verify(ticketTypeService).update(any());
        }

        @Test
        @DisplayName("创建多个座位订单成功")
        void testCreateOrder_MultipleSeats_Success() {
            // Given
            Seat seat2 = new Seat();
            seat2.setId(2L);
            seat2.setAreaId(1L);

            CreateOrderRequest request = new CreateOrderRequest();
            request.setShowId(1L);
            request.setTicketTypeId(1L);
            request.setSeatIds(Arrays.asList(1L, 2L));

            when(showService.getById(1L)).thenReturn(validShow);
            when(ticketTypeService.getById(1L)).thenReturn(validTicketType);
            when(seatService.listByIds(anyList())).thenReturn(Arrays.asList(validSeat, seat2));
            when(orderService.save(any(Order.class))).thenReturn(true);
            when(orderSeatService.saveBatch(anyList())).thenReturn(true);
            when(ticketTypeService.update(any())).thenReturn(true);
            when(orderQueryService.getOrderDetail(any())).thenReturn(new OrderResponse());

            // When
            OrderResponse response = orderCoreService.createOrder(1L, request);

            // Then
            assertNotNull(response);
            verify(orderSeatService).saveBatch(anyList());
        }
    }

    @Nested
    @DisplayName("创建订单 - 异常场景")
    class CreateOrderException {

        @Test
        @DisplayName("场次不存在抛出NotFoundException")
        void testCreateOrder_ShowNotFound_ThrowsException() {
            // Given
            CreateOrderRequest request = new CreateOrderRequest();
            request.setShowId(999L);
            request.setTicketTypeId(1L);
            request.setSeatIds(Collections.singletonList(1L));

            when(showService.getById(999L)).thenReturn(null);

            // When & Then
            assertThrows(NotFoundException.class, () -> orderCoreService.createOrder(1L, request));
        }

        @Test
        @DisplayName("场次未开放购票抛出BusinessException")
        void testCreateOrder_ShowNotOnSale_ThrowsException() {
            // Given
            validShow.setStatus(ShowStatus.NOT_ON_SALE);
            CreateOrderRequest request = new CreateOrderRequest();
            request.setShowId(1L);
            request.setTicketTypeId(1L);
            request.setSeatIds(Collections.singletonList(1L));

            when(showService.getById(1L)).thenReturn(validShow);

            // When & Then
            assertThrows(BusinessException.class, () -> orderCoreService.createOrder(1L, request));
        }

        @Test
        @DisplayName("票档不存在抛出NotFoundException")
        void testCreateOrder_TicketTypeNotFound_ThrowsException() {
            // Given
            CreateOrderRequest request = new CreateOrderRequest();
            request.setShowId(1L);
            request.setTicketTypeId(999L);
            request.setSeatIds(Collections.singletonList(1L));

            when(showService.getById(1L)).thenReturn(validShow);
            when(ticketTypeService.getById(999L)).thenReturn(null);

            // When & Then
            assertThrows(NotFoundException.class, () -> orderCoreService.createOrder(1L, request));
        }

        @Test
        @DisplayName("库存不足抛出BusinessException")
        void testCreateOrder_InsufficientStock_ThrowsException() {
            // Given
            validTicketType.setAvailableStock(0);
            CreateOrderRequest request = new CreateOrderRequest();
            request.setShowId(1L);
            request.setTicketTypeId(1L);
            request.setSeatIds(Collections.singletonList(1L));

            when(showService.getById(1L)).thenReturn(validShow);
            when(ticketTypeService.getById(1L)).thenReturn(validTicketType);

            // When & Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderCoreService.createOrder(1L, request));
            assertTrue(ex.getMessage().contains("库存不足"));
        }

        @Test
        @DisplayName("座位与票档区域不匹配抛出BusinessException")
        void testCreateOrder_SeatAreaMismatch_ThrowsException() {
            // Given
            validSeat.setAreaId(999L); // 不同区域
            CreateOrderRequest request = new CreateOrderRequest();
            request.setShowId(1L);
            request.setTicketTypeId(1L);
            request.setSeatIds(Collections.singletonList(1L));

            when(showService.getById(1L)).thenReturn(validShow);
            when(ticketTypeService.getById(1L)).thenReturn(validTicketType);
            when(seatService.listByIds(anyList())).thenReturn(Collections.singletonList(validSeat));

            // When & Then
            assertThrows(BusinessException.class, () -> orderCoreService.createOrder(1L, request));
        }
    }

    @Nested
    @DisplayName("创建订单 - 边界条件")
    class CreateOrderBoundary {

        @Test
        @DisplayName("超过最大购票数抛出BusinessException")
        void testCreateOrder_ExceedMaxTickets_ThrowsException() {
            // Given
            CreateOrderRequest request = new CreateOrderRequest();
            request.setShowId(1L);
            request.setTicketTypeId(1L);
            request.setSeatIds(Arrays.asList(1L, 2L, 3L, 4L, 5L)); // 5张 > max 4

            when(showService.getById(1L)).thenReturn(validShow);
            when(ticketTypeService.getById(1L)).thenReturn(validTicketType);

            // When & Then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> orderCoreService.createOrder(1L, request));
            assertTrue(ex.getMessage().contains("单笔订单最多购买"));
        }
    }
}
