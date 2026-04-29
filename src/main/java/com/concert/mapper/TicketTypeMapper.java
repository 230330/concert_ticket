package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.TicketType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @description:    票档表 Mapper 接口
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Mapper
public interface TicketTypeMapper extends BaseMapper<TicketType> {
    /**
     * 减少票品的已售数量
     * @param ticketTypeId 票品ID
     * @param decrement 要减少的数量（即取消订单中的座位数）
     * @return 影响的行数（成功更新则返回1，否则返回0）
     */
    @Update("UPDATE ticket_type SET sold_stock = sold_stock - #{decrement}, update_time = NOW()" +
            "WHERE id = #{ticketTypeId} AND sold_stock >= #{decrement}")
    int updateSoldStockDecrement(@Param("ticketTypeId") Long ticketTypeId,
                                    @Param("decrement") Integer decrement);
}
