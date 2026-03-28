package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 演唱会-艺人关联表实体类
 */
@Data
@TableName("concert_artist")
public class ConcertArtist {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 演唱会ID
     */
    @TableField("concert_id")
    private Long concertId;

    /**
     * 艺人ID
     */
    @TableField("artist_id")
    private Long artistId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
