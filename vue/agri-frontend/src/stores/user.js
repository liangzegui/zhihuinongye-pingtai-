import { defineStore } from 'pinia'
import { getToken, setToken, getUsername, setUsername, getRole, setRole, clearAuthInfo } from '@/utils/token'

/**
 * 用户状态管理
 * 集中管理登录状态、用户信息和角色权限
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    username: getUsername(),
    role: getRole()
  }),

  getters: {
    /** 是否已登录 */
    isLoggedIn: (state) => !!state.token,
    /** 是否管理员 */
    isAdmin: (state) => state.role === 'admin'
  },

  actions: {
    /**
     * 设置登录认证信息
     * @param {string} token - JWT Token
     * @param {string} username - 用户名
     * @param {string} role - 角色（admin/user）
     */
    setAuth(token, username, role) {
      this.token = token
      this.username = username
      this.role = role
      setToken(token)
      setUsername(username)
      setRole(role)
    },

    /**
     * 退出登录，清除所有认证信息
     */
    logout() {
      this.token = ''
      this.username = ''
      this.role = ''
      clearAuthInfo()
    }
  }
})
