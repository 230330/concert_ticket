package com.concert.service.order.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.concert.dto.response.OrderResponse;
import com.concert.entity.Order;
import com.concert.entity.User;
import com.concert.enums.OrderStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.ForbiddenException;
import com.concert.exception.NotFoundException;
import com.concert.service.*;
import com.concert.service.order.OrderPaymentService;
import com.concert.service.order.OrderQueryService;
import com.concert.utils.SecureRandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * @description: 订单支付服务实现（防重复支付 + 取票码增强）
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Service
public class OrderPaymentServiceImpl implements OrderPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(OrderPaymentServiceImpl.class);

    @Resource
    private OrderService orderService;

    @Resource
    private UserService userService;

    @Resource
    private NotificationService notificationService;

    @Resource
    private OrderQueryService orderQueryService;

    @Resource
    private RedisStockService redisStockService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse payOrder(Long userId, Long orderId) {
        // 1. 查询订单
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }

        // 验证订单归属
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作此订单");
        }

        // 2. 校验订单状态（使用CAS更新防止并发重复支付）
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("订单状态异常，无法支付");
        }

        // 校验是否过期
        if (LocalDateTime.now().isAfter(order.getExpireTime())) {
            throw new BusinessException("订单已过期，请重新下单");
        }

        // 3. 使用CAS乐观锁更新订单状态，防止并发重复支付
        String pickupCode = SecureRandomUtil.generateAlphanumericCode(8);
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatus.PENDING)  // CAS条件：只有待支付状态才能更新
                .set(Order::getStatus, OrderStatus.PAID)
                .set(Order::getPayTime, LocalDateTime.now())
                .set(Order::getPickupCode, pickupCode)
                .set(Order::getUpdateTime, LocalDateTime.now());

        boolean updated = orderService.update(updateWrapper);
        if (!updated) {
            // CAS更新失败，说明订单状态已被其他线程修改（重复支付）
            throw new BusinessException("订单支付失败，可能已被支付或已取消，请刷新页面查看");
        }

        // 4. 异步发送短信（事务提交后执行）
        String userPhone = userService.getById(userId).getPhone();
        String finalPickupCode = pickupCode;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    notificationService.sendPickupCodeSms(userPhone, finalPickupCode);
                } catch (Exception e) {
                    logger.error("发送取票码短信失败，phone={}, orderId={}", userPhone, orderId, e);
                }
            }
        });

        logger.info("订单支付成功，订单号：{}，取票码：{}", order.getOrderNo(), finalPickupCode);
        return orderQueryService.getOrderDetail(orderId);
    }
}
