<template>
  <div>
    <div class="page-title">⏰ 后续用药提醒</div>
    <div class="card">
      <p class="small muted">签收后自动建立用药提醒;回访发现"服药后不适"自动暂停,影响后续提醒、药房赔付与诊所合作评分。</p>
      <table>
        <thead><tr><th>处方号</th><th>患者</th><th>电话</th><th>每日次数</th>
          <th>下次提醒</th><th>状态</th><th>备注</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td>{{ r.rxNo }}</td>
            <td>{{ r.patientName }}</td>
            <td>{{ r.patientPhone }}</td>
            <td>{{ r.timesPerDay }} 次/日</td>
            <td>{{ fmt(r.nextRemindAt) }}</td>
            <td><span class="tag" :class="r.state==='ACTIVE'?'green':'orange'">
              {{ {ACTIVE:'正常',PAUSED:'不适暂停',DONE:'疗程结束'}[r.state] }}</span></td>
            <td class="small muted">{{ r.note }}</td>
            <td>
              <button class="btn sm" :disabled="r.state==='ACTIVE'" @click="setState(r,'ACTIVE')">恢复</button>
              <button class="btn sm warn" :disabled="r.state==='PAUSED'" @click="setState(r,'PAUSED')">暂停</button>
              <button class="btn sm secondary" :disabled="r.state==='DONE'" @click="setState(r,'DONE')">结束疗程</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!list.length" class="muted small mt8">暂无提醒(处方签收后自动生成)</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api.js'
import { fmt, showToast } from '../use.js'

const list = ref([])
onMounted(async () => { list.value = await api.get('/reminders') })

async function setState(r, state) {
  const note = prompt('调整说明', state === 'PAUSED' ? '患者反馈不适,暂停提醒' : '')
  await api.patch(`/reminders/${r.id}`, { state, note: note ?? r.note })
  showToast('提醒状态已更新', 'ok')
  list.value = await api.get('/reminders')
}
</script>
