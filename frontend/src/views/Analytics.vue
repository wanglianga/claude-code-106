<template>
  <div>
    <div class="page-title">📊 药房异常复盘分析</div>

    <div class="grid grid-4">
      <div class="card"><div class="muted">处方总量</div><div class="stat">{{ ov.rxTotal ?? '—' }}</div></div>
      <div class="card"><div class="muted">未结工单</div><div class="stat" style="color:var(--danger)">{{ ov.openIssues ?? '—' }}</div></div>
      <div class="card"><div class="muted">平均满意度</div><div class="stat">{{ ov.avgSatisfaction ?? '—' }}</div></div>
      <div class="card"><div class="muted">赔付总额</div><div class="stat" style="color:var(--warn)">¥{{ ov.compensation ?? '—' }}</div></div>
    </div>

    <div class="card">
      <h3>👨‍⚕️ 按医生复盘(异常/不适/夜煎/合作评分)</h3>
      <table>
        <thead><tr><th>医生</th><th>处方数</th><th>异常工单</th><th>不适反馈</th>
          <th>夜间急煎</th><th>满意度</th><th>合作评分</th></tr></thead>
        <tbody>
          <tr v-for="(r,i) in byDoctor" :key="i">
            <td>{{ r.doctorName }}</td><td>{{ r.rxCount }}</td>
            <td><span :style="{color:r.issueCount?'var(--danger)':'inherit'}">{{ r.issueCount }}</span></td>
            <td><span :style="{color:r.discomfortCount?'var(--danger)':'inherit'}">{{ r.discomfortCount }}</span></td>
            <td>{{ r.nightUrgentCount }}</td><td>{{ r.avgSatisfaction || '—' }}</td>
            <td><b :style="{color:r.cooperationScore>=80?'var(--ok)':r.cooperationScore>=60?'var(--warn)':'var(--danger)'}">
              {{ r.cooperationScore }}</b></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <h3>🌿 按药味复盘(使用/缺药替代/特殊煎法遗漏)</h3>
      <table>
        <thead><tr><th>药味</th><th>使用次数</th><th>被替代次数</th><th>特殊煎法遗漏关联</th></tr></thead>
        <tbody>
          <tr v-for="(r,i) in byHerb" :key="i">
            <td>{{ r.herbName }}</td><td>{{ r.usedCount }}</td>
            <td><span :style="{color:r.substitutedCount?'var(--warn)':'inherit'}">{{ r.substitutedCount }}</span></td>
            <td><span :style="{color:r.specialMissedCount?'var(--danger)':'inherit'}">{{ r.specialMissedCount }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="grid grid-2">
      <div class="card">
        <h3>🍵 按锅号复盘</h3>
        <table>
          <thead><tr><th>锅号</th><th>任务数</th><th>漏袋</th><th>破损</th><th>异常气味</th><th>关联超时</th></tr></thead>
          <tbody>
            <tr v-for="(r,i) in byPot" :key="i">
              <td><b>{{ r.potNo }}</b></td><td>{{ r.taskCount }}</td>
              <td :style="{color:r.missingBags?'var(--danger)':'inherit'}">{{ r.missingBags }}</td>
              <td :style="{color:r.damagedBags?'var(--danger)':'inherit'}">{{ r.damagedBags }}</td>
              <td>{{ r.abnormalSmellCount }}</td><td>{{ r.relatedTimeout }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="card">
        <h3>🛵 按配送员复盘</h3>
        <table>
          <thead><tr><th>配送员</th><th>单量</th><th>超时次数</th><th>平均超时(分)</th></tr></thead>
          <tbody>
            <tr v-for="(r,i) in byCourier" :key="i">
              <td>{{ r.courierName }}<div class="small muted">{{ r.phone }}</div></td>
              <td>{{ r.deliveryCount }}</td>
              <td :style="{color:r.timeoutCount?'var(--danger)':'inherit'}">{{ r.timeoutCount }}</td>
              <td>{{ r.avgTimeoutMinutes || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <h3>🏥 诊所合作评分</h3>
      <table>
        <thead><tr><th>诊所</th><th>处方数</th><th>异常</th><th>未结</th><th>不适</th>
          <th>满意度</th><th>合作评分</th></tr></thead>
        <tbody>
          <tr v-for="(r,i) in clinics" :key="i">
            <td>{{ r.clinicName }}</td><td>{{ r.rxCount }}</td><td>{{ r.issueCount }}</td>
            <td :style="{color:r.openIssueCount?'var(--warn)':'inherit'}">{{ r.openIssueCount }}</td>
            <td :style="{color:r.discomfortCount?'var(--danger)':'inherit'}">{{ r.discomfortCount }}</td>
            <td>{{ r.avgSatisfaction || '—' }}</td>
            <td><b :style="{color:r.cooperationScore>=80?'var(--ok)':r.cooperationScore>=60?'var(--warn)':'var(--danger)'}">
              {{ r.cooperationScore }}</b></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api.js'

const ov = ref({})
const byDoctor = ref([])
const byHerb = ref([])
const byPot = ref([])
const byCourier = ref([])
const clinics = ref([])

onMounted(async () => {
  const [o, d, h, p, c, cl] = await Promise.all([
    api.get('/analytics/overview'),
    api.get('/analytics/by-doctor'),
    api.get('/analytics/by-herb'),
    api.get('/analytics/by-pot'),
    api.get('/analytics/by-courier'),
    api.get('/analytics/clinics')
  ])
  ov.value = o; byDoctor.value = d; byHerb.value = h
  byPot.value = p; byCourier.value = c; clinics.value = cl
})
</script>
