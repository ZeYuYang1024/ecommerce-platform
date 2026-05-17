import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './styles/theme.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import axios from 'axios'
import App from './App.vue'
import router from './router'
import { clearAuthContext, ensureMerchantContext } from './utils/auth'

const MAX_SAFE = 9007199254740991

axios.defaults.transformResponse = [(data) => {
  if (typeof data === 'string') {
    try {
      return JSON.parse(data, (key, value) => {
        if (typeof value === 'number' && Number.isInteger(value) && (value > MAX_SAFE || value < -MAX_SAFE)) {
          return String(value)
        }
        return value
      })
    } catch {
      return data
    }
  }
  return data
}]

axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

ensureMerchantContext()

axios.interceptors.response.use(
  res => res,
  (err) => {
    if (err.response && err.response.status === 401) {
      clearAuthContext()
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
