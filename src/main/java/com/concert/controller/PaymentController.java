package com.concert.controller;

import com.concert.annotation.RateLimit;
import com.concert.common.Result;
import com.concert.config.AlipayProperties;
import com.concert.entity.Order;
import com.concert.enums.OrderStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.ForbiddenException;
import com.concert.exception.NotFoundException;
import com.concert.service.OrderService;
import com.concert.service.PaymentService;
import com.concert.utils.SecurityUtil;
import com.alipay.api.internal.util.AlipaySignature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器
 * 处理支付宝支付相关接口
 *
 * @author hzf
 * @date 2026/05/28
 */
@Tag(name = "支付管理", description = "支付宝支付相关接口")
@RestController
@RequestMapping("/api/pay")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Resource
    private PaymentService paymentService;

    @Resource
    private OrderService orderService;

    @Resource
    private AlipayProperties alipayProperties;

    /**
     * 发起支付宝支付
     * 返回支付宝支付页面表单HTML，前端直接提交即可跳转到支付宝
     */
    @Operation(summary = "发起支付宝支付", description = "创建支付宝支付订单，返回支付页面表单HTML")
    @PostMapping("/alipay/create")
    @RateLimit(key = "pay:alipay", count = 5, period = 60)
    public Result<String> createAlipayPayment(@RequestParam Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        // 1. 查询订单
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }

        // 2. 验证订单归属
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作此订单");
        }

        // 3. 验证订单状态
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("订单状态异常，无法支付");
        }

        // 4. 调用支付服务
        String subject = "演唱会门票-" + order.getOrderNo();
        String form = paymentService.createAlipayOrder(orderId, order.getOrderNo(), order.getTotalAmount(), subject);

        return Result.success(form);
    }

    /**
     * 支付宝异步通知回调
     * 由支付宝服务器主动调用，不需要用户登录
     */
    @Operation(summary = "支付宝异步通知", description = "支付宝支付结果异步通知回调地址")
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        // 1. 获取支付宝回调参数
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append(i == values.length - 1 ? values[i] : values[i] + ",");
            }
            params.put(name, valueStr.toString());
        }

        logger.info("收到支付宝异步通知：{}", params);

        // 2. 处理通知
        return paymentService.handleAlipayNotify(params);
    }

    /**
     * 支付宝同步跳转
     * 用户支付完成后跳转回商户网站
     */
    @Operation(summary = "支付宝同步跳转", description = "支付完成后跳转回商户网站")
    @GetMapping("/alipay/return")
    public Result<String> alipayReturn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append(i == values.length - 1 ? values[i] : values[i] + ",");
            }
            params.put(name, valueStr.toString());
        }

        boolean verified = paymentService.handleAlipayReturn(params);
        if (verified) {
            return Result.success("支付成功");
        } else {
            return Result.error("支付验签失败");
        }
    }

    /**
     * 查询支付状态
     */
    @Operation(summary = "查询支付状态", description = "查询支付宝交易状态，用于前端轮询")
    @GetMapping("/alipay/status")
    @RateLimit(key = "pay:status", count = 20, period = 60)
    public Result<String> queryPayStatus(@RequestParam String orderNo) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        String status = paymentService.queryAlipayTrade(orderNo);
        return Result.success(status);
    }
}
