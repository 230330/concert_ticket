package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.dto.response.ConcertDetailResponse;
import com.concert.dto.response.ConcertListResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.Concert;
import com.concert.mapper.ConcertMapper;
import com.concert.service.ConcertService;
import com.concert.service.concert.ConcertCoreService;
import com.concert.service.concert.ConcertSearchService;
import com.concert.service.concert.ConcertShowService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * @description:    演出信息服务实现类（门面模式，委托给子服务）
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Service
public class ConcertServiceImpl extends ServiceImpl<ConcertMapper, Concert> implements ConcertService {

    @Resource
    @Lazy
    private ConcertCoreService concertCoreService;

    @Resource
    @Lazy
    private ConcertShowService concertShowService;

    @Resource
    @Lazy
    private ConcertSearchService concertSearchService;

    @Override
    public PageResponse<ConcertListResponse> getHotConcerts(Integer page, Integer size, String sort) {
        return concertSearchService.getHotConcerts(page, size, sort);
    }

    @Override
    public PageResponse<ConcertListResponse> getUpcomingConcerts(Integer page, Integer size) {
        return concertShowService.getUpcomingConcerts(page, size);
    }

    @Override
    public PageResponse<ConcertListResponse> searchConcerts(String keyword, String city, String artistName,
                                                             LocalDate startDate, LocalDate endDate,
                                                             Integer page, Integer size) {
        return concertSearchService.searchConcerts(keyword, city, artistName, startDate, endDate, page, size);
    }

    @Override
    public ConcertDetailResponse getConcertDetail(Long id) {
        return concertCoreService.getConcertDetail(id);
    }
}
