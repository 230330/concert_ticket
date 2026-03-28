package com.concert.controller;

import com.concert.common.Result;
import com.concert.config.security.LoginUser;
import com.concert.dto.request.CancelOrderRequest;
import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.request.PayOrderRequest;
import com.concert.dto.response.OrderResponse;
import com.concert.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<OrderResponse> createOrder(@RequestBody @Validated CreateOrderRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        try {
            OrderResponse response = orderService.createOrder(userId, request);
            return Result.success(response);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 支付订单（模拟）
     */
    @PostMapping("/pay")
    public Result<OrderResponse> payOrder(@RequestBody @Validated PayOrderRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        try {
            OrderResponse response = orderService.payOrder(userId, request.getOrderId());
            return Result.success(response);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    @PutMapping("/cancel")
    public Result<Void> cancelOrder(@RequestBody @Validated CancelOrderRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        try {
            orderService.cancelOrder(userId, request.getOrderId());
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{orderId}")
    public Result<OrderResponse> getOrderDetail(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        OrderResponse response = orderService.getOrderDetail(orderId);
        if (response == null) {
            return Result.error("订单不存在");
        }

        // 验证订单归属
        if (!response.getUserId().equals(userId)) {
            return Result.error("无权查看此订单");
        }

        return Result.success(response);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser) {
            return ((LoginUser) principal).getId();
        }
        return null;
    }
}
