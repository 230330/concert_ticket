package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单表 Mapper 接口
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

}
