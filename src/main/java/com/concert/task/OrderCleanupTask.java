package com.concert.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.entity.Order;
import com.concert.enums.OrderStatus;
import com.concert.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单清理任务（优化版）
 * 功能：
 * 1. 每分钟清理过期未支付订单（分批处理，避免一次性加载过多）
 * 2. 每小时自动将已结束演出的订单标记为已完成（分批处理）
 * 3. 使用 Redis 分布式锁防止多实例重复执行
 *
 * @author hzf
 * @date 2026-04-23
 */
@Component
public class OrderCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(OrderCleanupTask.class);

    /** 单次处理过期订单的最大数量 */
    private static final int MAX_EXPIRED_BATCH_SIZE = 100;

    /** 单次自动完成订单的最大数量 */
    private static final int MAX_COMPLETE_BATCH_SIZE = 500;

    /** 分布式锁的 Key 前缀 */
    private static final String LOCK_KEY_PREFIX = "concert:ticket:task:";

    /** 锁的持有时间（秒），应大于任务最大执行时间 */
    private static final long LOCK_HOLD_SECONDS = 60;

    @Resource
    private OrderService orderService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // ==================== 过期订单清理 ====================

    /**
     * 每15分钟执行一次，清理过期未支付订单
     * 使用分批查询 + 批量取消，避免一次性加载过多订单
     */
    @Scheduled(cron = "0 */15 * * * ?")
    public void cleanupExpiredOrders() {
        String lockKey = LOCK_KEY_PREFIX + "cleanupExpiredOrders";
        // 尝试获取分布式锁（setIfAbsent + 过期时间）
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, LocalDateTime.now().toString(), LOCK_HOLD_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            logger.info("【过期订单清理】其他实例正在执行，本次跳过");
            return;
        }

        logger.info("【过期订单清理】开始执行...");
        long startTime = System.currentTimeMillis();
        int totalSuccess = 0;
        int totalFail = 0;
        int currentBatch = 0;

        try {
            while (true) {
                currentBatch++;
                // 分批查询过期订单（只查询 ID，减少数据传输）
                LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.select(Order::getId)
                        .eq(Order::getStatus, OrderStatus.PENDING)
                        .lt(Order::getExpireTime, LocalDateTime.now())
                        .last("LIMIT " + MAX_EXPIRED_BATCH_SIZE);

                List<Order> expiredOrders = orderService.list(queryWrapper);
                if (expiredOrders.isEmpty()) {
                    if (currentBatch == 1) {
                        logger.info("【过期订单清理】没有过期订单需要处理");
                    }
                    break;
                }

                List<Long> orderIds = expiredOrders.stream()
                        .map(Order::getId)
                        .collect(Collectors.toList());

                logger.info("【过期订单清理】第 {} 批，发现 {} 个过期订单", currentBatch, orderIds.size());

                try {
                    // 调用 Service 层批量取消方法（需自行实现，或循环调用单条取消）
                    // 推荐在 OrderService 中实现 batchCancelExpiredOrders(orderIds)
                    int success = orderService.batchCancelExpiredOrders(orderIds);
                    totalSuccess += success;
                    totalFail += (orderIds.size() - success);
                    logger.info("【过期订单清理】第 {} 批处理完成，成功：{}，失败：{}",
                            currentBatch, success, orderIds.size() - success);
                } catch (Exception e) {
                    totalFail += orderIds.size();
                    logger.error("【过期订单清理】第 {} 批处理异常，订单 ID 列表：{}，错误：{}",
                            currentBatch, orderIds, e.getMessage(), e);
                }

                // 如果本批数量小于批次大小，说明已经全部处理完毕
                if (expiredOrders.size() < MAX_EXPIRED_BATCH_SIZE) {
                    break;
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("【过期订单清理】全部完成，总批次：{}，总成功：{}，总失败：{}，耗时：{} ms",
                    currentBatch, totalSuccess, totalFail, elapsed);

        } finally {
            // 释放分布式锁
            redisTemplate.delete(lockKey);
        }
    }

    // ==================== 自动完成订单 ====================

    /**
     * 每小时整点执行一次，将演出时间已过的已支付订单自动标记为"已完成"
     * 使用分批更新，避免长事务
     */
    @Scheduled(cron = "0 0 */1 * * ?")
    public void autoCompleteFinishedOrders() {
        String lockKey = LOCK_KEY_PREFIX + "autoCompleteFinishedOrders";
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, LocalDateTime.now().toString(), LOCK_HOLD_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            logger.info("【订单自动完成】其他实例正在执行，本次跳过");
            return;
        }

        logger.info("【订单自动完成】开始执行...");
        long startTime = System.currentTimeMillis();
        int totalUpdated = 0;
        int currentBatch = 0;

        try {
            while (true) {
                currentBatch++;
                // 调用 Service 层批量完成方法（每次处理 MAX_COMPLETE_BATCH_SIZE 条）
                int updated = orderService.batchCompleteFinishedOrders(MAX_COMPLETE_BATCH_SIZE);
                totalUpdated += updated;

                if (updated > 0) {
                    logger.info("【订单自动完成】第 {} 批，完成了 {} 个订单", currentBatch, updated);
                }

                // 如果本批更新的数量小于批次大小，说明已经处理完毕
                if (updated < MAX_COMPLETE_BATCH_SIZE) {
                    break;
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("【订单自动完成】全部完成，总批次：{}，总更新订单数：{}，耗时：{} ms",
                    currentBatch, totalUpdated, elapsed);

        } catch (Exception e) {
            logger.error("【订单自动完成】任务执行异常：{}", e.getMessage(), e);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}