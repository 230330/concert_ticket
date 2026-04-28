package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:    艺人表实体类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Data
@TableName("artist")
public class Artist {

    /**
     * 艺人ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 艺人名称
     */
    @TableField("name")
    private String name;

    /**
     * 艺人头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 艺人简介
     */
    @TableField("intro")
    private String description;

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
