package com.concert.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 统一配置类
 * - objectMapper: HTTP 序列化（不含类型信息，供 Spring MVC 使用）
 * - redisObjectMapper: Redis 序列化（含多态类型信息，供 RedisTemplate / CacheManager 使用）
 *
 * @author hzf
 * @date 2026-05-07
 */
@Configuration
public class JacksonConfig {

    /** 统一的日期时间格式 */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 构建通用的 JavaTimeModule（LocalDateTime 序列化/反序列化）
     */
    private static JavaTimeModule buildJavaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        return module;
    }

    /**
     * HTTP 序列化 ObjectMapper（Spring MVC 默认使用）
     * 不含类型信息，输出干净 JSON
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "redisObjectMapper")
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(buildJavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Redis 序列化专用 ObjectMapper
     * 包含多态类型信息（@class），确保反序列化时能正确还原 Java 类型
     * 供 RedisTemplate 和 CacheManager 共享使用
     */
    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 安全配置：白名单包名，防止反序列化 RCE 漏洞
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("com.concert.entity")
                .allowIfSubType("com.concert.dto")
                .allowIfSubType("com.concert.enums")
                .allowIfSubType("java.util.")
                .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        mapper.registerModule(buildJavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        return mapper;
    }
}
