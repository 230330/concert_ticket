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
 * 校验密码格式（8-32位，必须包含字母和数字）
 * 与后端 RegisterRequest @Size(min=8,max=32) + @Pattern(?=.*[a-zA-Z])(?=.*\d) 保持一致
 * @param {string} password
 * @returns {Boolean}
 */
export function validPassword(password) {
  if (!password || password.length < 8 || password.length > 32) {
    return false
  }
  return /(?=.*[a-zA-Z])(?=.*\d)/.test(password)
}
