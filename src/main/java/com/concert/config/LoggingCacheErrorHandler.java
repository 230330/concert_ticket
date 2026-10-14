package com.concert.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * 缓存异常处理器：记录告警后跳过缓存操作，不向上抛出
 *
 * <p>缓存是可降级的依赖，读写失败不应该影响接口可用性：
 * 读取失败时 Spring 会当作未命中继续执行方法体（回落数据库），
 * 写入/清除失败时仅跳过缓存更新。
 * 默认的 SimpleCacheErrorHandler 会直接抛异常，导致 Redis 抖动时相关接口整体不可用。
 *
 * <p>异常只在此处记录，因此保留完整堆栈便于定位（例如序列化失败）；
 * 若担心 Redis 长时间不可用刷屏，可按 cache 维度采样后再收敛日志级别。
 *
 * @author hzf
 * @date 2026/10/14
 */
public class LoggingCacheErrorHandler implements CacheErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoggingCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log("读取", exception, cache, key);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log("写入", exception, cache, key);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log("清除", exception, cache, key);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log("清空", exception, cache, null);
    }

    /**
     * 统一记录缓存操作失败，方法返回即表示异常已被吞掉
     */
    private void log(String action, RuntimeException exception, Cache cache, Object key) {
        logger.warn("缓存{}失败，已跳过缓存操作：cache={}, key={}", action, cache.getName(), key, exception);
    }
}
