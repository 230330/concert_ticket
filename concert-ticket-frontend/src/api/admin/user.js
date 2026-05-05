import request from '@/utils/request'

const BASE = '/api/admin/user'

/**
 * 分页查询用户列表
 */
export function getUserList(params) {
  return request({ url: `${BASE}/list`, method: 'get', params })
}

/**
 * 查询用户详情
 */
export function getUserDetail(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/**
 * 封禁用户
 */
export function banUser(data) {
  return request({ url: `${BASE}/ban`, method: 'put', data })
}

/**
 * 解封用户
 */
export function unbanUser(data) {
  return request({ url: `${BASE}/unban`, method: 'put', data })
}
