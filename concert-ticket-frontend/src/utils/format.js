/**
 * 时间格式化
 *
 * 后端统一返回 yyyy-MM-dd HH:mm:ss（见 JacksonConfig），前端只需把 T 换成空格并截断。
 * 各页面统一从这里导入，不要各自再写一份——此前 5 个页面各有一份 formatTime，
 * 精度（截到分/截到秒）与空值占位（'-'/''）都不一致。
 */

/**
 * 格式化时间显示：统一截到分钟，空值返回 '-'
 *
 * @param {string} value 后端返回的时间字符串
 * @returns {string} 形如 2026-04-17 15:30
 */
export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').substring(0, 16)
}
