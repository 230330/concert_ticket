package com.concert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concert.entity.ConcertArtist;

import java.util.List;

/**
 * @description:    演唱会-艺人关联服务接口
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

public interface ConcertArtistService extends IService<ConcertArtist> {
    /**
     * 保存演唱会和艺人的关联关系
     * @param concertId 演唱会ID
     * @param artistIds 艺人ID列表
     */
    void saveConcertArtists(Long concertId, List<Long> artistIds);
}
