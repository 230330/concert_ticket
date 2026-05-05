import request from '@/utils/request'

const BASE = '/api/admin/ticket-type'

/**
 * 分页查询票种列表
 */
export function getTicketTypeList(params) {
  return request({ url: `${BASE}/list`, method: 'get', params })
}

/**
 * 查询票种详情
 */
export function getTicketTypeDetail(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/**
 * 新增票种
 */
export function addTicketType(data) {
  return request({ url: `${BASE}/add`, method: 'post', data })
}

/**
 * 更新票种
 */
export function updateTicketType(id, data) {
  return request({ url: `${BASE}/${id}`, method: 'put', data })
}

/**
 * 删除票种
 */
export function deleteTicketType(id) {
  return request({ url: `${BASE}/${id}`, method: 'delete' })
}

/**
 * 库存调整
 */
export function adjustStock(data) {
  return request({ url: `${BASE}/stock/adjust`, method: 'put', data })
}
