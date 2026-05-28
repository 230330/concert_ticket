package com.concert.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.concert.config.AlipayProperties;
import com.concert.entity.Order;
import com.concert.enums.OrderStatus;
import com.concert.exception.BusinessException;
import com.concert.mq.MessageProducer;
import com.concert.service.OrderService;
import com.concert.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务实现
 * 集成支付宝电脑网站支付（Page Pay）
 *
 * @author hzf
 * @date 2026/05/28
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    /** 支付幂等性 Redis Key 前缀 */
    private static final String PAY_IDEMPOTENT_PREFIX = "pay:idempotent:";

    /** 幂等性 Key 过期时间（24小时） */
    private static final long IDEMPOTENT_EXPIRE_HOURS = 24;

    @Resource
    private AlipayClient alipayClient;

    @Resource
    private AlipayProperties alipayProperties;

    @Resource
    private OrderService orderService;

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String createAlipayOrder(Long orderId, String orderNo, BigDecimal totalAmount, String subject) {
        // 1. 防重复提交：检查是否已发起过支付
        String idempotentKey = PAY_IDEMPOTENT_PREFIX + orderId;
        Boolean isFirst = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, orderNo, IDEMPOTENT_EXPIRE_HOURS, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(isFirst)) {
            throw new BusinessException("请勿重复提交支付请求");
        }

        // 2. 创建支付宝支付请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        request.setReturnUrl(alipayProperties.getReturnUrl());

        // 3. 设置业务参数
        // bizContent 使用 JSON 格式
        String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\"," +
                "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}",
                orderNo, totalAmount.toPlainString(), subject);
        request.setBizContent(bizContent);

        // 4. 调用 SDK 生成表单
        try {
            String form = alipayClient.pageExecute(request).getBody();
            logger.info("支付宝下单成功：orderNo={}, amount={}", orderNo, totalAmount);
            return form;
        } catch (AlipayApiException e) {
            // 下单失败，清除幂等标记
            stringRedisTemplate.delete(idempotentKey);
            logger.error("支付宝下单失败：orderNo={}, error={}", orderNo, e.getErrMsg());
            throw new BusinessException("支付下单失败，请稍后重试");
        }
    }

    @Override
    public String handleAlipayNotify(Map<String, String> params) {
        // 1. 验证签名
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );

            if (!signVerified) {
                logger.error("支付宝回调验签失败：params={}", params);
                return "failure";
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝回调验签异常：{}", e.getMessage(), e);
            return "failure";
        }

        // 2. 解析回调参数
        String tradeStatus = params.get("trade_status");
        String outTradeNo = params.get("out_trade_no");      // 商户订单号
        String tradeNo = params.get("trade_no");             // 支付宝交易号
        String totalAmount = params.get("total_amount");     // 实付金额

        logger.info("收到支付宝回调：outTradeNo={}, tradeNo={}, status={}, amount={}",
                outTradeNo, tradeNo, tradeStatus, totalAmount);

        // 3. 处理交易状态
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            // 支付成功，查找订单并更新状态
            Order order = findOrderByOrderNo(outTradeNo);
            if (order == null) {
                logger.error("支付宝回调：订单不存在，outTradeNo={}", outTradeNo);
                return "failure";
            }

            // 幂等性检查：只有待支付的订单才处理
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.PAID);
                order.setPayTime(java.time.LocalDateTime.now());
                orderService.updateById(order);

                // 发送支付成功通知
                messageProducer.sendPaymentNotification(order.getId(), "PAID");

                logger.info("支付宝回调处理成功：orderNo={}, tradeNo={}", outTradeNo, tradeNo);
            } else {
                logger.info("支付宝回调：订单已处理过，orderNo={}, status={}", outTradeNo, order.getStatus());
            }
        }

        return "success";
    }

    @Override
    public String queryAlipayTrade(String orderNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", orderNo));

        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                logger.info("查询支付宝交易成功：orderNo={}, status={}", orderNo, response.getTradeStatus());
                return response.getTradeStatus();
            } else {
                logger.error("查询支付宝交易失败：orderNo={}, code={}, msg={}",
                        orderNo, response.getCode(), response.getMsg());
                return null;
            }
        } catch (AlipayApiException e) {
            logger.error("查询支付宝交易异常：orderNo={}, error={}", orderNo, e.getErrMsg());
            return null;
        }
    }

    @Override
    public boolean alipayRefund(String orderNo, BigDecimal refundAmount, String refundReason) {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"refund_amount\":\"%s\",\"refund_reason\":\"%s\"}",
                orderNo, refundAmount.toPlainString(), refundReason);
        request.setBizContent(bizContent);

        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                logger.info("支付宝退款成功：orderNo={}, refundAmount={}", orderNo, refundAmount);
                return true;
            } else {
                logger.error("支付宝退款失败：orderNo={}, code={}, msg={}",
                        orderNo, response.getCode(), response.getMsg());
                return false;
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝退款异常：orderNo={}, error={}", orderNo, e.getErrMsg());
            return false;
        }
    }

    @Override
    public boolean handleAlipayReturn(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );
        } catch (AlipayApiException e) {
            logger.error("支付宝同步跳转验签异常：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 根据订单编号查找订单
     */
    private Order findOrderByOrderNo(String orderNo) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order> query =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        query.eq(Order::getOrderNo, orderNo);
        return orderService.getOne(query);
    }
}
