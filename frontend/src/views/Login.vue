<template>
  <div class="login-wrap">
    <div class="login-box">
      <h2 style="color:var(--primary-dark)">同济堂中药房平台</h2>
      <p class="muted">县城中药房代煎配送与用药回访平台</p>
      <label>用户名</label>
      <input v-model="username" placeholder="如 pharmacist" @keyup.enter="login" />
      <label>密码</label>
      <input v-model="password" type="password" placeholder="演示密码 123456" @keyup.enter="login" />
      <button class="btn" style="width:100%;margin-top:16px" :disabled="loading" @click="login">
        {{ loading ? '登录中…' : '登 录' }}
      </button>
      <p v-if="err" class="small" style="color:var(--danger);margin-top:10px">{{ err }}</p>
      <div class="mt16 small muted">
        <div>演示账号(密码均为 123456):</div>
        <div class="pill-btns" style="margin-top:6px">
          <button class="btn sm secondary" v-for="a in accounts" :key="a.u"
                  @click="username=a.u;password='123456'">
            {{ a.label }} {{ a.u }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, setAuth } from '../api.js'

const router = useRouter()
const username = ref('pharmacist')
const password = ref('123456')
const err = ref('')
const loading = ref(false)

const accounts = [
  { u: 'patient', label: '患者' },
  { u: 'clinic', label: '诊所' },
  { u: 'pharmacist', label: '药师' },
  { u: 'decoctor', label: '煎药员' },
  { u: 'courier', label: '配送员' },
  { u: 'finance', label: '财务' },
  { u: 'admin', label: '管理员' }
]

async function login() {
  err.value = ''
  loading.value = true
  try {
    const data = await api.post('/auth/login', { username: username.value, password: password.value })
    setAuth(data.token, data.user)
    router.push('/')
  } catch (e) {
    err.value = e.message
  } finally {
    loading.value = false
  }
}
</script>
