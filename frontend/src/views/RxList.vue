<template>
  <div>
    <div class="page-title">📋 处方履约
      <router-link v-if="can(['PATIENT','CLINIC'])" to="/prescriptions/new" class="btn sm" style="margin-left:auto">+ 提交处方</router-link>
      <router-link v-if="can(['CLINIC'])" to="/prescriptions/new?batch=1" class="btn sm secondary">批量开方</router-link>
    </div>
    <div class="card">
      <div class="flex" style="margin-bottom:10px;flex-wrap:wrap">
        <input v-model="kw" placeholder="搜索处方号/患者/医生" style="max-width:240px" />
        <select v-model="statusFilter" style="max-width:180px">
          <option value="">全部状态</option>
          <option v-for="(l, k) in statusLabels" :key="k" :value="k">{{ l }}</option>
        </select>
        <span class="muted small">共 {{ filtered.length }} 条</span>
      </div>
      <table>
        <thead>
          <tr>
            <th>处方号</th><th>患者</th><th>诊所/医生</th><th>剂数</th><th>取药</th>
            <th>结算</th><th>状态</th><th>标签</th><th>提交时间</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in filtered" :key="r.id">
            <td><b>{{ r.rxNo }}</b><div v-if="r.batchNo" class="small muted">{{ r.batchNo }}</div></td>
            <td>{{ r.patientName }}</td>
            <td class="small">{{ r.clinicName || '—' }}<div>{{ r.doctorName }}</div></td>
            <td>{{ r.doses }}</td>
            <td>{{ r.pickupMethodLabel }}</td>
            <td>{{ r.settlementLabel }}</td>
            <td><span class="tag" :class="tagClass(r.status)">{{ r.statusLabel }}</span></td>
            <td>
              <span v-if="r.nightUrgent" class="tag red">夜间急煎</span>
              <span v-if="r.addSugar" class="tag blue">加糖</span>
            </td>
            <td class="small muted">{{ fmt(r.submittedAt) }}</td>
            <td><router-link class="btn sm secondary" :to="`/prescriptions/${r.id}`">履约详情</router-link></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { api, getUser } from '../api.js'
import { tagClass, fmt } from '../use.js'

const user = getUser()
const can = (roles) => roles.includes(user.role)
const kw = ref('')
const statusFilter = ref('')
const list = ref([])
const statusLabels = {
  PENDING_REVIEW: '待审方', REJECTED: '已驳回', APPROVED: '待抓药', DISPENSED: '待煎煮',
  READY_PICKUP: '待自取', DECOCTING: '代煎中', DECOCTED: '待配送',
  DELIVERING: '配送中', SIGNED: '已签收', FOLLOWED_UP: '已回访'
}

const filtered = computed(() => list.value.filter(r => {
  const hitKw = !kw.value || [r.rxNo, r.patientName, r.doctorName, r.clinicName]
    .filter(Boolean).some(s => s.includes(kw.value))
  const hitStatus = !statusFilter.value || r.status === statusFilter.value
  return hitKw && hitStatus
}))

onMounted(async () => {
  list.value = await api.get('/prescriptions')
})
</script>
