<template>
  <div>
    <div class="page-title">🛵 配送调度看板</div>
    <div class="card">
      <table>
        <thead>
          <tr><th>处方号</th><th>患者</th><th>取药方式</th><th>波次</th><th>配送员</th>
            <th>地址</th><th>承诺送达</th><th>状态</th><th>签收</th><th>超时</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="t in tasks" :key="t.rxId">
            <td><b>{{ t.rxNo }}</b></td>
            <td>{{ t.patientName }}<div v-if="t.proxyName" class="small muted">代取:{{ t.proxyName }}</div></td>
            <td class="small">{{ {DELIVERY:'配送到家',SELF_PICKUP:'自取',ELDER_PROXY:'老人代取'}[t.pickupMethod] }}</td>
            <td>{{ t.wave }}</td>
            <td>{{ t.courier || '未指派' }}</td>
            <td class="small">{{ t.address }}</td>
            <td class="small">{{ fmt(t.promisedAt) }}</td>
            <td><span class="tag" :class="t.status==='SIGNED'?'green':'orange'">{{ t.statusLabel }}</span></td>
            <td class="small">{{ t.signedBy || '—' }}<div class="muted">{{ fmt(t.signedAt) }}</div></td>
            <td><span v-if="t.timeoutMinutes" class="tag red">超时 {{ t.timeoutMinutes }} 分</span>
                <span v-else class="tag green">准时</span></td>
            <td><router-link class="btn sm secondary" :to="`/prescriptions/${t.rxId}`">处理</router-link></td>
          </tr>
        </tbody>
      </table>
      <div v-if="!tasks.length" class="muted small mt8">暂无配送任务(煎药完成后派单)</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api.js'
import { fmt } from '../use.js'

const tasks = ref([])
onMounted(async () => {
  tasks.value = await api.get('/delivery/tasks')
})
</script>
