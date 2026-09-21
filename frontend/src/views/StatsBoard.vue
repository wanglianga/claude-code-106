<template>
  <div>
    <div class="page-title">统计复盘</div>
    <div class="page-sub">按医生、药味、锅号、配送员复盘异常，指导药房管理与诊所合作</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div class="card">
      <div class="flex mb16">
        <button v-for="(n, k) in dims" :key="k" class="btn btn-sm"
                :class="dim === k ? '' : 'btn-ghost'" @click="dim = k; load()">{{ n }}</button>
      </div>
      <table>
        <thead><tr><th>{{ dims[dim] }}</th><th>异常次数</th><th>占比</th><th>涉及赔付</th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="row.name">
            <td>{{ row.name }}</td>
            <td>{{ row.count }}</td>
            <td style="width:38%"><div class="bar-wrap"><div class="bar" :style="{ width: pct(row.count) + '%' }"></div></div></td>
            <td>{{ fmtMoney(row.compensation) }}</td>
          </tr>
          <tr v-if="!rows.length"><td colspan="4" class="muted" style="text-align:center">暂无异常数据</td></tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <div class="card-title">诊所合作评分</div>
      <table>
        <thead><tr><th>诊所</th><th>联系人</th><th>合作评分</th><th>评分</th><th>处方量</th></tr></thead>
        <tbody>
          <tr v-for="c in clinics" :key="c.clinic.id">
            <td>{{ c.clinic.name }}</td>
            <td>{{ c.clinic.contactPerson }} {{ c.clinic.phone }}</td>
            <td><b :class="c.clinic.cooperationScore >= 90 ? 'ok-text' : c.clinic.cooperationScore >= 75 ? 'warn-text' : 'danger-text'">{{ c.clinic.cooperationScore.toFixed(1) }}</b></td>
            <td style="width:32%"><div class="bar-wrap"><div class="bar" :style="{ width: c.clinic.cooperationScore + '%' }"></div></div></td>
            <td>{{ c.prescriptionCount }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api'
import { fmtMoney } from '../dict'

const dims = { doctor: '按医生', herb: '按药味', pot: '按锅号', courier: '按配送员' }
const dim = ref('doctor')
const rows = ref([])
const clinics = ref([])
const error = ref('')
const maxCount = ref(1)

function pct(v) {
  return Math.round((v / maxCount.value) * 100)
}

async function load() {
  try {
    rows.value = await api.get('/stats/exceptions', { params: { dim: dim.value } })
    maxCount.value = Math.max(1, ...rows.value.map(r => r.count))
  } catch (e) {
    error.value = e.message
  }
}

onMounted(async () => {
  await load()
  try {
    clinics.value = await api.get('/stats/clinics')
  } catch (e) {
    error.value = e.message
  }
})
</script>
