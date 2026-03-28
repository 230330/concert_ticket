package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.ConcertArtist;
import com.concert.mapper.ConcertArtistMapper;
import com.concert.service.ConcertArtistService;
import org.springframework.stereotype.Service;

/**
 * 演唱会-艺人关联服务实现类
 */
@Service
public class ConcertArtistServiceImpl extends ServiceImpl<ConcertArtistMapper, ConcertArtist> implements ConcertArtistService {

}
