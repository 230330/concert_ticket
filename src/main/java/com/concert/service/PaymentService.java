package com.concert.service;

import java.math.BigDecimal;

/**
 * 支付服务接口
 * 集成支付宝支付能力
 *
 * @author hzf
 * @date 2026/05/28
 */
public interface PaymentService {

    /**
     * 创建支付宝支付订单（电脑网站支付）
     *
     * @param orderId     系统订单ID
     * @param orderNo     系统订单编号
     * @param totalAmount 支付金额
     * @param subject     订单标题
     * @return 支付表单HTML（用于前端跳转）
     */
    String createAlipayOrder(Long orderId, String orderNo, BigDecimal totalAmount, String subject);

    /**
     * 处理支付宝异步通知
     * 验证签名后更新订单状态
     *
     * @param params 支付宝回调参数
     * @return 处理结果（success/failure）
     */
    String handleAlipayNotify(java.util.Map<String, String> params);

    /**
     * 查询支付宝交易状态
     *
     * @param orderNo 系统订单编号
     * @return 交易状态字符串
     */
    String queryAlipayTrade(String orderNo);

    /**
     * 申请退款
     *
     * @param orderNo     系统订单编号
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @return 退款是否成功
     */
    boolean alipayRefund(String orderNo, BigDecimal refundAmount, String refundReason);

    /**
     * 处理支付宝同步跳转
     *
     * @param params 支付宝回调参数
     * @return 验签结果
     */
    boolean handleAlipayReturn(java.util.Map<String, String> params);
}
