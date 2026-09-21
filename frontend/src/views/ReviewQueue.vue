<template>
  <div>
    <div class="page-title">🔍 药师审方队列</div>
    <div class="card">
      <p class="small muted">逐张核对十八反十九畏、剂量异常、缺药替代、医保目录限制;审方结论驱动抓药、收费与代煎排队。点击进入完整履约详情执行审方。</p>
      <table>
        <thead>
          <tr><th>处方号</th><th>患者</th><th>医生/诊所</th><th>剂数</th><th>药味</th>
            <th>结算</th><th>取药</th><th>标记</th><th>实时校验</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="r in pending" :key="r.id">
            <td><b>{{ r.rxNo }}</b></td>
            <td>{{ r.patientName }}<div class="small muted">{{ r.contraindications }}</div></td>
            <td class="small">{{ r.doctorName }}<div>{{ r.clinicName }}</div></td>
            <td>{{ r.doses }}</td>
            <td class="small">
              <span v-for="h in r.herbs" :key="h.id" style="margin-right:4px">
                {{ h.herbName }}{{ h.specialMethod==='NORMAL'?'':('('+h.specialLabel+')') }}
              </span>
            </td>
            <td>{{ r.settlementLabel }}</td>
            <td>{{ r.pickupMethodLabel }}</td>
            <td>
              <span v-if="r.nightUrgent" class="tag red">夜间急煎</span>
              <span v-if="r.addSugar" class="tag blue">加糖</span>
            </td>
            <td>
              <button class="btn sm secondary" @click="preview(r)">校验</button>
              <div v-if="previews[r.id]" class="small mt8" style="max-width:280px">
                <div v-if="!previews[r.id].findings.length" style="color:var(--ok)">✅ 无异常</div>
                <div v-for="(f,i) in previews[r.id].findings" :key="i"
                     :style="{color: previews[r.id].blockingFindings.includes(f)?'var(--danger)':'var(--warn)'}">
                  {{ previews[r.id].blockingFindings.includes(f)?'⛔':'⚠️' }} {{ f }}
                </div>
                <div v-for="(w,i) in previews[r.id].insuranceWarnings" :key="'w'+i" style="color:var(--info)">ℹ️ {{ w }}</div>
              </div>
            </td>
            <td><router-link class="btn sm" :to="`/prescriptions/${r.id}`">去审方</router-link></td>
          </tr>
        </tbody>
      </table>
      <div v-if="!pending.length" class="muted small mt8">暂无待审处方</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api.js'
import { showToast } from '../use.js'

const pending = ref([])
const previews = ref({})

onMounted(async () => {
  const all = await api.get('/prescriptions')
  pending.value = all.filter(r => r.status === 'PENDING_REVIEW')
})

async function preview(r) {
  try {
    previews.value = { ...previews.value, [r.id]: await api.get(`/prescriptions/${r.id}/review-preview`) }
  } catch (e) { showToast(e.message, 'error') }
}
</script>
