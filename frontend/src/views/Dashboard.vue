<template>
  <div>
    <div class="page-title">工作台</div>
    <div class="page-sub">处方履约全流程概览（{{ today }}）</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div class="grid grid-4 mb16">
      <div class="stat-card"><div class="stat-num">{{ d.todayCount ?? '—' }}</div><div class="stat-label">今日新增处方</div></div>
      <div class="stat-card"><div class="stat-num">{{ sc('PENDING_REVIEW') }}</div><div class="stat-label">待审方</div></div>
      <div class="stat-card"><div class="stat-num">{{ d.decoctQueued ?? '—' }}</div><div class="stat-label">代煎排队</div></div>
      <div class="stat-card"><div class="stat-num">{{ d.decocting ?? '—' }}</div><div class="stat-label">煎药中</div></div>
      <div class="stat-card"><div class="stat-num">{{ d.delivering ?? '—' }}</div><div class="stat-label">配送中</div></div>
      <div class="stat-card"><div class="stat-num">{{ d.pendingFollowUp ?? '—' }}</div><div class="stat-label">待回访</div></div>
      <div class="stat-card"><div class="stat-num" :class="{ 'danger-text': d.openExceptions > 0 }">{{ d.openExceptions ?? '—' }}</div><div class="stat-label">未结异常</div></div>
      <div class="stat-card"><div class="stat-num">{{ fmtMoney(d.totalCompensation) }}</div><div class="stat-label">累计赔付</div></div>
    </div>

    <div class="card">
      <div class="card-title">处方状态分布</div>
      <table>
        <thead><tr><th>状态</th><th>数量</th><th>占比</th></tr></thead>
        <tbody>
          <tr v-for="(v, k) in d.statusCounts" :key="k">
            <td><span class="badge" :class="STATUS_CLASS[k]">{{ STATUS_NAMES[k] || k }}</span></td>
            <td>{{ v }}</td>
            <td style="width:40%">
              <div class="bar-wrap"><div class="bar" :style="{ width: pct(v) + '%' }"></div></div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api'
import { STATUS_NAMES, STATUS_CLASS, fmtMoney } from '../dict'

const d = ref({})
const error = ref('')
const today = new Date().toLocaleDateString('zh-CN')

function sc(k) {
  return d.value.statusCounts ? d.value.statusCounts[k] ?? 0 : '—'
}
function pct(v) {
  const total = Object.values(d.value.statusCounts || {}).reduce((a, b) => a + b, 0)
  return total ? Math.round((v / total) * 100) : 0
}

onMounted(async () => {
  try {
    d.value = await api.get('/stats/dashboard')
  } catch (e) {
    error.value = e.message
  }
})
</script>
