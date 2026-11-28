import { clearSession, token } from './session'

let defaultBase = ''
// #ifdef MP-WEIXIN
defaultBase = 'http://127.0.0.1:8080'
// #endif
export const API_BASE = (import.meta.env.VITE_API_BASE_URL || defaultBase).replace(/\/+$/, '')

export function request(options) {
  const requestToken = options.auth === false ? '' : token.value
  return new Promise((resolve) => {
    uni.request({
      url: API_BASE + options.url,
      method: options.method || 'GET',
      data: options.data,
      ...(options.timeout ? { timeout: options.timeout } : {}),
      header: {
        'Content-Type': 'application/json',
        ...(requestToken ? { Authorization: `Bearer ${requestToken}` } : {}),
        ...(options.header || {})
      },
      success(res) {
        if (res.statusCode === 401 && requestToken && requestToken === token.value) clearSession()
        if (res.statusCode >= 400) {
          resolve({
            code: res.data?.code || res.statusCode,
            message: res.data?.message || (res.statusCode === 401 ? '请先登录' : '请求失败，请稍后重试'),
            data: null
          })
        } else {
          resolve(res.data || { code: res.statusCode, message: '', data: null })
        }
      },
      fail() {
        resolve({ code: -1, message: '网络开小差了，请重试', data: null })
      }
    })
  })
}

export default { request }
