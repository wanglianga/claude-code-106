import { reactive } from 'vue'

export const store = reactive({
  token: localStorage.getItem('token') || '',
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  login(token, user) {
    this.token = token
    this.user = user
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
  },
  logout() {
    this.token = ''
    this.user = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }
})

export const ROLE_NAMES = {
  ADMIN: '药房管理者',
  PHARMACIST: '药师',
  DECOCTER: '煎药员',
  COURIER: '配送员',
  FINANCE: '财务',
  CLINIC: '诊所',
  PATIENT: '患者'
}
