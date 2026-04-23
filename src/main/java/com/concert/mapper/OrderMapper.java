package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @description:    订单表 Mapper 接口
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    /**
     * 查询需要自动完成的订单ID（已支付，且演出场次已结束）
     * @param limit 最大数量
     * @return 订单ID列表
     */
    @Select("SELECT o.id FROM `order` o " +
            "INNER JOIN `show` s ON o.show_id = s.id " +
            "WHERE o.status = 1 " +
            "AND (s.show_date < CURDATE() " +
            "     OR (s.show_date = CURDATE() AND s.show_time < CURTIME())) " +
            "LIMIT #{limit}")
    List<Long> selectNeedCompleteOrderIds(@Param("limit") int limit);

    /**
     * 减少票种已售数量
     * @param ticketTypeId 票种ID
     * @param decrement 减少数量
     * @return 影响行数
     */
    @Update("UPDATE ticket_type SET sold_quantity = sold_quantity - #{decrement}, update_time = NOW() " +
            "WHERE id = #{ticketTypeId} AND sold_quantity >= #{decrement}")
    int updateSoldQuantityDecrement(@Param("ticketTypeId") Long ticketTypeId,
                                    @Param("decrement") Integer decrement);
}
