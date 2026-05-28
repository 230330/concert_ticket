package com.concert.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Cache + Redis 缓存配置
 * 为高频访问接口提供缓存策略，减少数据库压力
 *
 * @author hzf
 * @date 2026/05/28
 */
@Configuration
@EnableCaching
public class CacheConfig {

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

    /** 缓存名称常量 */
    public static final String CACHE_CONCERT_LIST = "concert:list";
    public static final String CACHE_CONCERT_DETAIL = "concert:detail";
    public static final String CACHE_SHOW_INFO = "show:info";
    public static final String CACHE_HOT_CONCERT = "concert:hot";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 构建 Jackson 序列化器（与 RedisConfig 保持一致）
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("com.concert.entity")
                .allowIfSubType("com.concert.dto")
                .allowIfSubType("com.concert.enums")
                .allowIfSubType("java.util.")
                .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(java.time.LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        jacksonSerializer.setObjectMapper(objectMapper);

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

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
