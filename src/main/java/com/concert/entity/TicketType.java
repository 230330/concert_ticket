package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @description:    系统用户角色表实体类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Data
@TableName("ticket_type")
public class TicketType {

    /**
     * 票档ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 场次ID
     */
    @TableField("show_id")
    private Long showId;

    /**
     * 区域ID
     */
    @TableField("area_id")
    private Long areaId;

    /**
     * 票档名称（如：内场VIP、看台A区）
     */
    @TableField("name")
    private String name;

    /**
     * 票价
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 总库存
     */
    @TableField("total_stock")
    private Integer totalStock;

    /**
     * 已售票
     */
    @TableField("sold_stock")
    private Integer soldStock;

    /**
     * 可用库存
     */
    @TableField("available_stock")
    private Integer availableStock;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
