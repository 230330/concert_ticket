/**
 * 状态映射常量，与后端枚举保持一致
 *
 * 后端：ConcertStatus 0 未开始 / 1 进行中 / 2 已结束 / 3 已取消
 *       ShowStatus    0 未开售 / 1 售票中 / 2 已售罄 / 3 已结束 / 4 已取消
 *       OrderStatus   0 待支付 / 1 已支付 / 2 已取消 / 3 已退款 / 4 已完成
 *       UserStatus    0 已封禁 / 1 正常
 *
 * 各页面统一从这里导入，不要再各自复制一份映射。
 */

/** 订单状态 */
export const ORDER_STATUS_MAP = {
  0: { label: '待支付', tagType: 'warning' },
  1: { label: '已支付', tagType: 'success' },
  2: { label: '已取消', tagType: 'info' },
  3: { label: '已退款', tagType: 'danger' },
  4: { label: '已完成', tagType: 'success' }
}

/** 演唱会状态 */
export const CONCERT_STATUS_MAP = {
  0: { label: '未开始', tagType: 'info' },
  1: { label: '进行中', tagType: 'warning' },
  2: { label: '已结束', tagType: 'success' },
  3: { label: '已取消', tagType: 'danger' }
}

/** 场次状态 */
export const SHOW_STATUS_MAP = {
  0: { label: '未开售', tagType: 'info' },
  1: { label: '售票中', tagType: 'warning' },
  2: { label: '已售罄', tagType: 'danger' },
  3: { label: '已结束', tagType: 'success' },
  4: { label: '已取消', tagType: 'info' }
}

/** 用户状态 */
export const USER_STATUS_MAP = {
  0: { label: '已封禁', tagType: 'danger' },
  1: { label: '正常', tagType: 'success' }
}

/**
 * 取状态文案，未识别的状态返回「未知」
 */
export function resolveStatusLabel(statusMap, status) {
  return statusMap[status] ? statusMap[status].label : '未知'
}

/**
 * 取状态标签样式，未识别的状态返回 info
 */
export function resolveStatusTagType(statusMap, status) {
  return statusMap[status] ? statusMap[status].tagType : 'info'
}

/**
 * 由状态映射生成下拉选项，供搜索与表单复用
 */
export function toStatusOptions(statusMap) {
  return Object.entries(statusMap).map(([value, { label }]) => ({ value: Number(value), label }))
}
