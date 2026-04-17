package com.concert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.dto.response.ConcertDetailResponse;
import com.concert.dto.response.ConcertListResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.*;
import com.concert.exception.NotFoundException;
import com.concert.mapper.ConcertMapper;
import com.concert.service.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description:    演出信息服务实现类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class ConcertServiceImpl extends ServiceImpl<ConcertMapper, Concert> implements ConcertService {

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
        queryWrapper.in(Concert::getStatus, 0, 1);

        if ("time".equals(sort)) {
            queryWrapper.orderByDesc(Concert::getCreateTime);
        } else {
            queryWrapper.orderByDesc(Concert::getId);
        }

        this.page(concertPage, queryWrapper);

        List<ConcertListResponse> responseList = convertToConcertListResponse(concertPage.getRecords());
        return new PageResponse<>(concertPage.getCurrent(), concertPage.getSize(),
                concertPage.getTotal(), responseList);
    }

    @Override
    public PageResponse<ConcertListResponse> getUpcomingConcerts(Integer page, Integer size) {
        LocalDateTime now = LocalDateTime.now();

        // 查询即将开始的场次
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.gt(Show::getShowTime, now)
                .in(Show::getStatus, 0, 1)
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
                .in(Concert::getStatus, 0, 1);
        this.page(concertPage, queryWrapper);

        List<ConcertListResponse> responseList = convertToConcertListResponse(concertPage.getRecords());
        return new PageResponse<>(concertPage.getCurrent(), concertPage.getSize(),
                concertPage.getTotal(), responseList);
    }

    @Override
    public PageResponse<ConcertListResponse> searchConcerts(String keyword, String city, String artistName,
                                                             LocalDate startDate, LocalDate endDate,
                                                             Integer page, Integer size) {
        // 1. 基础条件
        LambdaQueryWrapper<Concert> concertQuery = new LambdaQueryWrapper<>();
        concertQuery.in(Concert::getStatus, 0, 1);

        // 2. 关键词模糊匹配
        if (keyword != null && !keyword.trim().isEmpty()) {
            concertQuery.like(Concert::getName, keyword.trim());
        }

        // 3. 按艺人名称筛选
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

        // 4. 按城市或日期范围筛选
        boolean needShowFilter = (city != null && !city.trim().isEmpty())
                || startDate != null || endDate != null;

        if (needShowFilter) {
            LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
            showQuery.in(Show::getStatus, 0, 1);

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

        // 5. 分页查询
        concertQuery.orderByDesc(Concert::getId);
        Page<Concert> concertPage = new Page<>(page, size);
        this.page(concertPage, concertQuery);

        List<ConcertListResponse> responseList = convertToConcertListResponse(concertPage.getRecords());
        return new PageResponse<>(concertPage.getCurrent(), concertPage.getSize(),
                concertPage.getTotal(), responseList);
    }

    @Override
    public ConcertDetailResponse getConcertDetail(Long id) {
        Concert concert = this.getById(id);
        if (concert == null) {
            throw new NotFoundException("演唱会不存在");
        }

        ConcertDetailResponse response = new ConcertDetailResponse();
        response.setId(concert.getId());
        response.setName(concert.getName());
        response.setPoster(concert.getPoster());
        response.setDescription(concert.getDescription());
        response.setStatus(concert.getStatus());
        response.setCreateTime(concert.getCreateTime());

        // 查询艺人信息
        LambdaQueryWrapper<ConcertArtist> caQuery = new LambdaQueryWrapper<>();
        caQuery.eq(ConcertArtist::getConcertId, id);
        List<ConcertArtist> concertArtists = concertArtistService.list(caQuery);

        if (!concertArtists.isEmpty()) {
            List<Long> artistIds = concertArtists.stream()
                    .map(ConcertArtist::getArtistId)
                    .collect(Collectors.toList());
            List<Artist> artists = artistService.listByIds(artistIds);

            List<ConcertDetailResponse.ArtistInfo> artistInfos = artists.stream()
                    .map(artist -> {
                        ConcertDetailResponse.ArtistInfo info = new ConcertDetailResponse.ArtistInfo();
                        info.setId(artist.getId());
                        info.setName(artist.getName());
                        info.setAvatar(artist.getAvatar());
                        info.setDescription(artist.getDescription());
                        return info;
                    })
                    .collect(Collectors.toList());
            response.setArtists(artistInfos);
        } else {
            response.setArtists(Collections.emptyList());
        }

        // 查询场次信息
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.eq(Show::getConcertId, id)
                .orderByAsc(Show::getShowTime);
        List<Show> shows = showService.list(showQuery);

        if (!shows.isEmpty()) {
            List<Long> venueIds = shows.stream()
                    .map(Show::getVenueId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, Venue> venueMap = venueService.listByIds(venueIds).stream()
                    .collect(Collectors.toMap(Venue::getId, v -> v));

            List<ConcertDetailResponse.ShowInfo> showInfos = shows.stream()
                    .map(show -> {
                        ConcertDetailResponse.ShowInfo info = new ConcertDetailResponse.ShowInfo();
                        info.setId(show.getId());
                        info.setShowTime(show.getShowTime());
                        info.setSaleStartTime(show.getSaleStartTime());
                        info.setSaleEndTime(show.getSaleEndTime());
                        info.setStatus(show.getStatus());

                        Venue venue = venueMap.get(show.getVenueId());
                        if (venue != null) {
                            info.setVenueName(venue.getName());
                            info.setCity(venue.getCity());
                            info.setAddress(venue.getAddress());
                        }
                        return info;
                    })
                    .collect(Collectors.toList());
            response.setShows(showInfos);
        } else {
            response.setShows(Collections.emptyList());
        }

        return response;
    }

    /**
     * 批量转换演唱会列表为响应对象
     */
    private List<ConcertListResponse> convertToConcertListResponse(List<Concert> concerts) {
        if (concerts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> concertIds = concerts.stream()
                .map(Concert::getId)
                .collect(Collectors.toList());

        // 批量查询艺人关联
        LambdaQueryWrapper<ConcertArtist> caQuery = new LambdaQueryWrapper<>();
        caQuery.in(ConcertArtist::getConcertId, concertIds);
        List<ConcertArtist> concertArtists = concertArtistService.list(caQuery);

        List<Long> artistIds = concertArtists.stream()
                .map(ConcertArtist::getArtistId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> artistNameMap = new HashMap<>();
        if (!artistIds.isEmpty()) {
            artistNameMap = artistService.listByIds(artistIds).stream()
                    .collect(Collectors.toMap(Artist::getId, Artist::getName));
        }

        // 按演唱会分组艺人
        Map<Long, String> finalArtistNameMap = artistNameMap;
        Map<Long, List<String>> concertArtistNamesMap = concertArtists.stream()
                .collect(Collectors.groupingBy(
                        ConcertArtist::getConcertId,
                        Collectors.mapping(ca -> finalArtistNameMap.get(ca.getArtistId()), Collectors.toList())
                ));

        // 批量查询场次
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.in(Show::getConcertId, concertIds)
                .orderByAsc(Show::getShowTime);
        List<Show> shows = showService.list(showQuery);

        // 批量查询场馆
        List<Long> venueIds = shows.stream()
                .map(Show::getVenueId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Venue> venueMap = new HashMap<>();
        if (!venueIds.isEmpty()) {
            venueMap = venueService.listByIds(venueIds).stream()
                    .collect(Collectors.toMap(Venue::getId, v -> v));
        }

        // 按演唱会分组场次
        Map<Long, List<Show>> concertShowsMap = shows.stream()
                .collect(Collectors.groupingBy(Show::getConcertId));

        // 批量查询票档获取最低价
        List<Long> showIds = shows.stream()
                .map(Show::getId)
                .collect(Collectors.toList());
        Map<Long, BigDecimal> showMinPriceMap = new HashMap<>();
        if (!showIds.isEmpty()) {
            LambdaQueryWrapper<TicketType> ttQuery = new LambdaQueryWrapper<>();
            ttQuery.in(TicketType::getShowId, showIds);
            List<TicketType> ticketTypes = ticketTypeService.list(ttQuery);

            showMinPriceMap = ticketTypes.stream()
                    .collect(Collectors.groupingBy(
                            TicketType::getShowId,
                            Collectors.collectingAndThen(
                                    Collectors.minBy(Comparator.comparing(TicketType::getPrice)),
                                    opt -> opt.map(TicketType::getPrice).orElse(null)
                            )
                    ));
        }

        Map<Long, Venue> finalVenueMap = venueMap;
        Map<Long, BigDecimal> finalShowMinPriceMap = showMinPriceMap;

        return concerts.stream()
                .map(concert -> {
                    ConcertListResponse resp = new ConcertListResponse();
                    resp.setId(concert.getId());
                    resp.setName(concert.getName());
                    resp.setPoster(concert.getPoster());
                    resp.setStatus(concert.getStatus());
                    resp.setCreateTime(concert.getCreateTime());

                    // 艺人名称
                    resp.setArtistNames(concertArtistNamesMap.getOrDefault(concert.getId(), Collections.emptyList()));

                    // 最近场次信息
                    List<Show> concertShows = concertShowsMap.get(concert.getId());
                    if (concertShows != null && !concertShows.isEmpty()) {
                        Show nearestShow = concertShows.get(0);
                        resp.setNearestShowTime(nearestShow.getShowTime());

                        Venue venue = finalVenueMap.get(nearestShow.getVenueId());
                        if (venue != null) {
                            resp.setCity(venue.getCity());
                            resp.setVenueName(venue.getName());
                        }

                        BigDecimal minPrice = finalShowMinPriceMap.get(nearestShow.getId());
                        if (minPrice != null) {
                            resp.setMinPrice("¥" + minPrice.toString());
                        }
                    }

                    return resp;
                })
                .collect(Collectors.toList());
    }
}
