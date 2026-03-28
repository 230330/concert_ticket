package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场馆表实体类
 */
@Data
@TableName("venue")
public class Venue {

    /**
     * 场馆ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 场馆名称
     */
    @TableField("name")
    private String name;

    /**
     * 所在城市
     */
    @TableField("city")
    private String city;

    /**
     * 详细地址
     */
    @TableField("address")
    private String address;

    /**
     * 场馆容量
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
