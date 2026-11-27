import { shallowRef } from 'vue'

export const token = shallowRef(uni.getStorageSync('token') || '')
export const userInfo = shallowRef(token.value ? uni.getStorageSync('userInfo') || null : null)

export function saveSession(user) {
  token.value = user.token
  userInfo.value = { username: user.username, userId: user.userId }
  uni.setStorageSync('token', token.value)
  uni.setStorageSync('userInfo', userInfo.value)
}

export function clearSession() {
  token.value = ''
  userInfo.value = null
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
}
