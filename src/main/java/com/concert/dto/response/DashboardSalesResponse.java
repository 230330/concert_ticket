package com.concert.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @description:    管理端-数据看板-销售统计响应
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Data
public class DashboardSalesResponse {

    /**
     * 总订单数
     */
    private Long totalOrders;

    /**
     * 已支付订单数
     */
    private Long paidOrders;

    /**
     * 已取消订单数
     */
    private Long cancelledOrders;

    /**
     * 已退款订单数
     */
    private Long refundedOrders;

    /**
     * 已完成订单数
     */
    private Long completedOrders;

    /**
     * 总销售额（已支付+已完成）
     */
    private BigDecimal totalRevenue;

    /**
     * 实际收入（已完成）
     */
    private BigDecimal actualRevenue;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 总售票数
     */
    private Long totalTickets;

    /**
     * 活跃演唱会数量
     */
    private Long activeConcerts;

    /**
     * 活跃场次数量
     */
    private Long activeShows;

    /**
     * 注册用户数
     */
    private Long totalUsers;
}
