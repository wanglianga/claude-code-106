<template>
  <div>
    <div class="page-title">异常中心</div>
    <div class="page-sub">缺药、改地址、特殊煎法遗漏、包装破损、配送超时、服药不适、补开发票等统一在同一条处方履约记录中处理</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-if="okMsg" class="alert alert-ok">{{ okMsg }}</div>

    <div class="card">
      <div class="flex mb16">
        <select v-model="status" @change="load">
          <option value="">全部状态</option>
          <option v-for="(n, k) in EX_STATUS_NAMES" :key="k" :value="k">{{ n }}</option>
        </select>
      </div>
      <table>
        <thead><tr><th>类型</th><th>处方</th><th>患者/诊所</th><th>描述</th><th>状态</th><th>赔付</th><th>上报</th><th>处理</th></tr></thead>
        <tbody>
          <tr v-for="e in list" :key="e.id">
            <td><span class="badge b-warn">{{ EX_TYPE_NAMES[e.type] }}</span></td>
            <td><router-link :to="'/prescriptions/' + e.prescription.id">{{ e.prescription.rxNo }}</router-link></td>
            <td>{{ e.prescription.patientName }}<span class="muted"> / {{ e.prescription.clinic?.name || '直投' }}</span></td>
            <td>{{ e.description }}</td>
            <td><span class="badge" :class="e.status === 'RESOLVED' ? 'b-ok' : 'b-danger'">{{ EX_STATUS_NAMES[e.status] }}</span></td>
            <td>{{ fmtMoney(e.compensation) }}</td>
            <td class="muted">{{ e.reportedBy?.name }}<br />{{ fmtTime(e.createdAt) }}</td>
            <td>
              <span v-if="e.status === 'RESOLVED'" class="muted">{{ e.resolution }}<br />{{ e.handler?.name }} {{ fmtTime(e.resolvedAt) }}</span>
              <button v-else-if="canResolve" class="btn btn-sm" @click="openResolve(e)">处理</button>
              <span v-else class="muted">待处理</span>
            </td>
          </tr>
          <tr v-if="!list.length"><td colspan="8" class="muted" style="text-align:center">暂无异常</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="target" class="modal-mask" @click.self="target = null">
      <div class="modal">
        <div class="modal-title">处理异常：{{ EX_TYPE_NAMES[target.type] }}</div>
        <div class="muted mb8">{{ target.prescription.rxNo }} · {{ target.description }}</div>
        <div class="form-item mb8"><label>处理结果</label><textarea v-model="form.resolution" placeholder="如：已补发药包 / 已联系患者退费 / 已重新配送"></textarea></div>
        <div class="form-item mb16"><label>药房赔付金额(元)</label><input v-model.number="form.compensation" type="number" min="0" step="0.01" /></div>
        <div class="flex">
          <button class="btn" @click="resolve">确认处理</button>
          <button class="btn btn-ghost" @click="target = null">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../api'
import { store } from '../store'
import { EX_TYPE_NAMES, EX_STATUS_NAMES, fmtMoney, fmtTime } from '../dict'

const list = ref([])
const status = ref('')
const error = ref('')
const okMsg = ref('')
const target = ref(null)
const form = reactive({ resolution: '', compensation: 0 })
const canResolve = computed(() => ['ADMIN', 'PHARMACIST'].includes(store.user?.role))

async function load() {
  try {
    list.value = await api.get('/exceptions', { params: status.value ? { status: status.value } : {} })
  } catch (e) {
    error.value = e.message
  }
}

function openResolve(e) {
  target.value = e
  form.resolution = ''
  form.compensation = 0
}

async function resolve() {
  try {
    await api.post(`/exceptions/${target.value.id}/resolve`, form)
    target.value = null
    okMsg.value = '异常已处理完结'
    await load()
  } catch (e) { error.value = e.message }
}

onMounted(load)
</script>
