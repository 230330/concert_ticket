import request from '@/utils/request'

const BASE = '/api/admin/order'

/**
 * 分页查询订单列表
 */
export function getOrderList(params) {
  return request({ url: `${BASE}/list`, method: 'get', params })
}

/**
 * 查询订单详情
 */
export function getOrderDetail(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/**
 * 管理员退款
 */
export function adminRefund(id) {
  return request({ url: `${BASE}/${id}/refund`, method: 'put' })
}

/**
 * 管理员取消订单
 */
export function adminCancel(id) {
  return request({ url: `${BASE}/${id}/cancel`, method: 'put' })
}
