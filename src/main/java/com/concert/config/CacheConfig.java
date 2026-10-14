package com.concert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Cache + Redis 缓存配置
 * 复用 JacksonConfig#redisObjectMapper 避免重复配置
 *
 * <p>用户端缓存（concert:list、concert:detail、concert:hot、show:info）由
 * ConcertCacheEvictionAspect 在管理端写入成功后统一清理；
 * 看板缓存（dashboard:sales、dashboard:revenue）的数据随订单变化持续更新，依靠 TTL 过期而非主动清理
 *
 * @author hzf
 * @date 2026/05/28
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /** 默认缓存过期时间：30分钟 */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    /** 演唱会列表缓存：10分钟 */
    private static final Duration CONCERT_LIST_TTL = Duration.ofMinutes(10);

    /** 演唱会详情缓存：30分钟 */
    private static final Duration CONCERT_DETAIL_TTL = Duration.ofMinutes(30);

    /** 场次信息缓存：5分钟（场次状态变化较快） */
    private static final Duration SHOW_INFO_TTL = Duration.ofMinutes(5);

    /** 热门演唱会缓存：5分钟 */
    private static final Duration HOT_CONCERT_TTL = Duration.ofMinutes(5);

    /** 销售概览缓存：5分钟（随订单变化，看板需要相对新鲜的数据） */
    private static final Duration DASHBOARD_SALES_TTL = Duration.ofMinutes(5);

    /** 收入报表缓存：10分钟（按日期区间查询，聚合开销较大） */
    private static final Duration DASHBOARD_REVENUE_TTL = Duration.ofMinutes(10);

    /** 缓存名称常量 */
    public static final String CACHE_CONCERT_LIST = "concert:list";
    public static final String CACHE_CONCERT_DETAIL = "concert:detail";
    public static final String CACHE_SHOW_INFO = "show:info";
    public static final String CACHE_HOT_CONCERT = "concert:hot";
    public static final String CACHE_DASHBOARD_SALES = "dashboard:sales";
    public static final String CACHE_DASHBOARD_REVENUE = "dashboard:revenue";

    @Resource
    private ObjectMapper redisObjectMapper;

    /**
     * 注册缓存异常处理器，使缓存故障降级而非向上抛出
     * 交由 Spring 的 CachingConfigurer 机制生效，直接声明 CacheErrorHandler Bean 不会被自动识别
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 复用 JacksonConfig 中统一配置的 redisObjectMapper
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        jacksonSerializer.setObjectMapper(redisObjectMapper);

        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jacksonSerializer))
                .disableCachingNullValues();

        // 各缓存空间的自定义配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CACHE_CONCERT_LIST, defaultConfig.entryTtl(CONCERT_LIST_TTL));
        cacheConfigurations.put(CACHE_CONCERT_DETAIL, defaultConfig.entryTtl(CONCERT_DETAIL_TTL));
        cacheConfigurations.put(CACHE_SHOW_INFO, defaultConfig.entryTtl(SHOW_INFO_TTL));
        cacheConfigurations.put(CACHE_HOT_CONCERT, defaultConfig.entryTtl(HOT_CONCERT_TTL));
        cacheConfigurations.put(CACHE_DASHBOARD_SALES, defaultConfig.entryTtl(DASHBOARD_SALES_TTL));
        cacheConfigurations.put(CACHE_DASHBOARD_REVENUE, defaultConfig.entryTtl(DASHBOARD_REVENUE_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
