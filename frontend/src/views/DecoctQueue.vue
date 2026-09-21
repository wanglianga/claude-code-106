<template>
  <div>
    <div class="page-title">🍵 煎药作业看板 <span class="muted small">(夜间急煎自动置顶)</span></div>
    <div class="card">
      <table>
        <thead>
          <tr><th>处方号</th><th>患者</th><th>剂数</th><th>状态</th><th>锅号</th>
            <th>浸泡/次数/袋数</th><th>波次</th><th>扫码</th><th>漏/破</th><th>包装/复核</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="t in tasks" :key="t.rxId">
            <td><b>{{ t.rxNo }}</b><span v-if="t.nightUrgent" class="tag red" style="margin-left:4px">急</span></td>
            <td>{{ t.patientName }}</td>
            <td>{{ t.doses }}</td>
            <td><span class="tag blue">{{ t.statusLabel }}</span></td>
            <td>{{ t.potNo || '—' }}</td>
            <td class="small">{{ t.soakMinutes ?? '—' }}分 / {{ t.boilTimes ?? '—' }}次 / {{ t.bagCount ?? '—' }}袋</td>
            <td>{{ t.deliveryWave || '—' }}</td>
            <td class="small">{{ t.scannedBagCode ? '✓' : '未扫' }}</td>
            <td><span :style="{color:(t.missingBags+t.damagedBags)?'var(--danger)':'inherit'}">
              {{ t.missingBags }}/{{ t.damagedBags }}</span></td>
            <td class="small">{{ t.packer || '—' }}/{{ t.reviewer || '—' }}</td>
            <td><router-link class="btn sm secondary" :to="`/prescriptions/${t.rxId}`">作业</router-link></td>
          </tr>
        </tbody>
      </table>
      <div v-if="!tasks.length" class="muted small mt8">暂无代煎任务(抓药后自动入队)</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api.js'

const tasks = ref([])
onMounted(async () => {
  tasks.value = await api.get('/decoct/queue')
})
</script>
