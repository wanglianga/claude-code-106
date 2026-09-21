<template>
  <div>
    <div class="page-title">药材库存</div>
    <div class="page-sub">药材字典、库存与医保目录；库存不足时审方自动提示缺药替代</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-if="okMsg" class="alert alert-ok">{{ okMsg }}</div>

    <div class="card">
      <table>
        <thead><tr><th>编码</th><th>名称</th><th>类别</th><th>单价(元/g)</th><th>库存(g)</th><th>最大日剂量</th><th>医保目录</th><th v-if="canEdit">操作</th></tr></thead>
        <tbody>
          <tr v-for="h in herbs" :key="h.id">
            <td class="muted">{{ h.code }}</td>
            <td>{{ h.name }}</td>
            <td class="muted">{{ h.category }}</td>
            <td>¥{{ h.unitPrice }}</td>
            <td :class="{ 'danger-text': h.stockGram < 200 }">{{ h.stockGram }}</td>
            <td>{{ h.maxDailyDoseGram }}g</td>
            <td><span class="badge" :class="h.inInsurance ? 'b-ok' : 'b-warn'">{{ h.inInsurance ? '医保' : '自费' }}</span></td>
            <td v-if="canEdit"><button class="btn btn-sm btn-ghost" @click="restock(h)">补货</button></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api'
import { store } from '../store'

const herbs = ref([])
const error = ref('')
const okMsg = ref('')
const canEdit = computed(() => ['ADMIN', 'PHARMACIST'].includes(store.user?.role))

async function load() {
  try {
    herbs.value = await api.get('/herbs')
  } catch (e) {
    error.value = e.message
  }
}

async function restock(h) {
  const v = prompt(`设置【${h.name}】库存(克)`, h.stockGram)
  if (v === null) return
  try {
    await api.put(`/herbs/${h.id}/stock`, { stockGram: Number(v) })
    okMsg.value = `${h.name} 库存已更新`
    await load()
  } catch (e) { error.value = e.message }
}

onMounted(load)
</script>
