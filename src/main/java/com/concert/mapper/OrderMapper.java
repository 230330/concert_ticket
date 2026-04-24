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
    /**
     * 验证票码
     * @param pickupCode 票码
     * @return 影响行数
     */
    @Update("UPDATE `order` SET status = 3, update_time = NOW() WHERE pickup_code = #{pickupCode} AND status = 1 AND pickup_code IS NOT NULL")
    int verifyTicketCode(@Param("pickupCode") String pickupCode);

    /**
     * 根据手机号查询已支付的订单
     * @param phone 手机号
     * @return 订单列表
     */
    @Select("SELECT o.* FROM `order` o INNER JOIN `user` u ON o.user_id = u.id " +
            "WHERE u.phone = #{phone} AND o.status = 1 AND o.pickup_code IS NOT NULL " +
            "ORDER BY o.create_time DESC")
    List<Order> selectPaidOrdersWithTicketCodeByPhone(@Param("phone") String phone);
}
