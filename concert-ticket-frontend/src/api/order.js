import request from '@/utils/request'

/**
 * 创建订单
 */
export function createOrder(data) {
  return request({
    url: '/api/order/create',
    method: 'post',
    data
  })
}

/**
 * 支付订单（模拟）
 */
export function payOrder(data) {
  return request({
    url: '/api/order/pay',
    method: 'post',
    data
  })
}

/**
 * 取消订单
 */
export function cancelOrder(data) {
  return request({
    url: '/api/order/cancel',
    method: 'put',
    data
  })
}

/**
 * 退款订单
 */
export function refundOrder(data) {
  return request({
    url: '/api/order/refund',
    method: 'put',
    data
  })
}

/**
 * 获取订单详情
 */
export function getOrderDetail(orderId) {
  return request({
    url: `/api/order/${orderId}`,
    method: 'get'
  })
}

/**
 * 我的订单列表（分页+状态筛选）
 */
export function getMyOrders(params) {
  return request({
    url: '/api/order/my',
    method: 'get',
    params
  })
}
