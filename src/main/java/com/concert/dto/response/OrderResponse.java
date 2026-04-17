package com.concert.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @description:    订单响应
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Data
public class OrderResponse {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 场次ID
     */
    private Long showId;

    /**
     * 演唱会名称
     */
    private String concertName;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 演出时间
     */
    private LocalDateTime showTime;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 订单状态：0-待支付，1-已支付，2-已取消，3-已退款，4-已完成
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 取票码
     */
    private String pickupCode;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 座位列表
     */
    private List<SeatDetail> seats;

    /**
     * 座位详情
     */
    @Data
    public static class SeatDetail {
        /**
         * 座位ID
         */
        private Long seatId;

        /**
         * 座位编号
         */
        private String seatNo;

        /**
         * 区域名称
         */
        private String areaName;

        /**
         * 票档名称
         */
        private String ticketTypeName;

        /**
         * 票价
         */
        private BigDecimal price;
    }

    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        if (status == null) {
            return "";
        }
        switch (status) {
            case 0:
                return "待支付";
            case 1:
                return "已支付";
            case 2:
                return "已取消";
            case 3:
                return "已退款";
            case 4:
                return "已完成";
            default:
                return "未知";
        }
    }
}
