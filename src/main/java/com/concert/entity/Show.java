package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.concert.enums.ShowStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:    场次表实体类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Data
@TableName("`show`")
public class Show {

    /**
     * 场次ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 演唱会ID
     */
    @TableField("concert_id")
    private Long concertId;

    /**
     * 场馆ID
     */
    @TableField("venue_id")
    private Long venueId;

    /**
     * 演出时间
     */
    @TableField("show_time")
    private LocalDateTime showTime;

    /**
     * 开售时间
     */
    @TableField("sale_start_time")
    private LocalDateTime saleStartTime;

    /**
     * 停售时间
     */
    @TableField("sale_end_time")
    private LocalDateTime saleEndTime;

    /**
     * 状态
     */
    @TableField("status")
    private ShowStatus status;

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
