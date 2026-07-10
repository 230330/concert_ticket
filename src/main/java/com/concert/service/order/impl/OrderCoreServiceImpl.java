package com.concert.service.order.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.concert.mq.MessageProducer;
import com.concert.utils.DistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @description: 订单核心流程服务实现（双层防超卖 + 购票限制）
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

    @Value("${concert.order.max-orders-per-show:1}")
    private int maxOrdersPerShow;

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

    @Resource
    private DistributedLock distributedLock;

    @Autowired(required = false)
    private MessageProducer messageProducer;

    @Resource
    private RedisStockService redisStockService;

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
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BusinessException("请选择座位");
        }
        if (seatIds.size() > maxTicketsPerOrder) {
            throw new BusinessException("单笔订单最多购买" + maxTicketsPerOrder + "张票");
        }

        // 4. 检查用户是否已有该场次未支付订单（防刷单）
        checkUserOrderLimit(userId, request.getShowId());

        // 5. 验证座位
        List<Seat> seats = seatService.listByIds(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new NotFoundException("部分座位不存在");
        }

        for (Seat seat : seats) {
            if (!seat.getAreaId().equals(ticketType.getAreaId())) {
                throw new BusinessException("座位与票档区域不匹配");
            }
        }

        // 6. 第一层防护：Redis预扣库存（原子操作，快速失败）
        boolean redisDeducted = false;
        try {
            redisDeducted = redisStockService.preDeductStock(request.getTicketTypeId(), seatIds.size());
            if (!redisDeducted) {
                throw new BusinessException("库存不足，请稍后重试");
            }

            // 7. 第二层防护：分布式锁 + 数据库CAS扣减
            String stockLockKey = "stock:" + request.getShowId() + ":" + ticketType.getId();
            String lockValue = distributedLock.tryLockWithRetry(stockLockKey, 10, 3, 200);
            if (lockValue == null) {
                throw new BusinessException("系统繁忙，请稍后再试");
            }

            try {
                // 数据库层面的库存校验
                if (ticketType.getAvailableStock() < seatIds.size()) {
                    throw new BusinessException("库存不足");
                }

                // CAS乐观锁扣减数据库库存
                LambdaUpdateWrapper<TicketType> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(TicketType::getId, ticketType.getId())
                        .ge(TicketType::getAvailableStock, seatIds.size())
                        .setSql("available_stock = available_stock - " + seatIds.size());
                boolean updated = ticketTypeService.update(updateWrapper);
                if (!updated) {
                    throw new BusinessException("库存不足，请重新选择");
                }
            } finally {
                distributedLock.unlock(stockLockKey, lockValue);
            }

            // 8. 计算总价
            BigDecimal totalAmount = ticketType.getPrice().multiply(new BigDecimal(seatIds.size()));

            // 9. 生成订单
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

            // 10. 批量插入订单座位（唯一索引防重复选座）
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
                // 座位已被占用，回滚Redis预扣库存
                redisStockService.rollbackStock(request.getTicketTypeId(), seatIds.size());
                throw new BusinessException("座位已被占用，请重新选择");
            }

            logger.info("订单创建成功，订单号：{}，用户ID：{}，座位数：{}", order.getOrderNo(), userId, seatIds.size());

            // 11. 发送订单超时延迟消息（MQ实现订单自动取消）
            if (messageProducer != null) {
                long delayMs = orderExpireMinutes * 60 * 1000L;
                messageProducer.sendOrderTimeoutMessage(order.getId(), delayMs);
            } else {
                logger.info("RabbitMQ 已禁用，跳过发送订单超时消息：orderId={}", order.getId());
            }

            return orderQueryService.getOrderDetail(order.getId());

        } catch (BusinessException e) {
            // 业务异常时回滚Redis预扣库存
            if (redisDeducted) {
                try {
                    redisStockService.rollbackStock(request.getTicketTypeId(), seatIds.size());
                } catch (Exception ex) {
                    logger.error("回滚Redis库存失败，ticketTypeId={}, quantity={}", request.getTicketTypeId(), seatIds.size(), ex);
                }
            }
            throw e;
        } catch (Exception e) {
            // 其他异常时回滚Redis预扣库存
            if (redisDeducted) {
                try {
                    redisStockService.rollbackStock(request.getTicketTypeId(), seatIds.size());
                } catch (Exception ex) {
                    logger.error("回滚Redis库存失败，ticketTypeId={}, quantity={}", request.getTicketTypeId(), seatIds.size(), ex);
                }
            }
            throw e;
        }
    }

    /**
     * 检查用户购票限制：同一用户同一场次最多持有N个未支付订单
     */
    private void checkUserOrderLimit(Long userId, Long showId) {
        LambdaQueryWrapper<Order> query = new LambdaQueryWrapper<>();
        query.eq(Order::getUserId, userId)
             .eq(Order::getShowId, showId)
             .eq(Order::getStatus, OrderStatus.PENDING);
        long count = orderService.count(query);
        if (count >= maxOrdersPerShow) {
            throw new BusinessException("您已有该场次的待支付订单，请先完成支付或取消订单");
        }
    }

    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 20);
    }
}
