package com.concert.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * @description:    演唱会请求参数
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Data
public class ShowRequest {

    /**
     * 演唱会ID
     */
    @NotNull(message = "演唱会ID不能为空")
    private Long concertId;

    /**
     * 场馆ID
     */
    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    /**
     * 演出时间
     */
    @NotNull(message = "演出时间不能为空")
    private LocalDateTime showTime;

    /**
     * 开售时间
     */
    @NotNull(message = "开售时间不能为空")
    private LocalDateTime saleStartTime;

    /**
     * 停售时间
     */
    @NotNull(message = "停售时间不能为空")
    private LocalDateTime saleEndTime;

    /**
     * 状态：0-未开售，1-售票中，2-已售罄，3-已结束，4-已取消
     */
    private Integer status;
}
