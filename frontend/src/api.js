import axios from 'axios'
import { store } from './store'

const api = axios.create({ baseURL: '/api', timeout: 15000 })

api.interceptors.request.use(config => {
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`
  }
  return config
})

api.interceptors.response.use(
  resp => resp.data,
  err => {
    const msg = err.response?.data?.message || err.message || '请求失败'
    if (err.response?.status === 401) {
      store.logout()
      if (location.hash !== '#/login') location.hash = '#/login'
    }
    return Promise.reject(new Error(msg))
  }
)

export default api
