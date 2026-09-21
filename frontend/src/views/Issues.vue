<template>
  <div>
    <div class="page-title">⚠️ 异常工单中心
      <span class="muted small">缺药 / 改地址 / 特殊煎法遗漏 / 包装袋破损 / 配送超时 / 服药不适 / 补开发票</span>
    </div>
    <div class="card">
      <div class="flex" style="margin-bottom:10px;gap:10px;flex-wrap:wrap">
        <select v-model="filterType" style="max-width:180px">
          <option value="">全部类型</option>
          <option v-for="t in types" :key="t.v" :value="t.v">{{ t.l }}</option>
        </select>
        <select v-model="filterStatus" style="max-width:160px">
          <option value="">全部状态</option>
          <option value="OPEN">待处理</option>
          <option value="PROCESSING">处理中</option>
          <option value="RESOLVED">已解决</option>
        </select>
      </div>
      <table>
        <thead>
          <tr><th>工单号</th><th>类型</th><th>处方/患者</th><th>描述</th><th>定位(锅号/包装人/配送)</th>
            <th>主办环节</th><th>状态</th><th>赔付</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="i in filtered" :key="i.id">
            <td class="small">{{ i.issueNo }}</td>
            <td><span class="tag orange">{{ i.typeLabel }}</span></td>
            <td class="small"><router-link :to="`/prescriptions/${i.rxId}`">{{ i.rxNo }}</router-link>
              <div>{{ i.patientName }}</div></td>
            <td class="small" style="max-width:260px">{{ i.description }}
              <div v-if="i.resolution" class="muted">处理:{{ i.resolution }}</div></td>
            <td class="small muted" style="max-width:240px">{{ i.locateInfo }}</td>
            <td><span class="tag gray">{{ i.ownerStageLabel }}</span></td>
            <td><span class="tag" :class="i.status==='RESOLVED'?'green':'red'">{{ i.statusLabel }}</span></td>
            <td><span v-if="i.compensation>0" style="color:var(--danger)">¥{{ i.compensation }}</span>
                <span v-else class="muted">—</span></td>
            <td class="pill-btns">
              <router-link class="btn sm secondary" :to="`/prescriptions/${i.rxId}`">履约</router-link>
              <button v-if="i.status==='OPEN'" class="btn sm" @click="process(i)">受理</button>
              <button v-if="i.status!=='RESOLVED'" class="btn sm" @click="resolve(i)">解决/赔付</button>
              <button v-if="i.type==='INVOICE' && i.status!=='RESOLVED'" class="btn sm" @click="reissue(i)">补开发票</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { api } from '../api.js'
import { showToast, fmt } from '../use.js'

const list = ref([])
const filterType = ref('')
const filterStatus = ref('')
const types = [
  { v: 'SHORTAGE', l: '缺药' }, { v: 'ADDRESS_CHANGE', l: '临时改地址' },
  { v: 'SPECIAL_MISSED', l: '特殊煎法遗漏' }, { v: 'BAG_DAMAGED', l: '包装袋破损/漏袋' },
  { v: 'DELIVERY_TIMEOUT', l: '配送超时' }, { v: 'DISCOMFORT', l: '服药后不适' },
  { v: 'INVOICE', l: '补开发票' }
]

const filtered = computed(() => list.value.filter(i =>
  (!filterType.value || i.type === filterType.value) &&
  (!filterStatus.value || i.status === filterStatus.value)))

async function load() { list.value = await api.get('/issues') }
onMounted(load)

async function process(i) {
  await api.post(`/issues/${i.id}/processing`); showToast('已受理', 'ok'); load()
}
async function resolve(i) {
  const resolution = prompt('处理结果/整改措施', i.type === 'DISCOMFORT' ? '医生研判后建议停药复诊' : '已处理')
  if (resolution === null) return
  let compensation = null
  if (['BAG_DAMAGED', 'DELIVERY_TIMEOUT', 'DISCOMFORT'].includes(i.type)) {
    compensation = Number(prompt('药房赔付金额(元,无赔付填0)', '0') || 0)
  }
  await api.post(`/issues/${i.id}/resolve`, { resolution, compensation })
  showToast('工单已闭环', 'ok'); load()
}
async function reissue(i) {
  const title = prompt('发票抬头', i.patientName)
  if (title === null) return
  await api.post(`/issues/${i.id}/reissue`, { title })
  showToast('发票已补开,工单自动闭环', 'ok'); load()
}
</script>
