import { request } from '@/utils/api'
import { clearSession, saveSession, token, userInfo } from '@/utils/session'

async function login(username, password) {
  const res = await request({
    url: '/api/v1/auth/login',
    method: 'POST',
    data: { username, password },
    auth: false
  })
  if (res.code === 200 && res.data?.token) saveSession(res.data)
  return res
}

export function useAuthStore() {
  return {
    get token() { return token.value },
    get userInfo() { return userInfo.value },
    get isLogin() { return Boolean(token.value) },
    login,
    logout: clearSession
  }
}
