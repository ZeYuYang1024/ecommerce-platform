import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<any>(null)
  const token = useCookie('token')
  const username = useCookie('username')

  const isLoggedIn = computed(() => !!token.value)

  async function login(form: { username: string; password: string }) {
    const api = useApi()
    const res: any = await api.post('/auth/login', form)
    if (res.code === 200) {
      token.value = res.data.token
      username.value = res.data.username
      user.value = res.data
      return true
    }
    return false
  }

  async function register(form: { username: string; password: string }) {
    const api = useApi()
    const res: any = await api.post('/auth/register', form)
    if (res.code === 200) {
      token.value = res.data.token
      username.value = res.data.username
      user.value = res.data
      return true
    }
    return false
  }

  function logout() {
    token.value = null
    username.value = null
    user.value = null
    navigateTo('/')
  }

  return { user, token, username, isLoggedIn, login, register, logout }
})
