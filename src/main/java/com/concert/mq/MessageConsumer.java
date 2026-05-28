package com.concert.mq;

import com.concert.service.NotificationService;
import com.concert.service.OrderService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * RabbitMQ 消息消费者
 * 处理各类业务消息，支持手动确认和重试
 *
 * @author hzf
 * @date 2026/05/28
 */
@Component
public class MessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);

    @Resource
    private NotificationService notificationService;

    @Resource
    private OrderService orderService;

    /**
     * 消费短信发送消息
     */
    @RabbitListener(queues = "concert.queue.sms")
    public void handleSmsMessage(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());

        try {
            logger.info("收到短信消息：{}", body);
            String[] parts = body.split(":");
            if (parts.length >= 2) {
                String phone = parts[0];
                String code = parts[1];
                // 调用短信服务发送
                notificationService.sendVerificationCodeSms(phone, code);
            }
            // 手动确认
            channel.basicAck(deliveryTag, false);
            logger.info("短信消息处理完成：{}", body);
        } catch (Exception e) {
            logger.error("短信消息处理失败：{}, error={}", body, e.getMessage(), e);
            try {
                // 拒绝并重新入队（重试）
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                logger.error("短信消息NACK失败：{}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * 消费支付结果通知消息
     */
    @RabbitListener(queues = "concert.queue.payment")
    public void handlePaymentMessage(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());

        try {
            logger.info("收到支付通知消息：{}", body);
            String[] parts = body.split(":");
            if (parts.length >= 2) {
                Long orderId = Long.parseLong(parts[0]);
                String status = parts[1];
                // 这里可以扩展：发送支付结果通知给用户（App推送、邮件等）
                logger.info("支付通知处理：orderId={}, status={}", orderId, status);
            }
            channel.basicAck(deliveryTag, false);
            logger.info("支付通知消息处理完成：{}", body);
        } catch (Exception e) {
            logger.error("支付通知消息处理失败：{}, error={}", body, e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                logger.error("支付通知消息NACK失败：{}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * 消费订单超时消息
     * 订单创建后发送延迟消息，到期后此消费者处理超时订单
     */
    @RabbitListener(queues = "concert.queue.order.timeout")
    public void handleOrderTimeoutMessage(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());

        try {
            Long orderId = Long.parseLong(body.trim());
            logger.info("收到订单超时消息：orderId={}", orderId);

            // 查询订单状态，如果仍是待支付则取消
            orderService.cancelExpiredOrder(orderId);

            channel.basicAck(deliveryTag, false);
            logger.info("订单超时处理完成：orderId={}", orderId);
        } catch (NumberFormatException e) {
            logger.error("订单超时消息格式错误：{}", body);
            try {
                // 格式错误的消息直接丢弃，不重试
                channel.basicAck(deliveryTag, false);
            } catch (Exception ex) {
                logger.error("订单超时消息ACK失败：{}", ex.getMessage(), ex);
            }
        } catch (Exception e) {
            logger.error("订单超时处理失败：orderId={}, error={}", body, e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                logger.error("订单超时消息NACK失败：{}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * 消费死信队列消息
     * 所有消费失败达到最大重试次数的消息会进入此队列
     * 需要人工介入处理或记录到数据库
     */
    @RabbitListener(queues = "concert.queue.dlx")
    public void handleDlxMessage(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());

        try {
            logger.error("收到死信消息（需人工处理）：{}", body);

            // 记录到数据库或发送告警通知
            // TODO: 可接入告警系统（邮件/钉钉/企业微信等）

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            logger.error("死信消息处理失败：{}", e.getMessage(), e);
            try {
                channel.basicAck(deliveryTag, false);  // 死信不再重试
            } catch (Exception ex) {
                logger.error("死信消息ACK失败：{}", ex.getMessage(), ex);
            }
        }
    }
}
