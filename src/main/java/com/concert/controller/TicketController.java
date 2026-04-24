package com.concert.controller;

import com.concert.common.Result;
import com.concert.entity.Order;
import com.concert.service.OrderService;
import com.concert.utils.JwtUtil;
import com.concert.utils.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description:    税票相关控制器
 * @author: hzf
 * @date: 2026年04月24日 11:26
 * @version: 1.0
 */
@RestController
@RequestMapping("/api/ticket")
public class TicketController {
    @Resource
    private OrderService orderService;

    /**
     * 获取当前用户的取票码列表（通过手机号）
     */
    @GetMapping("/my-codes")
    public Result<List<Order>> getMyTicketCodes() {
        // 从Security上下文中获取当前登录用户的手机号
        String phone = SecurityUtil.getCurrentUserPhone();
        List<Order> orders = orderService.getPaidOrdersWithTicketCodeByPhone(phone);
        return Result.success(orders);
    }

    /**
     * 核销取票码（线下工作人员使用）
     */
    @PostMapping("/verify")
    public Result<Boolean> verifyTicketCode(@RequestParam String ticketCode) {
        boolean success = orderService.verifyTicketCode(ticketCode);
        if (success) {
            return Result.success(true);
        } else {
            return Result.error("取票码无效或已使用");
        }
    }
}
