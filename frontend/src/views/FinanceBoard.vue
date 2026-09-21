<template>
  <div>
    <div class="page-title">财务发票</div>
    <div class="page-sub">医保/自费结算、发票开具与补开、赔付汇总</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-if="okMsg" class="alert alert-ok">{{ okMsg }}</div>

    <div v-if="summary" class="grid grid-4 mb16">
      <div class="stat-card"><div class="stat-num">{{ summary.invoiceCount }}</div><div class="stat-label">发票张数</div></div>
      <div class="stat-card"><div class="stat-num">{{ fmtMoney(summary.invoicedAmount) }}</div><div class="stat-label">开票总额</div></div>
      <div class="stat-card"><div class="stat-num">{{ fmtMoney(summary.insuranceAmount) }}</div><div class="stat-label">医保承担</div></div>
      <div class="stat-card"><div class="stat-num">{{ fmtMoney(summary.compensationAmount) }}</div><div class="stat-label">累计赔付</div></div>
    </div>

    <div class="card">
      <div class="card-title">发票列表</div>
      <table>
        <thead><tr><th>发票号</th><th>处方</th><th>患者/诊所</th><th>金额</th><th>类型</th><th>状态</th><th>开具时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="i in invoices" :key="i.id">
            <td>{{ i.invoiceNo }}</td>
            <td><router-link :to="'/prescriptions/' + i.prescription.id">{{ i.prescription.rxNo }}</router-link></td>
            <td>{{ i.prescription.patientName }}<span class="muted"> / {{ i.prescription.clinic?.name || '直投' }}</span></td>
            <td>{{ fmtMoney(i.amount) }}</td>
            <td>{{ SETTLE_NAMES[i.type] }}</td>
            <td><span class="badge" :class="i.status === 'ISSUED' ? 'b-ok' : i.status === 'REISSUED' ? 'b-info' : 'b-warn'">{{ INVOICE_STATUS_NAMES[i.status] }}</span></td>
            <td class="muted">{{ fmtTime(i.issuedAt) }}</td>
            <td>
              <button v-if="canReissue && i.status !== 'REISSUED'" class="btn btn-sm" @click="reissue(i)">补开发票</button>
              <span v-else-if="i.status === 'REISSUED'" class="muted">{{ i.reissueReason }}</span>
            </td>
          </tr>
          <tr v-if="!invoices.length"><td colspan="8" class="muted" style="text-align:center">暂无发票</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api'
import { store } from '../store'
import { SETTLE_NAMES, INVOICE_STATUS_NAMES, fmtMoney, fmtTime } from '../dict'

const invoices = ref([])
const summary = ref(null)
const error = ref('')
const okMsg = ref('')
const canReissue = computed(() => ['FINANCE', 'ADMIN'].includes(store.user?.role))

async function load() {
  try {
    invoices.value = await api.get('/invoices')
    if (['FINANCE', 'ADMIN'].includes(store.user?.role)) {
      summary.value = await api.get('/finance/summary')
    }
  } catch (e) {
    error.value = e.message
  }
}

async function reissue(i) {
  const reason = prompt('补开原因', '诊所要求补开发票')
  if (!reason) return
  try {
    await api.post(`/invoices/${i.id}/reissue`, { reason })
    okMsg.value = `发票 ${i.invoiceNo} 已补开`
    await load()
  } catch (e) { error.value = e.message }
}

onMounted(load)
</script>
