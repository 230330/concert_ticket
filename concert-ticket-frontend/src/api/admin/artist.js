import request from '@/utils/request'

const BASE = '/api/admin/artist'

/**
 * 分页查询艺人列表
 */
export function getArtistList(params) {
  return request({ url: `${BASE}/list`, method: 'get', params })
}

/**
 * 查询艺人详情
 */
export function getArtistDetail(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/**
 * 新增艺人
 */
export function addArtist(data) {
  return request({ url: `${BASE}/add`, method: 'post', data })
}

/**
 * 更新艺人
 */
export function updateArtist(id, data) {
  return request({ url: `${BASE}/${id}`, method: 'put', data })
}

/**
 * 删除艺人
 */
export function deleteArtist(id) {
  return request({ url: `${BASE}/${id}`, method: 'delete' })
}
