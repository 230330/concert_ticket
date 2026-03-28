package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 演唱会表实体类
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
     * 状态：0-未开始，1-进行中，2-已结束，3-已取消
     */
    @TableField("status")
    private Integer status;

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
