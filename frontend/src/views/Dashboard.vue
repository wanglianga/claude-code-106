<template>
  <div>
    <div class="page-title">🏠 工作台 <span class="muted small">欢迎,{{ user.displayName }}({{ user.roleLabel }})</span></div>

    <div class="grid grid-4" v-if="can(['ADMIN','PHARMACIST','FINANCE'])">
      <div class="card"><div class="muted">处方总量</div><div class="stat">{{ ov.rxTotal ?? '—' }}</div></div>
      <div class="card"><div class="muted">未结异常工单</div><div class="stat" style="color:var(--danger)">{{ ov.openIssues ?? '—' }}</div></div>
      <div class="card"><div class="muted">自付收入(元)</div><div class="stat">{{ ov.selfPayRevenue ?? '—' }}</div></div>
      <div class="card"><div class="muted">医保支付(元)</div><div class="stat">{{ ov.insuranceRevenue ?? '—' }}</div></div>
      <div class="card"><div class="muted">药房赔付(元)</div><div class="stat" style="color:var(--warn)">{{ ov.compensation ?? '—' }}</div></div>
      <div class="card"><div class="muted">平均满意度</div><div class="stat">{{ ov.avgSatisfaction ?? '—' }}</div></div>
      <div class="card"><div class="muted">服药不适反馈</div><div class="stat" style="color:var(--danger)">{{ ov.discomfortCount ?? '—' }}</div></div>
      <div class="card">
        <div class="muted">处方状态分布</div>
        <div class="small mt8">
          <span v-for="(c, k) in ov.statusCount" :key="k" style="margin-right:8px">
            {{ statusLabel(k) }} <b>{{ c }}</b>
          </span>
        </div>
      </div>
    </div>

    <div class="grid grid-2">
      <div class="card" v-if="can(['PHARMACIST','ADMIN'])">
        <h3>🔍 待审处方({{ pending.length }})</h3>
        <table>
          <tr v-for="r in pending.slice(0,6)" :key="r.id">
            <td><router-link :to="`/prescriptions/${r.id}`">{{ r.rxNo }}</router-link></td>
            <td>{{ r.patientName }}</td>
            <td>{{ r.doses }} 剂</td>
            <td><span class="tag orange">{{ r.settlementLabel }}</span></td>
            <td><span v-if="r.nightUrgent" class="tag red">夜间急煎</span></td>
          </tr>
        </table>
        <div v-if="!pending.length" class="muted small">暂无待审处方</div>
      </div>

      <div class="card" v-if="can(['DECOCTOR'])">
        <h3>🍵 代煎任务({{ tasks.length }})</h3>
        <table>
          <tr v-for="t in tasks.slice(0,8)" :key="t.rxId">
            <td>{{ t.rxNo }}</td>
            <td>{{ t.patientName }}</td>
            <td>{{ t.statusLabel }}</td>
            <td><span v-if="t.nightUrgent" class="tag red">急</span></td>
          </tr>
        </table>
      </div>

      <div class="card" v-if="can(['COURIER'])">
        <h3>🛵 我的配送({{ deliveries.length }})</h3>
        <table>
          <tr v-for="t in deliveries.slice(0,8)" :key="t.rxId">
            <td>{{ t.rxNo }}</td><td>{{ t.patientName }}</td>
            <td>{{ t.statusLabel }}</td>
            <td><span v-if="t.timeoutMinutes" class="tag red">超时{{ t.timeoutMinutes }}分</span></td>
          </tr>
        </table>
      </div>

      <div class="card">
        <h3>⚠️ 最新异常工单({{ issues.length }})</h3>
        <table>
          <tr v-for="i in issues.slice(0,7)" :key="i.id">
            <td>{{ i.issueNo }}</td>
            <td>{{ i.typeLabel }}</td>
            <td><span class="tag" :class="i.status==='RESOLVED'?'green':'red'">{{ i.statusLabel }}</span></td>
            <td class="small">{{ i.rxNo }}</td>
          </tr>
        </table>
      </div>

      <div class="card">
        <h3>📋 我的/最新处方({{ rxes.length }})</h3>
        <table>
          <tr v-for="r in rxes.slice(0,7)" :key="r.id">
            <td><router-link :to="`/prescriptions/${r.id}`">{{ r.rxNo }}</router-link></td>
            <td>{{ r.patientName }}</td>
            <td><span class="tag" :class="tagClass(r.status)">{{ statusLabel(r.status) }}</span></td>
            <td class="small muted">{{ fmt(r.submittedAt) }}</td>
          </tr>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api, getUser } from '../api.js'
import { tagClass, fmt } from '../use.js'

const user = getUser()
const ov = ref({})
const rxes = ref([])
const pending = ref([])
const tasks = ref([])
const deliveries = ref([])
const issues = ref([])

const can = (roles) => roles.includes(user.role)
const statusLabel = (s) => ({
  PENDING_REVIEW: '待审方', REJECTED: '已驳回', APPROVED: '待抓药', DISPENSED: '待煎煮',
  READY_PICKUP: '待自取', DECOCTING: '代煎中', DECOCTED: '待配送',
  DELIVERING: '配送中', SIGNED: '已签收', FOLLOWED_UP: '已回访'
}[s] || s)

onMounted(async () => {
  rxes.value = await api.get('/prescriptions')
  pending.value = rxes.value.filter(r => r.status === 'PENDING_REVIEW')
  issues.value = await api.get('/issues')
  if (can(['ADMIN', 'PHARMACIST', 'FINANCE'])) {
    ov.value = await api.get('/analytics/overview').catch(() => ({}))
  }
  if (can(['DECOCTOR'])) tasks.value = await api.get('/decoct/queue').catch(() => [])
  if (can(['COURIER'])) deliveries.value = (await api.get('/delivery/tasks').catch(() => []))
})
</script>
