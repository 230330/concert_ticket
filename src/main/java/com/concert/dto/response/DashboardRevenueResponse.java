package com.concert.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @description:    管理端-数据看板-收入报表响应（按日期维度）
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Data
public class DashboardRevenueResponse {

    /**
     * 日期（yyyy-MM-dd）
     */
    private String date;

    /**
     * 当日订单数
     */
    private Long orderCount;

    /**
     * 当日销售额
     */
    private BigDecimal revenue;

    /**
     * 当日售票数
     */
    private Long ticketCount;

    /**
     * 当日退款金额
     */
    private BigDecimal refundAmount;
}
