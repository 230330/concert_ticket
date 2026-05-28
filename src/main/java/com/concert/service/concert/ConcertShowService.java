package com.concert.service.concert;

import com.concert.dto.response.ConcertListResponse;
import com.concert.dto.response.PageResponse;

/**
 * @description: 演唱会场次服务
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public interface ConcertShowService {

    /**
     * 即将开始演出列表（分页）
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页响应
     */
    PageResponse<ConcertListResponse> getUpcomingConcerts(Integer page, Integer size);
}
