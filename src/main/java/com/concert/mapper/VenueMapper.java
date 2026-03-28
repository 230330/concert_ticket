package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.Venue;
import org.apache.ibatis.annotations.Mapper;

/**
 * 场馆表 Mapper 接口
 */
@Mapper
public interface VenueMapper extends BaseMapper<Venue> {

}
