package com.concert.controller;

import com.concert.annotation.RateLimit;
import com.concert.common.Result;
import com.concert.entity.Order;
import com.concert.service.OrderService;
import com.concert.utils.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: 取票/核销相关控制器
 * @author: hzf
 * @date: 2026-04-24
 */
@Tag(name = "取票核销", description = "取票码查看、核销等接口")
@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    @Resource
    private OrderService orderService;

    /**
     * 获取当前用户的取票码列表（通过手机号）
     */
    @Operation(summary = "我的取票码", description = "获取当前登录用户的取票码列表，包含已支付和已完成的订单")
    @GetMapping("/my-codes")
    public Result<List<Order>> getMyTicketCodes() {
        String phone = SecurityUtil.getCurrentUserPhone();
        List<Order> orders = orderService.getPaidOrdersWithTicketCodeByPhone(phone);
        return Result.success(orders);
    }

    /**
     * 核销取票码（管理员/工作人员使用）
     * 核销成功后订单状态变更为"已完成"
     */
    @Operation(summary = "核销取票码", description = "管理员/工作人员核销取票码，核销成功后订单变为已完成状态")
    @PostMapping("/verify")
    @RateLimit(key = "ticket:verify", count = 20, period = 60)
    public Result<Boolean> verifyTicketCode(@RequestParam String pickupCode) {
        if (pickupCode == null || pickupCode.trim().isEmpty()) {
            return Result.error("取票码不能为空");
        }

        String operatorId = SecurityUtil.getCurrentUserId() != null
                ? SecurityUtil.getCurrentUserId().toString() : "unknown";
        logger.info("核销取票码请求，取票码：{}，操作人ID：{}", pickupCode, operatorId);

        boolean success = orderService.verifyTicketCode(pickupCode.trim());
        if (success) {
            logger.info("核销成功，取票码：{}，操作人ID：{}", pickupCode, operatorId);
            return Result.success(true);
        } else {
            logger.warn("核销失败（取票码无效或已使用），取票码：{}，操作人ID：{}", pickupCode, operatorId);
            return Result.error("取票码无效或已使用");
        }
    }
}
