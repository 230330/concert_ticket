package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.Seat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 座位表 Mapper 接口
 */
@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

}
