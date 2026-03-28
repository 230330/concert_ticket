package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 座位区域表实体类
 */
@Data
@TableName("seat_area")
public class SeatArea {

    /**
     * 区域ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 场馆ID
     */
    @TableField("venue_id")
    private Long venueId;

    /**
     * 区域名称（如：VIP区、A区、B区）
     */
    @TableField("name")
    private String name;

    /**
     * 区域容量
     */
    @TableField("capacity")
    private Integer capacity;

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
