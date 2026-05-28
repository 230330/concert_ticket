package com.concert.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 交换机、队列、绑定配置
 *
 * 架构设计：
 * - concert.exchange.topic   : 主题交换机（短信、支付通知等业务消息）
 * - concert.exchange.delay   : 延迟交换机（订单超时自动取消等延迟消息，基于死信队列实现）
 * - concert.queue.sms        : 短信发送队列
 * - concert.queue.payment    : 支付结果通知队列
 * - concert.queue.order.timeout       : 订单超时处理队列
 * - concert.queue.order.timeout.delay : 订单超时延迟队列（消息过期后转入 order.timeout）
 * - concert.queue.dlx                 : 死信队列（消费失败的消息）
 *
 * @author hzf
 * @date 2026/05/28
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 交换机 ====================

    /** 业务主题交换机 */
    public static final String EXCHANGE_TOPIC = "concert.exchange.topic";

    /** 延迟交换机（直接投递到延迟队列，由 TTL + 死信驱动） */
    public static final String EXCHANGE_DELAY = "concert.exchange.delay";

    /** 死信交换机 */
    public static final String EXCHANGE_DLX = "concert.exchange.dlx";

    // ==================== 队列 ====================

    /** 短信发送队列 */
    public static final String QUEUE_SMS = "concert.queue.sms";

    /** 支付结果通知队列 */
    public static final String QUEUE_PAYMENT = "concert.queue.payment";

    /** 订单超时处理队列（最终消费队列） */
    public static final String QUEUE_ORDER_TIMEOUT = "concert.queue.order.timeout";

    /** 订单超时延迟队列（消息过期后转入 order.timeout 队列） */
    public static final String QUEUE_ORDER_TIMEOUT_DELAY = "concert.queue.order.timeout.delay";

    /** 死信队列 */
    public static final String QUEUE_DLX = "concert.queue.dlx";

    // ==================== 路由键 ====================

    public static final String ROUTING_KEY_SMS = "concert.sms";
    public static final String ROUTING_KEY_PAYMENT = "concert.payment";
    public static final String ROUTING_KEY_ORDER_TIMEOUT = "concert.order.timeout";

    // ==================== 交换机 Bean ====================

    @Bean
    public TopicExchange topicExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_TOPIC)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange delayExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_DELAY)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_DLX)
                .durable(true)
                .build();
    }

    // ==================== 队列 Bean ====================

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(QUEUE_SMS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_DLX)
                .build();
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_DLX)
                .build();
    }

    /**
     * 订单超时最终消费队列
     * 消费者监听此队列，处理超时订单
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_TIMEOUT)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", QUEUE_DLX)
                .build();
    }

    /**
     * 订单超时延迟队列
     * 消息在此队列中等待 TTL 过期，过期后通过死信机制转发到 orderTimeoutQueue
     * 不设固定 TTL，由发送消息时通过 expiration 参数指定
     */
    @Bean
    public Queue orderTimeoutDelayQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_TIMEOUT_DELAY)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DELAY)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_ORDER_TIMEOUT)
                .build();
    }

    /** 死信队列 */
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(QUEUE_DLX).build();
    }

    // ==================== 绑定 Bean ====================

    @Bean
    public Binding smsBinding() {
        return BindingBuilder.bind(smsQueue()).to(topicExchange()).with(ROUTING_KEY_SMS);
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder.bind(paymentQueue()).to(topicExchange()).with(ROUTING_KEY_PAYMENT);
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(delayExchange()).with(ROUTING_KEY_ORDER_TIMEOUT);
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with(QUEUE_DLX);
    }
}
