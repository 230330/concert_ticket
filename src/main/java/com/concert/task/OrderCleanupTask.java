package com.concert.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.entity.Order;
import com.concert.enums.OrderStatus;
import com.concert.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @description: 订单清理任务
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Component
public class OrderCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(OrderCleanupTask.class);

    @Resource
    private OrderService orderService;

    /**
     * 每分钟执行一次，清理过期未支付订单
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void cleanupExpiredOrders() {
        logger.info("开始执行过期订单清理任务...");

        // 查询所有状态为待支付且已过期的订单
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getStatus, OrderStatus.PENDING) // 待支付
                .lt(Order::getExpireTime, LocalDateTime.now()); // 已过期

        List<Order> expiredOrders = orderService.list(queryWrapper);

        if (expiredOrders.isEmpty()) {
            logger.info("没有过期订单需要处理");
            return;
        }

        logger.info("发现 {} 个过期订单，开始处理...", expiredOrders.size());

        int successCount = 0;
        int failCount = 0;

        for (Order order : expiredOrders) {
            try {
                orderService.cancelExpiredOrder(order.getId());
                successCount++;
            } catch (Exception e) {
                failCount++;
                logger.error("取消过期订单失败，订单ID：{}，错误：{}", order.getId(), e.getMessage());
            }
        }

        logger.info("过期订单清理完成，成功：{}，失败：{}", successCount, failCount);
    }

    /**
     * 每小时执行一次，将演出时间已过的已支付订单自动标记为"已完成"
     */
    @Scheduled(cron = "0 0 */1 * * ?")
    public void autoCompleteFinishedOrders() {
        logger.info("开始执行订单自动完成任务...");
        try {
            orderService.completeFinishedOrders();
        } catch (Exception e) {
            logger.error("订单自动完成任务执行失败：{}", e.getMessage());
        }
    }
}
