package com.concert.service.order.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.response.OrderResponse;
import com.concert.entity.*;
import com.concert.enums.OrderStatus;
import com.concert.enums.ShowStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.NotFoundException;
import com.concert.service.*;
import com.concert.service.order.OrderCoreService;
import com.concert.service.order.OrderQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @description: 订单核心流程服务实现
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Service
public class OrderCoreServiceImpl implements OrderCoreService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCoreServiceImpl.class);

    @Value("${concert.order.expire-minutes:15}")
    private int orderExpireMinutes;

    @Value("${concert.order.max-tickets-per-order:4}")
    private int maxTicketsPerOrder;

    @Resource
    private OrderService orderService;

    @Resource
    private ShowService showService;

    @Resource
    private TicketTypeService ticketTypeService;

    @Resource
    private SeatService seatService;

    @Resource
    private OrderSeatService orderSeatService;

    @Resource
    private OrderQueryService orderQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // 1. 获取场次信息
        Show show = showService.getById(request.getShowId());
        if (show == null) {
            throw new NotFoundException("场次不存在");
        }

        if (show.getStatus() != ShowStatus.ON_SALE) {
            throw new BusinessException("该场次暂未开放购票");
        }

        // 2. 获取票档信息
        TicketType ticketType = ticketTypeService.getById(request.getTicketTypeId());
        if (ticketType == null) {
            throw new NotFoundException("票档不存在");
        }

        if (!ticketType.getShowId().equals(request.getShowId())) {
            throw new BusinessException("票档与场次不匹配");
        }

        // 3. 检查座位数量
        List<Long> seatIds = request.getSeatIds();
        if (seatIds.size() > maxTicketsPerOrder) {
            throw new BusinessException("单笔订单最多购买" + maxTicketsPerOrder + "张票");
        }

        if (ticketType.getAvailableStock() < seatIds.size()) {
            throw new BusinessException("库存不足");
        }

        // 4. 验证座位
        List<Seat> seats = seatService.listByIds(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new NotFoundException("部分座位不存在");
        }

        for (Seat seat : seats) {
            if (!seat.getAreaId().equals(ticketType.getAreaId())) {
                throw new BusinessException("座位与票档区域不匹配");
            }
        }

        // 5. 计算总价
        BigDecimal totalAmount = ticketType.getPrice().multiply(new BigDecimal(seatIds.size()));

        // 6. 生成订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setShowId(request.getShowId());
        order.setTotalAmount(totalAmount);
        order.setTicketTypeId(request.getTicketTypeId());
        order.setSeatInfo(seatIds.toString());
        order.setStatus(OrderStatus.PENDING);
        order.setExpireTime(LocalDateTime.now().plusMinutes(orderExpireMinutes));

        orderService.save(order);

        // 7. 批量插入订单座位
        try {
            List<OrderSeat> orderSeats = new ArrayList<>();
            for (Long seatId : seatIds) {
                OrderSeat orderSeat = new OrderSeat();
                orderSeat.setOrderId(order.getId());
                orderSeat.setShowId(request.getShowId());
                orderSeat.setSeatId(seatId);
                orderSeat.setTicketTypeId(request.getTicketTypeId());
                orderSeat.setPrice(ticketType.getPrice());
                orderSeats.add(orderSeat);
            }
            orderSeatService.saveBatch(orderSeats);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("座位已被占用，请重新选择");
        }

        // 8. 更新票档库存
        LambdaUpdateWrapper<TicketType> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TicketType::getId, ticketType.getId())
                .ge(TicketType::getAvailableStock, seatIds.size())
                .setSql("available_stock = available_stock - " + seatIds.size());
        boolean updated = ticketTypeService.update(updateWrapper);
        if (!updated) {
            throw new BusinessException("库存不足");
        }

        logger.info("订单创建成功，订单号：{}，用户ID：{}，座位数：{}", order.getOrderNo(), userId, seatIds.size());

        return orderQueryService.getOrderDetail(order.getId());
    }

    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 20);
    }
}
