import { defineStore } from 'pinia'
import { login, getInfo } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { resetRouter } from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    phone: '',
    nickname: '',
    avatar: '',
    roles: []
  }),

  getters: {
    isLoggedIn: (state) => !!state.token
  },

  actions: {
    // 用户登录
    async loginAction(userInfo) {
      const { phone, password } = userInfo
      const { data } = await login({ phone: phone.trim(), password })
      this.token = data.token
      setToken(data.token)
    },

    // 获取用户信息
    async getInfoAction() {
      const { data } = await getInfo()
      if (!data) {
        return Promise.reject('获取用户信息失败，请重新登录。')
      }
      this.phone = data.phone
      this.nickname = data.nickname || data.phone
      this.avatar = data.avatar || ''
      this.roles = data.roles || []
      return data
    },

    // 用户登出
    logoutAction() {
      removeToken()
      resetRouter()
      this.$reset()
    },

    // 重置 Token
    resetTokenAction() {
      removeToken()
      this.$reset()
    }
  }
})
