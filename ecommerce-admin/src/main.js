import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/theme.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import axios from 'axios'
import App from './App.vue'
import router from './router'

const MAX_SAFE = 9007199254740991

// 响应转换：Snowflake Long ID → String，防止 JS 精度丢失
axios.defaults.transformResponse = [(data) => {
  if (typeof data === 'string') {
    try {
      return JSON.parse(data, (key, value) => {
        if (typeof value === 'number' && Number.isInteger(value) && (value > MAX_SAFE || value < -MAX_SAFE))
          return String(value)
        return value
      })
    } catch { return data }
  }
  return data
}]

// 请求拦截：自动带 Token
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：401 跳登录
axios.interceptors.response.use(
  res => res,
  err => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.mount('#app')
