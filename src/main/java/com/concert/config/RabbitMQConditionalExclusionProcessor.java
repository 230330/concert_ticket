package com.concert.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.*;

/**
 * 根据 concert.mq.enabled 属性，条件化排除 RabbitMQ 自动配置。
 * 
 * 当 concert.mq.enabled=false（默认）时，自动将 RabbitAutoConfiguration 加入
 * spring.autoconfigure.exclude 列表，避免 RabbitMQ 不可用时启动失败。
 * 当 concert.mq.enabled=true 时，不添加排除，允许正常自动配置。
 *
 * @author hzf
 * @date 2026/07/24
 */
public class RabbitMQConditionalExclusionProcessor implements EnvironmentPostProcessor {

    private static final String RABBIT_AUTO_CONFIG =
            "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration";
    private static final String EXCLUDE_KEY = "spring.autoconfigure.exclude";
    private static final String PROPERTY_SOURCE_NAME = "rabbitMQConditionalExclusion";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean mqEnabled = "true".equals(environment.getProperty("concert.mq.enabled"));

        if (!mqEnabled) {
            // 读取已有的排除列表
            String existing = environment.getProperty(EXCLUDE_KEY, "");
            List<String> excludes = new ArrayList<>();
            if (!existing.isEmpty()) {
                excludes.addAll(Arrays.asList(existing.split(",")));
            }
            // 仅当 RabbitAutoConfiguration 尚未被排除时才添加
            if (!excludes.contains(RABBIT_AUTO_CONFIG)) {
                excludes.add(RABBIT_AUTO_CONFIG);
            }

            Map<String, Object> map = new HashMap<>();
            map.put(EXCLUDE_KEY, String.join(",", excludes));

            MutablePropertySources sources = environment.getPropertySources();
            // 避免重复添加
            if (sources.contains(PROPERTY_SOURCE_NAME)) {
                sources.remove(PROPERTY_SOURCE_NAME);
            }
            sources.addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, map));
        }
    }
}
