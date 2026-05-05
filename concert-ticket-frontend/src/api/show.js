import request from '@/utils/request'

/**
 * 根据演出ID查询场次列表
 */
export function getShowList(params) {
  return request({
    url: '/api/show/list',
    method: 'get',
    params
  })
}

/**
 * 获取场次座位图
 */
export function getSeatMap(showId) {
  return request({
    url: `/api/show/${showId}/seats`,
    method: 'get'
  })
}
