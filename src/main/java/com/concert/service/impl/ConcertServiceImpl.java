package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.Concert;
import com.concert.mapper.ConcertMapper;
import com.concert.service.ConcertService;
import org.springframework.stereotype.Service;

/**
 * 演唱会服务实现类
 */
@Service
public class ConcertServiceImpl extends ServiceImpl<ConcertMapper, Concert> implements ConcertService {

}
