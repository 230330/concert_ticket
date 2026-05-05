import request from '@/utils/request'

/**
 * 获取当前用户的取票码列表
 */
export function getMyTicketCodes() {
  return request({
    url: '/api/ticket/my-codes',
    method: 'get'
  })
}

/**
 * 核销取票码（线下工作人员使用）
 */
export function verifyTicketCode(pickupCode) {
  return request({
    url: '/api/ticket/verify',
    method: 'post',
    params: { pickupCode }
  })
}
