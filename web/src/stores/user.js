import { defineStore } from 'pinia'
import { ref } from 'vue'

const TOKEN_KEY = 'cg_token'
const USER_KEY = 'cg_user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

  function setLogin(data) {
    token.value = data.token
    userInfo.value = {
      userId: data.userId,
      username: data.username,
      nickname: data.nickname,
      role: data.role
    }
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(userInfo.value))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, userInfo, setLogin, logout }
})
