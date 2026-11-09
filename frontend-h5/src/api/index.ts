import axios from 'axios'
import { showFailToast } from 'vant'
import router from '../router'

/**
 * 统一 API 客户端：
 * - 请求头自动携带 JWT
 * - 响应体 {code, msg, data}，code !== 0 弹错误提示
 * - 401 清除会话并跳登录页
 */
export const api = axios.create({ baseURL: '/api', timeout: 15000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      showFailToast(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg))
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/login')
    } else {
      showFailToast(err.response?.data?.msg || '网络开小差了，请稍后再试')
    }
    return Promise.reject(err)
  },
)
