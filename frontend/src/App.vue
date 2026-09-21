<template>
  <div v-if="!store.user" class="auth-wrap">
    <router-view />
  </div>
  <div v-else class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-title">杏林春中药房</div>
        <div class="brand-sub">代煎配送 · 用药回访平台</div>
      </div>
      <nav>
        <router-link v-for="m in menus" :key="m.path" :to="m.path" class="nav-item"
                     :class="{ active: isActive(m.path) }">{{ m.name }}</router-link>
      </nav>
      <div class="side-user">
        <div>{{ store.user.name }}</div>
        <div class="muted">{{ ROLE_NAMES[store.user.role] }}<span v-if="store.user.clinic"> · {{ store.user.clinic.name }}</span></div>
        <button class="btn btn-ghost btn-sm" @click="logout">退出登录</button>
      </div>
    </aside>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { store, ROLE_NAMES } from './store'

const route = useRoute()
const router = useRouter()

const ALL_MENUS = [
  { path: '/', name: '工作台', roles: ['ADMIN', 'PHARMACIST', 'DECOCTER', 'COURIER', 'FINANCE', 'CLINIC', 'PATIENT'] },
  { path: '/prescriptions/new', name: '提交处方', roles: ['PATIENT', 'CLINIC', 'ADMIN'] },
  { path: '/prescriptions', name: '处方列表', roles: ['ADMIN', 'PHARMACIST', 'CLINIC', 'PATIENT', 'DECOCTER', 'COURIER'] },
  { path: '/review', name: '审方台', roles: ['PHARMACIST', 'ADMIN'] },
  { path: '/decoct', name: '煎药台', roles: ['DECOCTER', 'ADMIN', 'PHARMACIST'] },
  { path: '/delivery', name: '配送台', roles: ['COURIER', 'ADMIN', 'PHARMACIST'] },
  { path: '/exceptions', name: '异常中心', roles: ['ADMIN', 'PHARMACIST', 'DECOCTER', 'COURIER', 'FINANCE'] },
  { path: '/followup', name: '回访管理', roles: ['PHARMACIST', 'ADMIN'] },
  { path: '/finance', name: '财务发票', roles: ['FINANCE', 'ADMIN', 'PHARMACIST', 'CLINIC', 'PATIENT'] },
  { path: '/stats', name: '统计复盘', roles: ['ADMIN', 'PHARMACIST'] },
  { path: '/herbs', name: '药材库存', roles: ['ADMIN', 'PHARMACIST', 'DECOCTER', 'COURIER', 'FINANCE', 'CLINIC', 'PATIENT'] }
]

const menus = computed(() => ALL_MENUS.filter(m => m.roles.includes(store.user.role)))

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function logout() {
  store.logout()
  router.push('/login')
}
</script>
