package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description:    座位表实体类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Data
@TableName("seat")
public class Seat {

    /**
     * 座位ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属区域ID
     */
    @TableField("area_id")
    private Long areaId;

    /**
     * 行号
     */
    @TableField("row_code")
    private String rowCode;

    /**
     * 列号
     */
    @TableField("col_num")
    private Integer colNum;

    /**
     * 座位编号（如：A1-01）
     */
    @TableField("seat_code")
    private String seatCode;

    /**
     * 座位状态（如：0空闲、1占用中、2锁定中）
     */
    @TableField("status")
    private Integer seatStatus;

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
