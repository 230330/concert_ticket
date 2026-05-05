import request from '@/utils/request'

const BASE = '/api/admin/venue'

/**
 * 分页查询场馆列表
 */
export function getVenueList(params) {
  return request({ url: `${BASE}/list`, method: 'get', params })
}

/**
 * 查询场馆详情
 */
export function getVenueDetail(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/**
 * 新增场馆
 */
export function addVenue(data) {
  return request({ url: `${BASE}/add`, method: 'post', data })
}

/**
 * 更新场馆
 */
export function updateVenue(id, data) {
  return request({ url: `${BASE}/${id}`, method: 'put', data })
}

/**
 * 删除场馆
 */
export function deleteVenue(id) {
  return request({ url: `${BASE}/${id}`, method: 'delete' })
}
