import request from '@/utils/request'

/**
 * 热门演出列表（分页）
 */
export function getHotConcerts(params) {
  return request({
    url: '/api/concert/hot',
    method: 'get',
    params
  })
}

/**
 * 即将开始演出列表（分页）
 */
export function getUpcomingConcerts(params) {
  return request({
    url: '/api/concert/upcoming',
    method: 'get',
    params
  })
}

/**
 * 搜索演唱会
 */
export function searchConcerts(params) {
  return request({
    url: '/api/concert/search',
    method: 'get',
    params
  })
}

/**
 * 演出详情
 */
export function getConcertDetail(id) {
  return request({
    url: `/api/concert/${id}`,
    method: 'get'
  })
}
