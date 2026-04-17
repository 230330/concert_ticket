package com.concert.controller;

import com.concert.common.Result;
import com.concert.config.security.LoginUser;
import com.concert.dto.request.CancelOrderRequest;
import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.request.PayOrderRequest;
import com.concert.dto.request.RefundOrderRequest;
import com.concert.dto.response.OrderResponse;
import com.concert.dto.response.PageResponse;
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

        OrderResponse response = orderService.createOrder(userId, request);
        return Result.success(response);
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

        OrderResponse response = orderService.payOrder(userId, request.getOrderId());
        return Result.success(response);
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

        orderService.cancelOrder(userId, request.getOrderId());
        return Result.success();
    }

    /**
     * 退款订单
     */
    @PutMapping("/refund")
    public Result<OrderResponse> refundOrder(@RequestBody @Validated RefundOrderRequest request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        OrderResponse response = orderService.refundOrder(userId, request.getOrderId());
        return Result.success(response);
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
     * 我的订单列表（分页+状态筛选）
     *
     * @param status 订单状态（可选）：0-待支付，1-已支付，2-已取消，3-已退款，4-已完成
     * @param page   页码（默认1）
     * @param size   每页条数（默认10）
     */
    @GetMapping("/my")
    public Result<PageResponse<OrderResponse>> getMyOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        PageResponse<OrderResponse> response = orderService.getMyOrders(userId, status, page, size);
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
