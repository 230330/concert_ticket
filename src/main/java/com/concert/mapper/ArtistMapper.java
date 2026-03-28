package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.Artist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 艺人表 Mapper 接口
 */
@Mapper
public interface ArtistMapper extends BaseMapper<Artist> {

}
