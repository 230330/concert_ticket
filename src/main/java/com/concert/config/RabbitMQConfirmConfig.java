package com.concert.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * RabbitMQ 消息可靠性配置
 * - Publisher Confirm：确认消息是否到达交换机
 * - Publisher Return：消息无法路由时退回
 *
 * @author hzf
 * @date 2026/05/28
 */
@Configuration
@ConditionalOnProperty(name = "concert.mq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfirmConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfirmConfig.class);

    @Resource
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
    }

    /**
     * 消息到达交换机的确认回调
     * 无论消息是否成功到达交换机，都会触发此回调
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            logger.debug("消息成功到达交换机：correlationId={}", correlationData != null ? correlationData.getId() : "null");
        } else {
            logger.error("消息未到达交换机：correlationId={}, cause={}",
                    correlationData != null ? correlationData.getId() : "null", cause);
            // 可在此处实现消息重发逻辑
        }
    }

    /**
     * 消息无法路由到队列时的退回回调
     * 当 mandatory=true 且消息无法路由时触发
     */
    @Override
    public void returnedMessage(ReturnedMessage returned) {
        logger.error("消息无法路由到队列：exchange={}, routingKey={}, replyCode={}, replyText={}, message={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText(),
                new String(returned.getMessage().getBody()));
        // 可在此处实现消息重发或告警逻辑
    }
}
