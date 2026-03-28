package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.OrderSeat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单座位表 Mapper 接口
 */
@Mapper
public interface OrderSeatMapper extends BaseMapper<OrderSeat> {

}
