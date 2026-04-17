package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description:    用户表 Mapper 接口
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
