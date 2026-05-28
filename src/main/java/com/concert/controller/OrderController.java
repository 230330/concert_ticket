package com.concert.controller;

import com.concert.common.Result;
import com.concert.dto.request.CancelOrderRequest;
import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.request.PayOrderRequest;
import com.concert.dto.request.RefundOrderRequest;
import com.concert.dto.response.OrderResponse;
import com.concert.dto.response.PageResponse;
import com.concert.service.OrderService;
import com.concert.utils.PageUtil;
import com.concert.utils.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: 订单相关接口
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Tag(name = "订单管理", description = "用户端订单相关接口，需要登录")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    @Operation(summary = "创建订单", description = "选择场次、票档和座位创建订单，需要登录")
    @PostMapping("/create")
    public Result<OrderResponse> createOrder(@RequestBody @Validated CreateOrderRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        OrderResponse response = orderService.createOrder(userId, request);
        return Result.success(response);
    }

    @Operation(summary = "支付订单", description = "模拟支付待支付订单，支付成功后生成取票码")
    @PostMapping("/pay")
    public Result<OrderResponse> payOrder(@RequestBody @Validated PayOrderRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        OrderResponse response = orderService.payOrder(userId, request.getOrderId());
        return Result.success(response);
    }

    @Operation(summary = "取消订单", description = "取消待支付的订单，库存将回滚")
    @PutMapping("/cancel")
    public Result<Void> cancelOrder(@RequestBody @Validated CancelOrderRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        orderService.cancelOrder(userId, request.getOrderId());
        return Result.success();
    }

    @Operation(summary = "退款订单", description = "对已支付订单申请退款，演出前48小时内不可退款")
    @PutMapping("/refund")
    public Result<OrderResponse> refundOrder(@RequestBody @Validated RefundOrderRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        OrderResponse response = orderService.refundOrder(userId, request.getOrderId());
        return Result.success(response);
    }

    @Operation(summary = "获取订单详情", description = "根据订单ID获取订单详情，含座位信息")
    @GetMapping("/{orderId}")
    public Result<OrderResponse> getOrderDetail(
            @Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        OrderResponse response = orderService.getOrderDetail(orderId);
        if (response == null) {
            return Result.error("订单不存在");
        }

        if (!response.getUserId().equals(userId)) {
            return Result.error("无权查看此订单");
        }

        return Result.success(response);
    }

    @Operation(summary = "我的订单列表", description = "分页查询当前用户的订单列表，支持状态筛选")
    @GetMapping("/my")
    public Result<PageResponse<OrderResponse>> getMyOrders(
            @Parameter(description = "订单状态：0-待支付，1-已支付，2-已取消，3-已退款，4-已完成") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数，默认10") @RequestParam(defaultValue = "10") Integer size) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        int[] params = PageUtil.validate(page, size);
        PageResponse<OrderResponse> response = orderService.getMyOrders(userId, status, params[0], params[1]);
        return Result.success(response);
    }
}
