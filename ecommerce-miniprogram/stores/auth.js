import { ref, computed } from 'vue'
import { request } from '@/utils/api'

const token = ref(uni.getStorageSync('token') || '')
const userInfo = ref(null)
const isLogin = computed(() => !!token.value)

async function login(username, password) {
  const res = await request({
    url: '/api/v1/auth/login',
    method: 'POST',
    data: { username, password }
  })
  if (res.code === 200) {
    token.value = res.data.token
    userInfo.value = res.data
    uni.setStorageSync('token', res.data.token)
  }
  return res
}

async function wxLogin() {
  try {
    const { code } = await uni.login({ provider: 'weixin' })
    const res = await request({
      url: '/api/v1/auth/wx-login',
      method: 'POST',
      data: { code }
    })
    if (res.code === 200) {
      token.value = res.data.token
      userInfo.value = res.data
      uni.setStorageSync('token', res.data.token)
    }
    return res
  } catch (e) {
    console.error('wx login failed', e)
  }
}

function logout() {
  token.value = ''
  userInfo.value = null
  uni.removeStorageSync('token')
}

export function useAuthStore() {
  return {
    get token() { return token.value },
    get userInfo() { return userInfo.value },
    get isLogin() { return isLogin.value },
    login,
    wxLogin,
    logout
  }
}
