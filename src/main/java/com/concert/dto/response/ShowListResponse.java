package com.concert.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 场次列表响应
 */
@Data
public class ShowListResponse {

    /**
     * 场次ID
     */
    private Long id;

    /**
     * 演唱会ID
     */
    private Long concertId;

    /**
     * 演唱会名称
     */
    private String concertName;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 城市
     */
    private String city;

    /**
     * 地址
     */
    private String address;

    /**
     * 演出时间
     */
    private LocalDateTime showTime;

    /**
     * 开售时间
     */
    private LocalDateTime saleStartTime;

    /**
     * 停售时间
     */
    private LocalDateTime saleEndTime;

    /**
     * 状态：0-未开售，1-售票中，2-已售罄，3-已结束，4-已取消
     */
    private Integer status;

    /**
     * 票档列表
     */
    private List<TicketTypeInfo> ticketTypes;

    /**
     * 票档信息
     */
    @Data
    public static class TicketTypeInfo {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer totalStock;
        private Integer availableStock;
        private Long areaId;
        private String areaName;
    }
}
