<template>
  <div class="login-card">
    <div class="login-title">杏林春中药房</div>
    <div class="login-sub">县城中药房代煎配送与用药回访平台</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div class="form-item mb8">
      <label>用户名</label>
      <input v-model="username" placeholder="请输入用户名" @keyup.enter="doLogin" />
    </div>
    <div class="form-item mb16">
      <label>密码</label>
      <input v-model="password" type="password" placeholder="请输入密码" @keyup.enter="doLogin" />
    </div>
    <button class="btn" style="width:100%" :disabled="loading" @click="doLogin">
      {{ loading ? '登录中…' : '登 录' }}
    </button>
    <div class="quick-accounts">
      <button v-for="a in accounts" :key="a.u" @click="fill(a)">{{ a.label }}</button>
    </div>
    <div class="muted mt8" style="font-size:11px;text-align:center">点击上方角色可快速填充演示账号</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { store } from '../store'

const router = useRouter()
const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const accounts = [
  { label: '管理者 admin', u: 'admin', p: 'admin123' },
  { label: '药师', u: 'pharmacist', p: '123456' },
  { label: '煎药员', u: 'decocter', p: '123456' },
  { label: '配送员', u: 'courier', p: '123456' },
  { label: '财务', u: 'finance', p: '123456' },
  { label: '诊所', u: 'clinic1', p: '123456' },
  { label: '患者', u: 'patient1', p: '123456' }
]

function fill(a) {
  username.value = a.u
  password.value = a.p
}

async function doLogin() {
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await api.post('/auth/login', { username: username.value, password: password.value })
    store.login(data.token, data.user)
    router.push('/')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>
