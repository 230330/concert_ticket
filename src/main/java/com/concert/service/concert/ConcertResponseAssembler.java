package com.concert.service.concert;

import com.concert.dto.response.ConcertListResponse;
import com.concert.entity.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 演唱会列表响应组装工具
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
public class ConcertResponseAssembler {

    /**
     * 将演唱会实体列表转换为列表响应对象列表
     * 此方法由 ConcertSearchServiceImpl 和 ConcertShowServiceImpl 共用
     */
    public static List<ConcertListResponse> convertToConcertListResponse(
            List<Concert> concerts,
            ConcertDependencyHolder holder) {

        if (concerts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> concertIds = concerts.stream()
                .map(Concert::getId)
                .collect(Collectors.toList());

        // 批量查询艺人关联
        List<ConcertArtist> concertArtists = holder.getConcertArtists(concertIds);

        List<Long> artistIds = concertArtists.stream()
                .map(ConcertArtist::getArtistId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> artistNameMap = new HashMap<>();
        if (!artistIds.isEmpty()) {
            artistNameMap = holder.getArtistNames(artistIds);
        }

        Map<Long, String> finalArtistNameMap = artistNameMap;
        Map<Long, List<String>> concertArtistNamesMap = concertArtists.stream()
                .collect(Collectors.groupingBy(
                        ConcertArtist::getConcertId,
                        Collectors.mapping(ca -> finalArtistNameMap.get(ca.getArtistId()), Collectors.toList())
                ));

        // 批量查询场次
        List<Show> shows = holder.getShows(concertIds);

        // 批量查询场馆
        List<Long> venueIds = shows.stream()
                .map(Show::getVenueId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Venue> venueMap = new HashMap<>();
        if (!venueIds.isEmpty()) {
            venueMap = holder.getVenueMap(venueIds);
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
            showMinPriceMap = holder.getShowMinPriceMap(showIds);
        }

        // 组装响应对象
        Map<Long, Venue> finalVenueMap = venueMap;
        Map<Long, BigDecimal> finalShowMinPriceMap = showMinPriceMap;

        return concerts.stream()
                .map(concert -> {
                    ConcertListResponse resp = new ConcertListResponse();
                    resp.setId(concert.getId());
                    resp.setName(concert.getName());
                    resp.setPoster(concert.getPoster());
                    resp.setDescription(concert.getDescription());
                    resp.setStatus(concert.getStatus());
                    resp.setCreateTime(concert.getCreateTime());

                    resp.setArtistNames(concertArtistNamesMap.getOrDefault(concert.getId(), Collections.emptyList()));

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

    /**
     * 依赖持有者接口，用于解耦数据查询
     */
    public interface ConcertDependencyHolder {
        List<ConcertArtist> getConcertArtists(List<Long> concertIds);
        Map<Long, String> getArtistNames(List<Long> artistIds);
        List<Show> getShows(List<Long> concertIds);
        Map<Long, Venue> getVenueMap(List<Long> venueIds);
        Map<Long, BigDecimal> getShowMinPriceMap(List<Long> showIds);
    }
}
