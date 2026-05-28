package com.concert.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁工具类
 * 基于 SETNX + EXPIRE 实现，使用 Lua 脚本保证释放锁的原子性
 *
 * <p>使用示例：
 * <pre>
 *   String lockKey = "order:stock:" + ticketTypeId;
 *   String lockValue = distributedLock.tryLock(lockKey, 10);
 *   if (lockValue != null) {
 *       try {
 *           // 执行业务逻辑
 *       } finally {
 *           distributedLock.unlock(lockKey, lockValue);
 *       }
 *   }
 * </pre>
 *
 * @author hzf
 * @date 2026/05/28
 */
@Component
public class DistributedLock {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLock.class);

    /** 锁 key 前缀 */
    private static final String LOCK_KEY_PREFIX = "lock:";

    /** 释放锁的 Lua 脚本：只有当锁的值与传入值相等时才删除，保证不会误删其他线程的锁 */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   return redis.call('del', KEYS[1]) " +
            "else " +
            "   return 0 " +
            "end";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取分布式锁（立即返回）
     *
     * @param lockKey        锁的 key（不含前缀，会自动添加 "lock:" 前缀）
     * @param expireSeconds  锁的过期时间（秒），必须大于业务执行时间
     * @return 锁的唯一标识值（用于安全释放锁），获取失败返回 null
     */
    public String tryLock(String lockKey, long expireSeconds) {
        String fullKey = LOCK_KEY_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString().replace("-", "");

        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(fullKey, lockValue, expireSeconds, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(success)) {
            logger.debug("获取分布式锁成功：key={}, value={}", fullKey, lockValue);
            return lockValue;
        }

        logger.debug("获取分布式锁失败：key={}", fullKey);
        return null;
    }

    /**
     * 尝试获取分布式锁（带重试）
     *
     * @param lockKey        锁的 key
     * @param expireSeconds  锁的过期时间（秒）
     * @param retryTimes     重试次数
     * @param retryInterval  重试间隔（毫秒）
     * @return 锁的唯一标识值，获取失败返回 null
     */
    public String tryLockWithRetry(String lockKey, long expireSeconds, int retryTimes, long retryInterval) {
        for (int i = 0; i <= retryTimes; i++) {
            String lockValue = tryLock(lockKey, expireSeconds);
            if (lockValue != null) {
                return lockValue;
            }

            if (i < retryTimes) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("获取分布式锁被中断：key={}", lockKey);
                    return null;
                }
            }
        }

        logger.warn("获取分布式锁重试失败：key={}, 重试次数={}", lockKey, retryTimes);
        return null;
    }

    /**
     * 释放分布式锁
     * 使用 Lua 脚本保证原子性：只有当锁的值与传入值相等时才删除
     *
     * @param lockKey   锁的 key
     * @param lockValue 锁的唯一标识值（tryLock 返回的值）
     * @return 是否成功释放
     */
    public boolean unlock(String lockKey, String lockValue) {
        String fullKey = LOCK_KEY_PREFIX + lockKey;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(redisScript,
                Collections.singletonList(fullKey), lockValue);

        boolean released = Long.valueOf(1L).equals(result);
        if (released) {
            logger.debug("释放分布式锁成功：key={}", fullKey);
        } else {
            logger.warn("释放分布式锁失败（锁已过期或被其他线程持有）：key={}", fullKey);
        }

        return released;
    }

    /**
     * 在分布式锁保护下执行任务
     *
     * @param lockKey        锁的 key
     * @param expireSeconds  锁的过期时间（秒）
     * @param task           要执行的任务
     * @param <T>            返回值类型
     * @return 任务执行结果
     * @throws IllegalStateException 获取锁失败时抛出
     */
    public <T> T executeWithLock(String lockKey, long expireSeconds, LockTask<T> task) {
        String lockValue = tryLock(lockKey, expireSeconds);
        if (lockValue == null) {
            throw new IllegalStateException("系统繁忙，请稍后再试");
        }

        try {
            return task.execute();
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 在分布式锁保护下执行任务（带重试）
     *
     * @param lockKey        锁的 key
     * @param expireSeconds  锁的过期时间（秒）
     * @param retryTimes     重试次数
     * @param retryInterval  重试间隔（毫秒）
     * @param task           要执行的任务
     * @param <T>            返回值类型
     * @return 任务执行结果
     * @throws IllegalStateException 获取锁失败时抛出
     */
    public <T> T executeWithLockRetry(String lockKey, long expireSeconds,
                                       int retryTimes, long retryInterval,
                                       LockTask<T> task) {
        String lockValue = tryLockWithRetry(lockKey, expireSeconds, retryTimes, retryInterval);
        if (lockValue == null) {
            throw new IllegalStateException("系统繁忙，请稍后再试");
        }

        try {
            return task.execute();
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 锁任务函数式接口
     */
    @FunctionalInterface
    public interface LockTask<T> {
        T execute();
    }
}
