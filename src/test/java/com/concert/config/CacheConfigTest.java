package com.concert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * CacheConfig 单元测试
 * 锁定各缓存区域的名称与过期时间：未在 CacheConfig 中声明的区域会静默使用 30 分钟默认 TTL
 */
class CacheConfigTest {

    private RedisCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        CacheConfig cacheConfig = new CacheConfig();
        ReflectionTestUtils.setField(cacheConfig, "redisObjectMapper", new ObjectMapper());
        cacheManager = (RedisCacheManager) cacheConfig.cacheManager(mock(RedisConnectionFactory.class));
        // 容器会对 CacheManager Bean 调用 afterPropertiesSet，此处手动触发以按声明初始化各缓存区域
        cacheManager.afterPropertiesSet();
    }

    @Test
    @DisplayName("用户端缓存区域已声明且 TTL 符合约定")
    void testConcertCacheTtls() {
        assertEquals(Duration.ofMinutes(10), ttlOf(CacheConfig.CACHE_CONCERT_LIST));
        assertEquals(Duration.ofMinutes(30), ttlOf(CacheConfig.CACHE_CONCERT_DETAIL));
        assertEquals(Duration.ofMinutes(5), ttlOf(CacheConfig.CACHE_SHOW_INFO));
        assertEquals(Duration.ofMinutes(5), ttlOf(CacheConfig.CACHE_HOT_CONCERT));
    }

    @Test
    @DisplayName("看板缓存区域已显式声明，不退化为默认 TTL")
    void testDashboardCacheTtls() {
        assertEquals(Duration.ofMinutes(5), ttlOf(CacheConfig.CACHE_DASHBOARD_SALES));
        assertEquals(Duration.ofMinutes(10), ttlOf(CacheConfig.CACHE_DASHBOARD_REVENUE));
    }

    /**
     * 取指定缓存区域实际生效的过期时间
     * 未在 CacheConfig 中声明的区域要么取不到配置，要么拿到 30 分钟默认值，两种情况下断言都会失败
     */
    private Duration ttlOf(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        assertNotNull(cache, "缓存区域不可用：" + cacheName);

        // CacheManager 启用了 transactionAware()，需要先剥掉事务装饰器
        if (cache instanceof TransactionAwareCacheDecorator) {
            cache = ((TransactionAwareCacheDecorator) cache).getTargetCache();
        }

        RedisCacheConfiguration configuration = ((RedisCache) cache).getCacheConfiguration();
        assertNotNull(configuration, "缓存区域未配置：" + cacheName);
        return configuration.getTtl();
    }
}
