package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.Venue;
import com.concert.mapper.VenueMapper;
import com.concert.service.VenueService;
import org.springframework.stereotype.Service;

/**
 * 场馆服务实现类
 */
@Service
public class VenueServiceImpl extends ServiceImpl<VenueMapper, Venue> implements VenueService {

}
