import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useUserStore } from '../stores/user'

// 统一请求封装：相对路径 /api 由 Vite 代理转发到后端 8080
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const user = useUserStore()
  if (user.token) {
    config.headers.Authorization = `Bearer ${user.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一响应结构：{ code, message, data }
    if (res && res.code === 200) {
      return res.data
    }
    ElMessage.error((res && res.message) || '请求失败')
    return Promise.reject(new Error((res && res.message) || 'Error'))
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      useUserStore().logout()
      router.push('/login')
    } else {
      const msg = error.response && error.response.data && error.response.data.message
      ElMessage.error(msg || '网络错误，请确认后端已启动')
    }
    return Promise.reject(error)
  }
)

export default request
