const BASE = 'http://192.168.5.6:8080'

export function request(options) {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        ...(options.header || {})
      },
      success(res) {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.showToast({ title: '请先登录', icon: 'none' })
          reject(res.data)
        } else {
          uni.showToast({ title: res.data?.message || '请求失败', icon: 'none' })
          reject(res.data)
        }
      },
      fail(err) {
        uni.showToast({ title: '网络开小差了', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default { request }
