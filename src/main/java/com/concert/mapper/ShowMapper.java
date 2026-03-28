package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.Show;
import org.apache.ibatis.annotations.Mapper;

/**
 * 场次表 Mapper 接口
 */
@Mapper
public interface ShowMapper extends BaseMapper<Show> {

}
