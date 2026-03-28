package com.concert.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.entity.Order;
import com.concert.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单清理定时任务
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
        queryWrapper.eq(Order::getStatus, 0) // 待支付
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
}
