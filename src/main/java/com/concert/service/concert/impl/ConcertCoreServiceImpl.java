package com.concert.service.concert.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.dto.response.ConcertDetailResponse;
import com.concert.entity.*;
import com.concert.exception.NotFoundException;
import com.concert.service.*;
import com.concert.service.concert.ConcertCoreService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: 演唱会核心服务实现（详情）
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Service
public class ConcertCoreServiceImpl implements ConcertCoreService {

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

    @Override
    public ConcertDetailResponse getConcertDetail(Long id) {
        Concert concert = concertService.getById(id);
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

        // 查询关联的艺人信息
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

        // 查询关联的场次信息（含场馆）
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.eq(Show::getConcertId, id)
                .orderByAsc(Show::getShowTime);
        List<Show> shows = showService.list(showQuery);

        if (!shows.isEmpty()) {
            List<Long> venueIds = shows.stream()
                    .map(Show::getVenueId)
                    .distinct()
                    .collect(Collectors.toList());

            java.util.Map<Long, Venue> venueMap = venueService.listByIds(venueIds).stream()
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
}
