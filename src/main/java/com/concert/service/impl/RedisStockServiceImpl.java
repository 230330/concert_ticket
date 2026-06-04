package com.concert.service.impl;

import com.concert.entity.TicketType;
import com.concert.service.RedisStockService;
import com.concert.service.TicketTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @description: Redis库存预扣减服务实现
 * 使用Redis Lua脚本保证库存预扣和回滚的原子性
 * @author: hzf
 * @date: 2026-06-04
 */
@Service
public class RedisStockServiceImpl implements RedisStockService {

    private static final Logger logger = LoggerFactory.getLogger(RedisStockServiceImpl.class);

    /** 库存缓存key前缀 */
    private static final String STOCK_KEY_PREFIX = "stock:ticket_type:";

    /** 库存缓存过期时间（小时） */
    private static final long STOCK_CACHE_HOURS = 24;

    /**
     * 预扣库存Lua脚本：
     * 如果库存 >= 扣减数量，则扣减并返回1；否则返回0
     */
    private static final String PRE_DEDUCT_SCRIPT =
            "local stock = tonumber(redis.call('get', KEYS[1])) " +
            "if stock == nil then return -1 end " +
            "if stock >= tonumber(ARGV[1]) then " +
            "  redis.call('decrby', KEYS[1], ARGV[1]) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";

    /**
     * 回滚库存Lua脚本：
     * 增加库存数量
     */
    private static final String ROLLBACK_SCRIPT =
            "local stock = tonumber(redis.call('get', KEYS[1])) " +
            "if stock == nil then return -1 end " +
            "redis.call('incrby', KEYS[1], ARGV[1]) " +
            "return 1";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private TicketTypeService ticketTypeService;

    @Override
    public boolean preDeductStock(Long ticketTypeId, int quantity) {
        String key = STOCK_KEY_PREFIX + ticketTypeId;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(PRE_DEDUCT_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(key), String.valueOf(quantity));

        if (result == null) {
            logger.warn("预扣库存Lua脚本执行返回null，ticketTypeId={}", ticketTypeId);
            return false;
        }

        if (result == -1) {
            // Redis中无缓存，尝试初始化后重试一次
            logger.info("Redis库存缓存不存在，尝试初始化，ticketTypeId={}", ticketTypeId);
            initStockIfNeeded(ticketTypeId);
            result = stringRedisTemplate.execute(script,
                    Collections.singletonList(key), String.valueOf(quantity));
            if (result != null && result == 1) {
                logger.info("初始化后预扣库存成功，ticketTypeId={}, quantity={}", ticketTypeId, quantity);
                return true;
            }
            logger.warn("初始化后预扣库存仍失败，ticketTypeId={}, result={}", ticketTypeId, result);
            return false;
        }

        if (result == 1) {
            logger.debug("预扣库存成功，ticketTypeId={}, quantity={}", ticketTypeId, quantity);
            return true;
        }

        logger.info("预扣库存失败（库存不足），ticketTypeId={}, quantity={}", ticketTypeId, quantity);
        return false;
    }

    @Override
    public void rollbackStock(Long ticketTypeId, int quantity) {
        String key = STOCK_KEY_PREFIX + ticketTypeId;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(ROLLBACK_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(key), String.valueOf(quantity));

        if (result != null && result == 1) {
            logger.debug("回滚库存成功，ticketTypeId={}, quantity={}", ticketTypeId, quantity);
        } else {
            logger.warn("回滚库存失败，ticketTypeId={}, quantity={}, result={}", ticketTypeId, quantity, result);
            // 即使Redis回滚失败，数据库层面的回滚仍会执行，确保最终一致性
        }
    }

    @Override
    public int getAvailableStock(Long ticketTypeId) {
        String key = STOCK_KEY_PREFIX + ticketTypeId;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return -1;
        }
        return Integer.parseInt(value);
    }

    @Override
    public void loadStockToRedis(Long ticketTypeId) {
        TicketType ticketType = ticketTypeService.getById(ticketTypeId);
        if (ticketType != null) {
            String key = STOCK_KEY_PREFIX + ticketTypeId;
            stringRedisTemplate.opsForValue().set(key,
                    String.valueOf(ticketType.getAvailableStock()),
                    STOCK_CACHE_HOURS, TimeUnit.HOURS);
            logger.info("加载票档库存到Redis，ticketTypeId={}, stock={}", ticketTypeId, ticketType.getAvailableStock());
        }
    }

    @Override
    public void loadAllStockToRedis() {
        List<TicketType> ticketTypes = ticketTypeService.list();
        int loaded = 0;
        for (TicketType ticketType : ticketTypes) {
            String key = STOCK_KEY_PREFIX + ticketType.getId();
            stringRedisTemplate.opsForValue().set(key,
                    String.valueOf(ticketType.getAvailableStock()),
                    STOCK_CACHE_HOURS, TimeUnit.HOURS);
            loaded++;
        }
        logger.info("加载所有票档库存到Redis完成，共{}条", loaded);
    }

    @Override
    public void initStockIfNeeded(Long ticketTypeId) {
        String key = STOCK_KEY_PREFIX + ticketTypeId;
        // 使用setIfAbsent避免覆盖已有的缓存
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key,
                String.valueOf(-1), 5, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(success)) {
            // 获取到了设置权，从数据库加载真实库存
            loadStockToRedis(ticketTypeId);
        }
    }
}
