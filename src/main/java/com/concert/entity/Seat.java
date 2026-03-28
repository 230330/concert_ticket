package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 座位表实体类
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
     * 排号
     */
    @TableField("row_num")
    private Integer rowNum;

    /**
     * 列号
     */
    @TableField("col_num")
    private Integer colNum;

    /**
     * 座位编号（如：A1-01）
     */
    @TableField("seat_no")
    private String seatNo;

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
