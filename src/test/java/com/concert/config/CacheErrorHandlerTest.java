package com.concert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 缓存故障降级测试
 *
 * <p>用真实的 CacheConfig 加一个连不上的 Redis 连接工厂，验证 Redis 不可用时
 * @Cacheable 方法仍然返回数据库结果，而不是把 RedisConnectionFailureException 抛给调用方
 */
class CacheErrorHandlerTest {

    @Test
    @DisplayName("Redis 不可用时 @Cacheable 方法回落数据库且不抛出异常")
    void testCacheableFallsBackWhenRedisUnavailable() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestConfig.class)) {

            CountingService service = context.getBean(CountingService.class);

            String first = assertDoesNotThrow(service::load);
            String second = assertDoesNotThrow(service::load);

            // 结果来自数据库；缓存不可用时每次都真正执行方法体
            assertEquals("db-value", first);
            assertEquals("db-value", second);
            assertEquals(2, service.getInvocations());
        }
    }

    @Configuration
    @EnableCaching
    @Import(CacheConfig.class)
    static class TestConfig {

        /**
         * 模拟 Redis 不可用：获取连接即失败
         */
        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
            when(factory.getConnection()).thenThrow(new RedisConnectionFailureException("redis down"));
            return factory;
        }

        /**
         * CacheConfig 通过 @Resource 注入该 Bean
         */
        @Bean
        ObjectMapper redisObjectMapper() {
            return new ObjectMapper();
        }

        @Bean
        CountingService countingService() {
            return new CountingService();
        }
    }

    /**
     * 被测的缓存方法载体
     */
    static class CountingService {

        private final AtomicInteger invocations = new AtomicInteger();

        @Cacheable(value = CacheConfig.CACHE_CONCERT_DETAIL, key = "'probe'")
        public String load() {
            invocations.incrementAndGet();
            return "db-value";
        }

        int getInvocations() {
            return invocations.get();
        }
    }
}
