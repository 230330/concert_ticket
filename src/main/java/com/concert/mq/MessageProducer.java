package com.concert.mq;

import com.concert.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * RabbitMQ 消息生产者
 * 封装消息发送逻辑，支持普通消息和延迟消息
 *
 * @author hzf
 * @date 2026/05/28
 */
@Component
public class MessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(MessageProducer.class);

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送短信消息（异步处理短信发送）
     *
     * @param phone       手机号
     * @param code        验证码
     * @param templateCode 短信模板编码
     */
    public void sendSmsMessage(String phone, String code, String templateCode) {
        String message = phone + ":" + code + ":" + templateCode;
        sendMessage(RabbitMQConfig.EXCHANGE_TOPIC, RabbitMQConfig.ROUTING_KEY_SMS, message,
                "短信消息[phone=" + phone + "]");
    }

    /**
     * 发送支付结果通知消息
     *
     * @param orderId 订单ID
     * @param status  支付状态（PAID/REFUNDED）
     */
    public void sendPaymentNotification(Long orderId, String status) {
        String message = orderId + ":" + status;
        sendMessage(RabbitMQConfig.EXCHANGE_TOPIC, RabbitMQConfig.ROUTING_KEY_PAYMENT, message,
                "支付通知[orderId=" + orderId + ", status=" + status + "]");
    }

    /**
     * 发送订单超时延迟消息
     * 消息将延迟 delayMs 毫秒后投递到订单超时处理队列
     *
     * @param orderId 订单ID
     * @param delayMs 延迟时间（毫秒）
     */
    public void sendOrderTimeoutMessage(Long orderId, long delayMs) {
        String message = String.valueOf(orderId);
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());

        MessagePostProcessor processor = msg -> {
            // 设置消息过期时间（毫秒），过期后通过死信转发到 orderTimeoutQueue
            msg.getMessageProperties().setExpiration(String.valueOf(delayMs));
            return msg;
        };

        try {
            rabbitTemplate.convertAndSend(
                    "",  // 使用默认交换机直接投递到延迟队列
                    RabbitMQConfig.QUEUE_ORDER_TIMEOUT_DELAY,
                    message,
                    processor,
                    correlationData
            );
            logger.info("发送订单超时延迟消息：orderId={}, delayMs={}", orderId, delayMs);
        } catch (AmqpException e) {
            logger.error("发送订单超时延迟消息失败：orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 通用消息发送方法
     */
    private void sendMessage(String exchange, String routingKey, String message, String desc) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
            logger.info("发送{}成功：{}", desc, message);
        } catch (AmqpException e) {
            logger.error("发送{}失败：{}, error={}", desc, message, e.getMessage(), e);
        }
    }
}
