package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @description:    订单座位表实体类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Data
@TableName("order_seat")
public class OrderSeat {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 场次ID（用于唯一索引防止并发）
     */
    @TableField("show_id")
    private Long showId;

    /**
     * 座位ID
     */
    @TableField("seat_id")
    private Long seatId;

    /**
     * 票档ID
     */
    @TableField("ticket_type_id")
    private Long ticketTypeId;

    /**
     * 票价（下单时的价格快照）
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
