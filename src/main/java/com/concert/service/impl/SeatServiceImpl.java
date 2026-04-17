package com.concert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.entity.Seat;
import com.concert.mapper.SeatMapper;
import com.concert.service.SeatService;
import org.springframework.stereotype.Service;

/**
 * @description:    座席区域服务实现类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class SeatServiceImpl extends ServiceImpl<SeatMapper, Seat> implements SeatService {

}
