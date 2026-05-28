package com.concert.service;

import com.concert.dto.response.DashboardRevenueResponse;
import com.concert.dto.response.DashboardSalesResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * @description: 仪表盘服务接口
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public interface DashboardService {

    /**
     * 销售统计概览
     *
     * @return 销售统计
     */
    DashboardSalesResponse getSalesOverview();

    /**
     * 收入报表（按日期范围统计）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 收入报表列表
     */
    List<DashboardRevenueResponse> getRevenueReport(LocalDate startDate, LocalDate endDate);
}
