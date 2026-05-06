import request from '@/utils/request'

/**
 * 用户登录（手机号+密码）
 */
export function login(data) {
  return request({
    url: '/api/user/login',
    method: 'post',
    data
  })
}

/**
 * 用户注册（手机号+密码+验证码）
 */
export function register(data) {
  return request({
    url: '/api/user/register',
    method: 'post',
    data
  })
}

/**
 * 发送短信验证码
 */
export function sendSms(data) {
  return request({
    url: '/api/user/sendSms',
    method: 'post',
    data
  })
}

/**
 * 获取当前登录用户信息
 */
export function getInfo() {
  return request({
    url: '/api/user/info',
    method: 'get'
  })
}

/**
 * 更新用户信息
 */
export function updateInfo(data) {
  return request({
    url: '/api/user/update',
    method: 'put',
    data
  })
}

/**
 * 修改密码
 */
export function changePassword(data) {
  return request({
    url: '/api/user/changePassword',
    method: 'put',
    data
  })
}
