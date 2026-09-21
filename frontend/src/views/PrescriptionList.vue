<template>
  <div>
    <div class="flex-between">
      <div>
        <div class="page-title">处方列表</div>
        <div class="page-sub">点击行查看完整履约记录</div>
      </div>
      <router-link v-if="canCreate" to="/prescriptions/new" class="btn">+ 提交处方</router-link>
    </div>

    <div class="card">
      <div class="flex mb16 wrap">
        <select v-model="status" @change="load">
          <option value="">全部状态</option>
          <option v-for="(n, k) in STATUS_NAMES" :key="k" :value="k">{{ n }}</option>
        </select>
        <input v-model="batchNo" placeholder="批次号筛选(诊所批量)" style="width:200px" @keyup.enter="load" />
        <button class="btn btn-ghost btn-sm" @click="load">查询</button>
      </div>
      <div v-if="error" class="alert alert-error">{{ error }}</div>
      <table>
        <thead>
          <tr><th>处方编号</th><th>患者</th><th>诊所/医生</th><th>剂数</th><th>取药方式</th><th>状态</th><th>金额</th><th>提交时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="p in list" :key="p.id" class="clickable" @click="$router.push('/prescriptions/' + p.id)">
            <td>
              {{ p.rxNo }}
              <span v-if="p.urgent" class="badge b-danger">急煎</span>
              <span v-if="p.batchNo" class="badge b-muted">批量</span>
            </td>
            <td>{{ p.patientName }}</td>
            <td>{{ p.clinic?.name || '患者直投' }}<span v-if="p.doctor" class="muted"> / {{ p.doctor.name }}</span></td>
            <td>{{ p.doses }} 剂</td>
            <td>{{ PICKUP_NAMES[p.pickupMethod] }}</td>
            <td><span class="badge" :class="STATUS_CLASS[p.status]">{{ STATUS_NAMES[p.status] }}</span></td>
            <td>{{ fmtMoney(p.totalAmount) }}</td>
            <td class="muted">{{ fmtTime(p.createdAt) }}</td>
          </tr>
          <tr v-if="!list.length"><td colspan="8" class="muted" style="text-align:center">暂无处方</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api'
import { store } from '../store'
import { STATUS_NAMES, STATUS_CLASS, PICKUP_NAMES, fmtMoney, fmtTime } from '../dict'

const list = ref([])
const status = ref('')
const batchNo = ref('')
const error = ref('')
const canCreate = computed(() => ['PATIENT', 'CLINIC', 'ADMIN'].includes(store.user?.role))

async function load() {
  error.value = ''
  try {
    const params = {}
    if (status.value) params.status = status.value
    if (batchNo.value) params.batchNo = batchNo.value
    list.value = await api.get('/prescriptions', { params })
  } catch (e) {
    error.value = e.message
  }
}
onMounted(load)
</script>
