import request from '@/utils/request'

const BASE = '/api/admin/concert'

/**
 * 分页查询演唱会列表
 */
export function getConcertList(params) {
  return request({ url: `${BASE}/list`, method: 'get', params })
}

/**
 * 查询演唱会详情
 */
export function getConcertDetail(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/**
 * 新增演唱会
 */
export function addConcert(data) {
  return request({ url: `${BASE}/add`, method: 'post', data })
}

/**
 * 更新演唱会
 */
export function updateConcert(id, data) {
  return request({ url: `${BASE}/${id}`, method: 'put', data })
}

/**
 * 删除演唱会
 */
export function deleteConcert(id) {
  return request({ url: `${BASE}/${id}`, method: 'delete' })
}

/**
 * 更新演唱会状态
 */
export function updateConcertStatus(id, data) {
  return request({ url: `${BASE}/${id}/status`, method: 'put', data })
}
