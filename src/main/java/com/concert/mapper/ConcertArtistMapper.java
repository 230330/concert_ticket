package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.ConcertArtist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 演唱会-艺人关联表 Mapper 接口
 */
@Mapper
public interface ConcertArtistMapper extends BaseMapper<ConcertArtist> {

}
