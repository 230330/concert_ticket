package com.concert.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @description:    管理端-订单详情响应（含用户信息）
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Data
public class AdminOrderResponse {

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
     * 用户手机号
     */
    private String userPhone;

    /**
     * 用户昵称
     */
    private String userNickname;

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
     * 座位详情
     */
    private List<OrderResponse.SeatDetail> seats;
}
