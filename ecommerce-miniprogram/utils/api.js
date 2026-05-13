// H5 dev mode uses Vite proxy, mini-program uses absolute URL
export const API_BASE = typeof window !== 'undefined' ? '' : 'http://192.168.5.6:8080'

export function request(options) {
  const token = uni.getStorageSync('token')
  return new Promise((resolve) => {
    uni.request({
      url: API_BASE + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        ...(options.header || {})
      },
      success(res) {
        if (res.statusCode === 401) {
          uni.removeStorageSync('token')
        }
        resolve(res.data || { code: res.statusCode, message: '', data: null })
      },
      fail() {
        resolve({ code: -1, message: '网络开小差了', data: null })
      }
    })
  })
}

export default { request }
