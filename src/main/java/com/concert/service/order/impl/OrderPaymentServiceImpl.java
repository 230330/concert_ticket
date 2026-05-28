package com.concert.service.order.impl;

import com.concert.dto.response.OrderResponse;
import com.concert.entity.Order;
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
 * @description: 订单支付服务实现
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

        // 2. 校验订单状态
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("订单状态异常，无法支付");
        }

        // 校验是否过期
        if (LocalDateTime.now().isAfter(order.getExpireTime())) {
            throw new BusinessException("订单已过期，请重新下单");
        }

        // 3. 更新订单状态
        order.setStatus(OrderStatus.PAID);
        order.setPayTime(LocalDateTime.now());
        order.setPickupCode(SecureRandomUtil.generateAlphanumericCode(8));
        orderService.updateById(order);

        // 异步发送短信（事务提交后执行）
        String userPhone = userService.getById(userId).getPhone();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.sendPickupCodeSms(userPhone, order.getPickupCode());
            }
        });

        logger.info("订单支付成功，订单号：{}，取票码：{}", order.getOrderNo(), order.getPickupCode());
        return orderQueryService.getOrderDetail(orderId);
    }
}
