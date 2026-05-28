package com.concert.service.concert.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.dto.response.ConcertListResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.*;
import com.concert.enums.ConcertStatus;
import com.concert.enums.ShowStatus;
import com.concert.service.*;
import com.concert.service.concert.ConcertResponseAssembler;
import com.concert.service.concert.ConcertShowService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 演唱会场次服务实现
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Service
public class ConcertShowServiceImpl implements ConcertShowService {

    @Resource
    private ConcertService concertService;

    @Resource
    private ConcertArtistService concertArtistService;

    @Resource
    private ArtistService artistService;

    @Resource
    @Lazy
    private ShowService showService;

    @Resource
    private VenueService venueService;

    @Resource
    private TicketTypeService ticketTypeService;

    @Override
    public PageResponse<ConcertListResponse> getUpcomingConcerts(Integer page, Integer size) {
        LocalDateTime now = LocalDateTime.now();

        // 查询所有"即将开始"的场次
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.gt(Show::getShowTime, now)
                .in(Show::getStatus, ShowStatus.NOT_ON_SALE, ShowStatus.ON_SALE)
                .orderByAsc(Show::getShowTime);
        List<Show> upcomingShows = showService.list(showQuery);

        List<Long> concertIds = upcomingShows.stream()
                .map(Show::getConcertId)
                .distinct()
                .collect(Collectors.toList());

        if (concertIds.isEmpty()) {
            return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
        }

        Page<Concert> concertPage = new Page<>(page, size);
        LambdaQueryWrapper<Concert> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Concert::getId, concertIds)
                .in(Concert::getStatus, ConcertStatus.NOT_STARTED, ConcertStatus.IN_PROGRESS);
        concertService.page(concertPage, queryWrapper);

        List<ConcertListResponse> responseList = ConcertResponseAssembler.convertToConcertListResponse(
                concertPage.getRecords(), new ConcertDependencyHolderImpl());

        return new PageResponse<>(concertPage.getCurrent(), concertPage.getSize(),
                concertPage.getTotal(), responseList);
    }

    /**
     * 依赖持有者实现
     */
    private class ConcertDependencyHolderImpl implements ConcertResponseAssembler.ConcertDependencyHolder {
        @Override
        public List<ConcertArtist> getConcertArtists(List<Long> concertIds) {
            LambdaQueryWrapper<ConcertArtist> caQuery = new LambdaQueryWrapper<>();
            caQuery.in(ConcertArtist::getConcertId, concertIds);
            return concertArtistService.list(caQuery);
        }

        @Override
        public Map<Long, String> getArtistNames(List<Long> artistIds) {
            return artistService.listByIds(artistIds).stream()
                    .collect(Collectors.toMap(Artist::getId, Artist::getName));
        }

        @Override
        public List<Show> getShows(List<Long> concertIds) {
            LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
            showQuery.in(Show::getConcertId, concertIds)
                    .orderByAsc(Show::getShowTime);
            return showService.list(showQuery);
        }

        @Override
        public Map<Long, Venue> getVenueMap(List<Long> venueIds) {
            return venueService.listByIds(venueIds).stream()
                    .collect(Collectors.toMap(Venue::getId, v -> v));
        }

        @Override
        public Map<Long, BigDecimal> getShowMinPriceMap(List<Long> showIds) {
            LambdaQueryWrapper<TicketType> ttQuery = new LambdaQueryWrapper<>();
            ttQuery.in(TicketType::getShowId, showIds);
            List<TicketType> ticketTypes = ticketTypeService.list(ttQuery);

            return ticketTypes.stream()
                    .collect(Collectors.groupingBy(
                            TicketType::getShowId,
                            Collectors.collectingAndThen(
                                    Collectors.minBy(Comparator.comparing(TicketType::getPrice)),
                                    opt -> opt.map(TicketType::getPrice).orElse(null)
                            )
                    ));
        }
    }
}
