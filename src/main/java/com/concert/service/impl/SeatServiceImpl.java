package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.Seat;
import com.concert.mapper.SeatMapper;
import com.concert.service.SeatService;
import org.springframework.stereotype.Service;

/**
 * 座位服务实现类
 */
@Service
public class SeatServiceImpl extends ServiceImpl<SeatMapper, Seat> implements SeatService {

}
