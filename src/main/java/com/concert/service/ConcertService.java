package com.concert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concert.dto.response.ConcertDetailResponse;
import com.concert.dto.response.ConcertListResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Concert;

import java.time.LocalDate;

/**
 * 演唱会服务接口
 */
public interface ConcertService extends IService<Concert> {

    /**
     * 热门演出列表（分页）
     *
     * @param page 页码
     * @param size 每页条数
     * @param sort 排序方式：time-按时间，default-默认
     * @return 分页响应
     */
    PageResponse<ConcertListResponse> getHotConcerts(Integer page, Integer size, String sort);

    /**
     * 即将开始演出列表（分页）
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页响应
     */
    PageResponse<ConcertListResponse> getUpcomingConcerts(Integer page, Integer size);

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
                                                      LocalDate startDate, LocalDate endDate,
                                                      Integer page, Integer size);

    /**
     * 演唱会详情
     *
     * @param id 演唱会ID
     * @return 详情响应
     */
    ConcertDetailResponse getConcertDetail(Long id);
}
