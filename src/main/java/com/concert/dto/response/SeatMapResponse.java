package com.concert.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @description:    座位图响应
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Data
public class SeatMapResponse {

    /**
     * 场次ID
     */
    private Long showId;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 区域列表
     */
    private List<AreaInfo> areas;

    /**
     * 区域信息
     */
    @Data
    public static class AreaInfo {
        /**
         * 区域ID
         */
        private Long id;

        /**
         * 区域名称
         */
        private String name;

        /**
         * 区域容量
         */
        private Integer capacity;

        /**
         * 票价
         */
        private BigDecimal price;

        /**
         * 票档ID
         */
        private Long ticketTypeId;

        /**
         * 票档名称
         */
        private String ticketTypeName;

        /**
         * 座位列表（按行分组）
         */
        private List<RowInfo> rows;
    }

    /**
     * 行信息
     */
    @Data
    public static class RowInfo {
        /**
         * 排号
         */
        private String rowCode;

        /**
         * 座位列表
         */
        private List<SeatInfo> seats;
    }

    /**
     * 座位信息
     */
    @Data
    public static class SeatInfo {
        /**
         * 座位ID
         */
        private Long id;

        /**
         * 排号
         */
        private String rowNum;

        /**
         * 列号
         */
        private Integer colNum;

        /**
         * 座位编号
         */
        private String seatNo;

        /**
         * 是否已售：true-已售，false-可售
         */
        private Boolean sold;
    }
}
