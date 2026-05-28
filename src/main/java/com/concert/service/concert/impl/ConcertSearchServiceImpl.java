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
import com.concert.service.concert.ConcertSearchService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 演唱会搜索推荐服务实现
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Service
public class ConcertSearchServiceImpl implements ConcertSearchService {

    @Resource
    private ConcertService concertService;

    @Resource
    private ConcertArtistService concertArtistService;

    @Resource
    private ArtistService artistService;

    @Resource
    private ShowService showService;

    @Resource
    private VenueService venueService;

    @Resource
    private TicketTypeService ticketTypeService;

    @Override
    public PageResponse<ConcertListResponse> getHotConcerts(Integer page, Integer size, String sort) {
        Page<Concert> concertPage = new Page<>(page, size);

        LambdaQueryWrapper<Concert> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Concert::getStatus, ConcertStatus.NOT_STARTED, ConcertStatus.IN_PROGRESS);

        if ("time".equals(sort)) {
            queryWrapper.orderByDesc(Concert::getCreateTime);
        } else {
            queryWrapper.orderByDesc(Concert::getId);
        }

        concertService.page(concertPage, queryWrapper);

        List<ConcertListResponse> responseList = ConcertResponseAssembler.convertToConcertListResponse(
                concertPage.getRecords(), new ConcertDependencyHolderImpl());

        return new PageResponse<>(concertPage.getCurrent(), concertPage.getSize(),
                concertPage.getTotal(), responseList);
    }

    @Override
    public PageResponse<ConcertListResponse> searchConcerts(String keyword, String city, String artistName,
                                                             LocalDate startDate, LocalDate endDate,
                                                             Integer page, Integer size) {
        LambdaQueryWrapper<Concert> concertQuery = new LambdaQueryWrapper<>();
        concertQuery.in(Concert::getStatus, ConcertStatus.NOT_STARTED, ConcertStatus.IN_PROGRESS);

        if (keyword != null && !keyword.trim().isEmpty()) {
            concertQuery.like(Concert::getName, keyword.trim());
        }

        if (artistName != null && !artistName.trim().isEmpty()) {
            LambdaQueryWrapper<Artist> artistQuery = new LambdaQueryWrapper<>();
            artistQuery.like(Artist::getName, artistName.trim());
            List<Artist> matchedArtists = artistService.list(artistQuery);

            if (matchedArtists.isEmpty()) {
                return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
            }

            List<Long> artistIds = matchedArtists.stream()
                    .map(Artist::getId)
                    .collect(Collectors.toList());

            LambdaQueryWrapper<ConcertArtist> caQuery = new LambdaQueryWrapper<>();
            caQuery.in(ConcertArtist::getArtistId, artistIds);
            List<ConcertArtist> concertArtists = concertArtistService.list(caQuery);

            List<Long> concertIdsByArtist = concertArtists.stream()
                    .map(ConcertArtist::getConcertId)
                    .distinct()
                    .collect(Collectors.toList());

            if (concertIdsByArtist.isEmpty()) {
                return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
            }

            concertQuery.in(Concert::getId, concertIdsByArtist);
        }

        boolean needShowFilter = (city != null && !city.trim().isEmpty())
                || startDate != null || endDate != null;

        if (needShowFilter) {
            LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
            showQuery.in(Show::getStatus, ShowStatus.NOT_ON_SALE, ShowStatus.ON_SALE);

            if (startDate != null) {
                showQuery.ge(Show::getShowTime, startDate.atStartOfDay());
            }
            if (endDate != null) {
                showQuery.le(Show::getShowTime, endDate.atTime(LocalTime.MAX));
            }

            List<Show> shows = showService.list(showQuery);

            if (shows.isEmpty()) {
                return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
            }

            if (city != null && !city.trim().isEmpty()) {
                List<Long> venueIds = shows.stream()
                        .map(Show::getVenueId)
                        .distinct()
                        .collect(Collectors.toList());

                LambdaQueryWrapper<Venue> venueQuery = new LambdaQueryWrapper<>();
                venueQuery.in(Venue::getId, venueIds)
                        .eq(Venue::getCity, city.trim());
                List<Venue> matchedVenues = venueService.list(venueQuery);

                if (matchedVenues.isEmpty()) {
                    return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
                }

                Set<Long> matchedVenueIds = matchedVenues.stream()
                        .map(Venue::getId)
                        .collect(Collectors.toSet());

                shows = shows.stream()
                        .filter(show -> matchedVenueIds.contains(show.getVenueId()))
                        .collect(Collectors.toList());

                if (shows.isEmpty()) {
                    return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
                }
            }

            List<Long> concertIdsByShow = shows.stream()
                    .map(Show::getConcertId)
                    .distinct()
                    .collect(Collectors.toList());

            concertQuery.in(Concert::getId, concertIdsByShow);
        }

        concertQuery.orderByDesc(Concert::getId);
        Page<Concert> concertPage = new Page<>(page, size);
        concertService.page(concertPage, concertQuery);

        List<ConcertListResponse> responseList = ConcertResponseAssembler.convertToConcertListResponse(
                concertPage.getRecords(), new ConcertDependencyHolderImpl());

        return new PageResponse<>(concertPage.getCurrent(), concertPage.getSize(),
                concertPage.getTotal(), responseList);
    }

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
