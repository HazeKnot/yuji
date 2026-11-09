import { api } from './index'

/**
 * 把 axios 的 { data: { data } } 收敛成业务数据本身。
 * 页面只关心 T，不再重复写三层泛型。
 */
export async function get<T>(url: string, config?: Record<string, unknown>): Promise<T> {
  const res = await api.get<never, { data: { data: T } }>(url, config)
  return res.data.data
}

export async function post<T>(url: string, data?: unknown): Promise<T> {
  const res = await api.post<never, { data: { data: T } }>(url, data)
  return res.data.data
}

export async function put<T>(url: string, data?: unknown): Promise<T> {
  const res = await api.put<never, { data: { data: T } }>(url, data)
  return res.data.data
}

export async function del<T>(url: string): Promise<T> {
  const res = await api.delete<never, { data: { data: T } }>(url)
  return res.data.data
}
