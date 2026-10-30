import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { ElForm, ElFormItem, ElIcon, ElDivider, ElCard, ElButton, ElInput, ElDialog } from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/theme.css'
import './styles/layout.css'
import './styles/admin.css'
import './styles/workspace.css'
import './styles/catalog.css'
import axios from 'axios'
import { dropExpiredSession, clearAuth } from './api/auth'
import { getProfile } from './api/user'

axios.defaults.timeout = 15000
axios.interceptors.response.use((res) => {
  if (typeof res.data?.code === 'number' && res.data.code !== 200) {
    throw new Error(res.data.message || '请求失败')
  }
  return res
}, (error) => {
  if (error.response?.status === 401) dropExpiredSession()
  if (error.response?.status === 403 && error.response?.data?.message?.includes('禁用')) clearAuth()
  error.message = error.response?.data?.message || '暂时无法连接服务器，请稍后重试'
  return Promise.reject(error)
})

if ((localStorage.getItem('token') || sessionStorage.getItem('token')) && !location.pathname.startsWith('/login')) {
  getProfile().catch(() => {})
}

const app = createApp(App)

app.use(createPinia())
app.use(router)
for (const component of [ElForm, ElFormItem, ElIcon, ElDivider, ElCard, ElButton, ElInput, ElDialog]) {
  app.component(component.name, component)
}

app.mount('#app')
