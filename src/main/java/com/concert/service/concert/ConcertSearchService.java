package com.concert.service.concert;

import com.concert.dto.response.ConcertListResponse;
import com.concert.dto.response.PageResponse;

import java.time.LocalDate;

/**
 * @description: 演唱会搜索推荐服务
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public interface ConcertSearchService {

    /**
     * 热门演出列表（分页）
     *
     * @param page 页码
     * @param size 每页条数
     * @param sort 排序方式
     * @return 分页响应
     */
    PageResponse<ConcertListResponse> getHotConcerts(Integer page, Integer size, String sort);

    /**
     * 搜索演唱会
     *
     * @param keyword    关键词
     * @param city       城市
     * @param artistName 艺人名称
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param page       页码
     * @param size       每页条数
     * @return 分页响应
     */
    PageResponse<ConcertListResponse> searchConcerts(String keyword, String city, String artistName,
                                                      LocalDate startDate, LocalDate endDate, String sort,
                                                      Integer page, Integer size);
}
