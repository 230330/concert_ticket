package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.Concert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 演唱会表 Mapper 接口
 */
@Mapper
public interface ConcertMapper extends BaseMapper<Concert> {

}
