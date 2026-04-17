package com.concert.dto.response;

import com.concert.enums.ConcertStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description:    管理端-用户信息响应
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Data
public class ConcertListResponse {

    /**
     * 演唱会ID
     */
    private Long id;

    /**
     * 演唱会名称
     */
    private String name;

    /**
     * 演唱会海报
     */
    private String poster;

    /**
     * 状态
     */
    private ConcertStatus status;

    /**
     * 艺人名称列表
     */
    private List<String> artistNames;

    /**
     * 最低票价
     */
    private String minPrice;

    /**
     * 城市
     */
    private String city;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 最近场次时间
     */
    private LocalDateTime nearestShowTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
