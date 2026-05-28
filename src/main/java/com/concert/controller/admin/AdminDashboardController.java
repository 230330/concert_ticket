package com.concert.controller.admin;

import com.concert.common.Result;
import com.concert.dto.response.DashboardRevenueResponse;
import com.concert.dto.response.DashboardSalesResponse;
import com.concert.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * @description:    管理员仪表盘控制器
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Tag(name = "管理端-数据看板", description = "销售统计、收入报表等数据看板接口")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Resource
    private DashboardService dashboardService;

    /**
     * 销售统计概览
     */
    @Operation(summary = "销售统计概览", description = "获取订单统计、销售额、退款额、活跃演出数等汇总数据")
    @GetMapping("/sales")
    public Result<DashboardSalesResponse> salesOverview() {
        DashboardSalesResponse response = dashboardService.getSalesOverview();
        return Result.success(response);
    }

    /**
     * 收入报表（按日期范围统计）
     */
    @Operation(summary = "收入报表", description = "按日期范围统计每日订单数、销售额、退款额、售票数")
    @GetMapping("/revenue")
    public Result<List<DashboardRevenueResponse>> revenueReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<DashboardRevenueResponse> result = dashboardService.getRevenueReport(startDate, endDate);
        return Result.success(result);
    }
}
