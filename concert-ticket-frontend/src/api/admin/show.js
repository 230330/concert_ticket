import request from '@/utils/request'

const BASE = '/api/admin/show'

/**
 * 分页查询场次列表
 */
export function getShowList(params) {
  return request({ url: `${BASE}/list`, method: 'get', params })
}

/**
 * 查询场次详情
 */
export function getShowDetail(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/**
 * 新增场次
 */
export function addShow(data) {
  return request({ url: `${BASE}/add`, method: 'post', data })
}

/**
 * 更新场次
 */
export function updateShow(id, data) {
  return request({ url: `${BASE}/${id}`, method: 'put', data })
}

/**
 * 删除场次
 */
export function deleteShow(id) {
  return request({ url: `${BASE}/${id}`, method: 'delete' })
}
