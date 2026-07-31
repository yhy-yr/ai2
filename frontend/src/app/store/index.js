import { defineStore } from 'pinia'
import api from '../../services/api'

function readUser() {
  const raw = localStorage.getItem('user')
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    localStorage.removeItem('user')
    return null
  }
}

export function roleHome(role) {
  return role ? '/workspace' : '/login'
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    sessionToken: localStorage.getItem('sessionToken') || '',
    user: readUser(),
    bootstrapDone: false
  }),
  getters: {
    isLoggedIn(state) {
      return Boolean(state.sessionToken && state.user)
    }
  },
  actions: {
    saveSession(sessionToken, user) {
      this.sessionToken = sessionToken
      this.user = user
      localStorage.setItem('sessionToken', sessionToken)
      localStorage.setItem('user', JSON.stringify(user))
    },
    clearSession() {
      this.sessionToken = ''
      this.user = null
      localStorage.removeItem('sessionToken')
      localStorage.removeItem('user')
    },
    async login(username, password) {
      const data = await api.login({ username, password })
      this.saveSession(data.sessionToken, data.user)
      this.bootstrapDone = true
      return data.user
    },
    async logout() {
      try {
        if (this.sessionToken) {
          await api.logout()
        }
      } finally {
        this.clearSession()
        this.bootstrapDone = true
      }
    },
    async bootstrap() {
      if (!this.sessionToken) {
        this.clearSession()
        this.bootstrapDone = true
        return
      }

      try {
        const user = await api.me()
        this.user = user
        localStorage.setItem('user', JSON.stringify(user))
      } catch (error) {
        this.clearSession()
      } finally {
        this.bootstrapDone = true
      }
    }
  }
})
