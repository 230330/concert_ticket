import request from '@/utils/request'

const BASE = '/api/admin/dashboard'

/**
 * 销售统计概览
 */
export function getSalesOverview() {
  return request({ url: `${BASE}/sales`, method: 'get' })
}

/**
 * 收入报表（按日期范围统计）
 */
export function getRevenueReport(params) {
  return request({ url: `${BASE}/revenue`, method: 'get', params })
}
