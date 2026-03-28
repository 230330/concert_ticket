package com.concert.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 演唱会详情响应
 */
@Data
public class ConcertDetailResponse {

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
     * 演唱会描述
     */
    private String description;

    /**
     * 状态：0-未开始，1-进行中，2-已结束，3-已取消
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 艺人列表
     */
    private List<ArtistInfo> artists;

    /**
     * 场次列表
     */
    private List<ShowInfo> shows;

    /**
     * 艺人信息
     */
    @Data
    public static class ArtistInfo {
        private Long id;
        private String name;
        private String avatar;
        private String description;
    }

    /**
     * 场次信息
     */
    @Data
    public static class ShowInfo {
        private Long id;
        private LocalDateTime showTime;
        private LocalDateTime saleStartTime;
        private LocalDateTime saleEndTime;
        private Integer status;
        private String venueName;
        private String city;
        private String address;
    }
}
