package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.Artist;
import com.concert.mapper.ArtistMapper;
import com.concert.service.ArtistService;
import org.springframework.stereotype.Service;

/**
 * @description:    艺人服务实现类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class ArtistServiceImpl extends ServiceImpl<ArtistMapper, Artist> implements ArtistService {

}
