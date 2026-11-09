import { defineStore } from 'pinia'
import { api } from '../api'

interface UserVO {
  id: number
  phone: string
  nickname: string
  role: string
}

interface LoginResp {
  token: string
  user: UserVO
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null') as UserVO | null,
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
  },
  actions: {
    async login(phone: string, password: string) {
      const res = await api.post<never, { data: { data: LoginResp } }>('/auth/login', { phone, password })
      this.setSession(res.data.data)
    },
    async register(phone: string, password: string, nickname?: string) {
      const res = await api.post<never, { data: { data: LoginResp } }>('/auth/register', {
        phone,
        password,
        nickname,
      })
      this.setSession(res.data.data)
    },
    setSession(data: LoginResp) {
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
  },
})
