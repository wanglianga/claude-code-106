<template>
  <div>
    <div class="page-title">🧾 财务结算与发票</div>

    <div class="grid grid-3">
      <div class="card"><div class="muted">赔付工单</div><div class="stat">{{ ledger.count ?? 0 }} 笔</div></div>
      <div class="card"><div class="muted">药房赔付总额</div>
        <div class="stat" style="color:var(--danger)">¥{{ ledger.totalCompensation ?? 0 }}</div></div>
      <div class="card"><div class="muted">发票总数</div><div class="stat">{{ invoices.length }}</div></div>
    </div>

    <div class="card">
      <h3>赔付台账(回访不适/漏袋/超时等)</h3>
      <table>
        <thead><tr><th>工单号</th><th>处方</th><th>类型</th><th>金额</th><th>赔付时间</th></tr></thead>
        <tbody>
          <tr v-for="(x,i) in ledger.items||[]" :key="i">
            <td>{{ x.issueNo }}</td><td>{{ x.rxNo }}</td><td>{{ x.type }}</td>
            <td style="color:var(--danger)">¥{{ x.amount }}</td><td class="small">{{ fmt(x.resolvedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <h3>发票(含补开)</h3>
      <table>
        <thead><tr><th>发票号</th><th>处方</th><th>抬头</th><th>金额</th><th>类型</th><th>开具时间</th></tr></thead>
        <tbody>
          <tr v-for="v in invoices" :key="v.id">
            <td>{{ v.invoiceNo }}</td><td>{{ v.rxNo }}</td><td>{{ v.title }}</td><td>¥{{ v.amount }}</td>
            <td><span class="tag" :class="v.kind==='REISSUED'?'blue':'gray'">{{ v.kind==='REISSUED'?'补开':'正常' }}</span></td>
            <td class="small">{{ fmt(v.issuedAt) }}</td>
          </tr>
        </tbody>
      </table>
      <p class="small muted mt8">诊所/患者在处方详情页可发起"补开发票"申请,财务在异常工单中心受理后自动补开并闭环工单。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api.js'
import { fmt } from '../use.js'

const ledger = ref({})
const invoices = ref([])
onMounted(async () => {
  ledger.value = await api.get('/finance/compensation').catch(() => ({}))
  invoices.value = await api.get('/invoices').catch(() => [])
})
</script>
