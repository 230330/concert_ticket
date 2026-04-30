package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.ConcertArtist;
import com.concert.mapper.ConcertArtistMapper;
import com.concert.service.ConcertArtistService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description:    艺人服务实现类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class ConcertArtistServiceImpl extends ServiceImpl<ConcertArtistMapper, ConcertArtist> implements ConcertArtistService {

    @Resource
    @Lazy
    private ConcertArtistService concertArtistService;
    /**
     * 保存演唱会和艺人的关联关系
     *
     * @param concertId 演唱会ID
     * @param artistIds 艺人ID列表
     */
    @Override
    public void saveConcertArtists(Long concertId, List<Long> artistIds) {
        List<ConcertArtist> concertArtists = artistIds.stream().map(artistId -> {
            ConcertArtist ca = new ConcertArtist();
            ca.setConcertId(concertId);
            ca.setArtistId(artistId);
            return ca;
        }).collect(Collectors.toList());
        concertArtistService.saveBatch(concertArtists);
    }
}
