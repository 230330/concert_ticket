/**
 * 校验是否为外部链接
 * @param {string} path
 * @returns {Boolean}
 */
export function isExternal(path) {
  return /^(https?:|mailto:|tel:)/.test(path)
}

/**
 * 校验手机号格式（中国大陆手机号）
 * @param {string} phone
 * @returns {Boolean}
 */
export function validPhone(phone) {
  return /^1[3-9]\d{9}$/.test(phone)
}

/**
 * 校验密码长度（6-20位）
 * @param {string} password
 * @returns {Boolean}
 */
export function validPassword(password) {
  return password && password.length >= 6 && password.length <= 20
}
