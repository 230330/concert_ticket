package com.concert.config;

import com.concert.service.RedisStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @description: 系统启动时自动加载库存到Redis缓存
 * @author: hzf
 * @date: 2026-06-04
 */
@Component
public class StockCacheInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StockCacheInitializer.class);

    @Resource
    private RedisStockService redisStockService;

    @Override
    public void run(String... args) {
        try {
            logger.info("开始加载票档库存到Redis缓存...");
            redisStockService.loadAllStockToRedis();
            logger.info("票档库存缓存加载完成");
        } catch (Exception e) {
            logger.error("加载票档库存到Redis失败，系统仍可正常运行（将使用数据库库存）", e);
        }
    }
}
