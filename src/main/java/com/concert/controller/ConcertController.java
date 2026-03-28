package com.concert.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.Result;
import com.concert.dto.response.ConcertDetailResponse;
import com.concert.dto.response.ConcertListResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.*;
import com.concert.service.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 演唱会控制器
 */
@RestController
@RequestMapping("/api/concert")
public class ConcertController {

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

    /**
     * 热门演出列表（分页）
     *
     * @param page 页码
     * @param size 每页条数
     * @param sort 排序方式：time-按时间，default-默认
     */
    @GetMapping("/hot")
    public Result<PageResponse<ConcertListResponse>> getHotConcerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "default") String sort) {

        // 分页查询演唱会（状态为进行中或未开始）
        Page<Concert> concertPage = new Page<>(page, size);
        LambdaQueryWrapper<Concert> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Concert::getStatus, 0, 1);

        // 排序
        if ("time".equals(sort)) {
            queryWrapper.orderByDesc(Concert::getCreateTime);
        } else {
            queryWrapper.orderByDesc(Concert::getId);
        }

        concertService.page(concertPage, queryWrapper);

        // 转换为响应对象
        List<ConcertListResponse> responseList = convertToConcertListResponse(concertPage.getRecords());

        PageResponse<ConcertListResponse> pageResponse = new PageResponse<>(
                concertPage.getCurrent(),
                concertPage.getSize(),
                concertPage.getTotal(),
                responseList
        );

        return Result.success(pageResponse);
    }

    /**
     * 即将开始演出列表（分页）
     *
     * @param page 页码
     * @param size 每页条数
     */
    @GetMapping("/upcoming")
    public Result<PageResponse<ConcertListResponse>> getUpcomingConcerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        // 查询有即将开始场次的演唱会
        LocalDateTime now = LocalDateTime.now();

        // 先查询即将开始的场次
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.gt(Show::getShowTime, now)
                .in(Show::getStatus, 0, 1)
                .orderByAsc(Show::getShowTime);
        List<Show> upcomingShows = showService.list(showQuery);

        // 获取演唱会ID列表（去重）
        List<Long> concertIds = upcomingShows.stream()
                .map(Show::getConcertId)
                .distinct()
                .collect(Collectors.toList());

        if (concertIds.isEmpty()) {
            return Result.success(new PageResponse<>(1L, (long) size, 0L, Collections.emptyList()));
        }

        // 分页查询演唱会
        Page<Concert> concertPage = new Page<>(page, size);
        LambdaQueryWrapper<Concert> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Concert::getId, concertIds)
                .in(Concert::getStatus, 0, 1);

        concertService.page(concertPage, queryWrapper);

        // 转换为响应对象
        List<ConcertListResponse> responseList = convertToConcertListResponse(concertPage.getRecords());

        PageResponse<ConcertListResponse> pageResponse = new PageResponse<>(
                concertPage.getCurrent(),
                concertPage.getSize(),
                concertPage.getTotal(),
                responseList
        );

        return Result.success(pageResponse);
    }

    /**
     * 演出详情（包含艺人信息）
     *
     * @param id 演唱会ID
     */
    @GetMapping("/{id}")
    public Result<ConcertDetailResponse> getConcertDetail(@PathVariable Long id) {
        // 查询演唱会
        Concert concert = concertService.getById(id);
        if (concert == null) {
            return Result.error("演唱会不存在");
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
            // 获取所有场馆信息
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

        return Result.success(response);
    }

    /**
     * 转换为演唱会列表响应
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

        // 获取所有艺人ID
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

        // 获取所有场馆
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

                        // 最低票价
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
