import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from './router'

/**
 * 机构端 API 客户端：
 * - 与 H5 端共用一套后端，token 单独存 org_token（同机演示两个端互不影响）
 * - code !== 0 统一提示；401 清会话回登录页
 */
export const api = axios.create({ baseURL: '/api', timeout: 15000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('org_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('org_token')
      localStorage.removeItem('org_user')
      router.replace('/login')
      ElMessage.warning('登录已过期，请重新登录')
    } else {
      ElMessage.error(err.response?.data?.msg || '网络异常，请稍后再试')
    }
    return Promise.reject(err)
  },
)

export async function getData<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await api.get<never, { data: { data: T } }>(url, { params })
  return res.data.data
}

export async function postData<T>(url: string, data?: unknown): Promise<T> {
  const res = await api.post<never, { data: { data: T } }>(url, data)
  return res.data.data
}

export async function putData<T>(url: string, data?: unknown): Promise<T> {
  const res = await api.put<never, { data: { data: T } }>(url, data)
  return res.data.data
}

export async function delData<T>(url: string): Promise<T> {
  const res = await api.delete<never, { data: { data: T } }>(url)
  return res.data.data
}

export function saveSession(token: string, user: unknown) {
  localStorage.setItem('org_token', token)
  localStorage.setItem('org_user', JSON.stringify(user))
}

export function currentUser(): { nickname?: string } {
  try {
    return JSON.parse(localStorage.getItem('org_user') || '{}')
  } catch {
    return {}
  }
}

export function logout() {
  localStorage.removeItem('org_token')
  localStorage.removeItem('org_user')
  router.replace('/login')
}
