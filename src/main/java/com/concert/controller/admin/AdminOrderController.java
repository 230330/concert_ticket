package com.concert.controller.admin;

import com.concert.common.Result;
import com.concert.dto.response.AdminOrderResponse;
import com.concert.dto.response.PageResponse;
import com.concert.enums.OrderStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.NotFoundException;
import com.concert.service.OrderService;
import com.concert.utils.PageUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description:    管理员订单管理
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Tag(name = "管理端-订单管理", description = "管理员订单列表、详情、退款等接口")
@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    @Resource
    private OrderService orderService;

    /**
     * 分页查询订单列表
     */
    @Operation(summary = "分页查询订单列表", description = "管理员分页查询订单列表，支持状态、订单号、用户ID筛选")
    @GetMapping("/list")
    public Result<PageResponse<AdminOrderResponse>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId) {

        int[] params = PageUtil.validate(page, size);
        PageResponse<AdminOrderResponse> pageResponse = orderService.listOrdersForAdmin(
                params[0], params[1], status, orderNo, userId);
        return Result.success(pageResponse);
    }

    /**
     * 查询订单详情
     */
    @Operation(summary = "查询订单详情", description = "管理员查询订单详细信息，含座位详情和用户信息")
    @GetMapping("/{id}")
    public Result<AdminOrderResponse> detail(@PathVariable Long id) {
        AdminOrderResponse resp = orderService.getOrderDetailForAdmin(id);
        return Result.success(resp);
    }

    /**
     * 管理员退款（不受退款时限限制）
     */
    @Operation(summary = "管理员退款", description = "管理员强制退款，不受演出前48小时退款时限限制")
    @PutMapping("/{id}/refund")
    public Result<Void> adminRefund(@PathVariable Long id) {
        orderService.adminRefundOrder(id);
        return Result.success();
    }

    /**
     * 管理员取消订单
     */
    @Operation(summary = "管理员取消订单", description = "管理员取消待支付订单，库存将回滚")
    @PutMapping("/{id}/cancel")
    public Result<Void> adminCancel(@PathVariable Long id) {
        orderService.cancelExpiredOrder(id);
        return Result.success();
    }
}
