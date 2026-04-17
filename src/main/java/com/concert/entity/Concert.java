package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.concert.enums.ConcertStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:    演唱会表实体类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Data
@TableName("concert")
public class Concert {

    /**
     * 演唱会ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 演唱会名称
     */
    @TableField("name")
    private String name;

    /**
     * 演唱会海报
     */
    @TableField("poster")
    private String poster;

    /**
     * 演唱会描述
     */
    @TableField("description")
    private String description;

    /**
     * 状态
     */
    @TableField("status")
    private ConcertStatus status;

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
