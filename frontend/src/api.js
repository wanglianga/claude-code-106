const BASE = '/api'

export function getToken() {
  return localStorage.getItem('tcm_token') || ''
}
export function getUser() {
  const raw = localStorage.getItem('tcm_user')
  return raw ? JSON.parse(raw) : null
}
export function setAuth(token, user) {
  localStorage.setItem('tcm_token', token)
  localStorage.setItem('tcm_user', JSON.stringify(user))
}
export function clearAuth() {
  localStorage.removeItem('tcm_token')
  localStorage.removeItem('tcm_user')
}

async function request(method, path, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(getToken() ? { Authorization: 'Bearer ' + getToken() } : {})
    },
    body: body !== undefined ? JSON.stringify(body) : undefined
  })
  if (res.status === 401) {
    clearAuth()
    if (!location.hash.startsWith('#/login')) location.hash = '#/login'
    throw new Error('未登录或登录已失效')
  }
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    throw new Error(data?.message || ('请求失败(' + res.status + ')'))
  }
  return data
}

export const api = {
  get: (p) => request('GET', p),
  post: (p, b) => request('POST', p, b ?? {}),
  patch: (p, b) => request('PATCH', p, b ?? {})
}
