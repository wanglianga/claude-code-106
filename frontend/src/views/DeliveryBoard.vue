<template>
  <div>
    <div class="page-title">配送台</div>
    <div class="page-sub">按配送波次派送，签收闭环；超过时限自动登记配送超时异常</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-if="okMsg" class="alert alert-ok">{{ okMsg }}</div>

    <div class="card">
      <div class="card-title">待配送池({{ pending.length }})</div>
      <table>
        <thead><tr><th>处方编号</th><th>波次</th><th>收件人</th><th>地址</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="d in pending" :key="d.id">
            <td>{{ d.prescription.rxNo }} <span v-if="d.prescription.urgent" class="badge b-danger">急</span></td>
            <td>{{ d.waveNo }}</td>
            <td>{{ d.receiverName }} {{ d.receiverPhone }}</td>
            <td>{{ d.address }}</td>
            <td>
              <button class="btn btn-sm" @click="dispatch(d)">认领派送</button>
              <select v-if="isAdmin" v-model="courierPick[d.id]" style="margin-left:6px">
                <option v-for="c in couriers" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
              <button v-if="isAdmin" class="btn btn-sm btn-ghost" @click="dispatch(d, courierPick[d.id])">指派</button>
            </td>
          </tr>
          <tr v-if="!pending.length"><td colspan="5" class="muted" style="text-align:center">暂无待配送订单</td></tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <div class="card-title">{{ store.user.role === 'COURIER' ? '我的配送单' : '全部配送单' }}</div>
      <table>
        <thead><tr><th>处方编号</th><th>波次</th><th>配送员</th><th>收件人/地址</th><th>状态</th><th>派送时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="d in mine" :key="d.id">
            <td><router-link :to="'/prescriptions/' + d.prescription.id">{{ d.prescription.rxNo }}</router-link></td>
            <td>{{ d.waveNo }}</td>
            <td>{{ d.courier?.name || '—' }}</td>
            <td>{{ d.receiverName }} · {{ d.address }}</td>
            <td>
              <span class="badge" :class="d.status === 'SIGNED' ? 'b-ok' : d.status === 'DELIVERING' ? 'b-info' : 'b-warn'">{{ DELIVERY_STATUS_NAMES[d.status] }}</span>
              <span v-if="d.timeout" class="badge b-danger">超时</span>
            </td>
            <td class="muted">{{ fmtTime(d.dispatchedAt) }}</td>
            <td>
              <button v-if="d.status === 'DELIVERING'" class="btn btn-sm" @click="sign(d)">签收</button>
              <span v-else-if="d.status === 'SIGNED'" class="muted">{{ d.signedBy }} {{ fmtTime(d.signedAt) }}</span>
            </td>
          </tr>
          <tr v-if="!mine.length"><td colspan="7" class="muted" style="text-align:center">暂无配送单</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../api'
import { store } from '../store'
import { DELIVERY_STATUS_NAMES, fmtTime } from '../dict'

const pending = ref([])
const mine = ref([])
const couriers = ref([])
const courierPick = reactive({})
const error = ref('')
const okMsg = ref('')
const isAdmin = computed(() => store.user?.role === 'ADMIN')

async function load() {
  try {
    ;[pending.value, mine.value] = await Promise.all([
      api.get('/deliveries/pending'),
      api.get('/deliveries')
    ])
  } catch (e) {
    error.value = e.message
  }
}

async function dispatch(d, courierId) {
  error.value = ''
  try {
    await api.post(`/deliveries/${d.id}/dispatch`, courierId ? { courierId } : {})
    okMsg.value = `配送单 ${d.prescription.rxNo} 已派送`
    await load()
  } catch (e) { error.value = e.message }
}

async function sign(d) {
  const signedBy = prompt('签收人姓名', d.receiverName)
  if (!signedBy) return
  try {
    await api.post(`/deliveries/${d.id}/sign`, { signedBy })
    okMsg.value = '签收完成'
    await load()
  } catch (e) { error.value = e.message }
}

onMounted(async () => {
  await load()
  if (isAdmin.value) {
    couriers.value = await api.get('/meta/couriers')
    couriers.value.forEach(c => { if (!courierPick[c.id]) courierPick[c.id] = c.id })
  }
})
</script>
