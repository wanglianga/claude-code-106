<template>
  <div style="display:flex;min-height:100vh">
    <aside style="width:210px;background:#163a2d;color:#dcebe4;display:flex;flex-direction:column">
      <div style="padding:18px 16px;font-size:16px;font-weight:700;border-bottom:1px solid #24513f">
        🌿 同济堂中药房
      </div>
      <nav style="padding:10px 0;flex:1">
        <router-link v-for="m in menus" :key="m.path" :to="m.path"
          class="menu-item" active-class="active">
          <span style="margin-right:8px">{{ m.icon }}</span>{{ m.name }}
        </router-link>
      </nav>
      <div style="padding:12px 16px;border-top:1px solid #24513f;font-size:12px;color:#9cb8ac">
        <div>{{ user?.displayName }} · {{ user?.roleLabel }}</div>
        <div v-if="user?.organization">{{ user.organization }}</div>
        <a href="#" @click.prevent="logout" style="color:#8fd0b4">退出登录</a>
      </div>
    </aside>
    <main style="flex:1;padding:20px;overflow:auto;max-width:calc(100vw - 210px)">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getUser, clearAuth } from '../api.js'

const router = useRouter()
const user = getUser()

const ALL = [
  { path: '/', name: '工作台', icon: '🏠', roles: ['*'] },
  { path: '/prescriptions', name: '处方履约', icon: '📋', roles: ['*'] },
  { path: '/prescriptions/new', name: '提交处方', icon: '✍️', roles: ['PATIENT', 'CLINIC'] },
  { path: '/review', name: '药师审方', icon: '🔍', roles: ['PHARMACIST'] },
  { path: '/decoct', name: '煎药作业', icon: '🍵', roles: ['DECOCTOR', 'PHARMACIST', 'ADMIN'] },
  { path: '/delivery', name: '配送调度', icon: '🛵', roles: ['COURIER', 'PHARMACIST', 'ADMIN', 'FINANCE'] },
  { path: '/issues', name: '异常工单', icon: '⚠️', roles: ['*'] },
  { path: '/reminders', name: '用药提醒', icon: '⏰', roles: ['PHARMACIST', 'ADMIN', 'FINANCE'] },
  { path: '/finance', name: '财务发票', icon: '🧾', roles: ['FINANCE', 'ADMIN', 'CLINIC'] },
  { path: '/analytics', name: '异常复盘', icon: '📊', roles: ['ADMIN', 'PHARMACIST'] }
]

const menus = computed(() => ALL.filter(m =>
  m.roles.includes('*') || m.roles.includes(user.role)))

function logout() {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.menu-item {
  display: block; padding: 11px 18px; color: #dcebe4; font-size: 14px;
}
.menu-item:hover { background: #1d4a38; }
.menu-item.active { background: var(--primary); color: #fff; }
</style>
