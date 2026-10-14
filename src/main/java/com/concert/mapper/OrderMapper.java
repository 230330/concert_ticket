package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @description:    订单表 Mapper 接口
 * SQL 定义已迁移至 resources/mapper/OrderMapper.xml，
 * 状态值通过 OGNL 引用 OrderStatus 编译期常量，避免硬编码魔法数字
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
    List<Long> selectNeedCompleteOrderIds(@Param("limit") int limit);

    /**
     * 验证票码（将已支付订单标记为已退款）
     * @param pickupCode 票码
     * @return 影响行数
     */
    int verifyTicketCode(@Param("pickupCode") String pickupCode);

    /**
     * 根据手机号查询已支付的订单
     * @param phone 手机号
     * @return 订单列表
     */
    List<Order> selectPaidOrdersWithTicketCodeByPhone(@Param("phone") String phone);

    /**
     * 按订单状态聚合求和金额
     * @param statuses 状态值数组
     * @return 金额总和
     */
    BigDecimal sumAmountByStatuses(@Param("statuses") int[] statuses);

    /**
     * 按用户批量统计订单数量（供列表场景使用，避免逐个用户查询）
     * 返回的每行包含 userId 与 orderCount 两个键
     *
     * @param userIds 用户ID集合
     * @return 每个用户一行的统计结果，没有订单的用户不会出现
     */
    List<Map<String, Object>> countOrdersByUserIds(@Param("userIds") Collection<Long> userIds);
}
