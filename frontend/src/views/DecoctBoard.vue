<template>
  <div>
    <div class="page-title">煎药台</div>
    <div class="page-sub">按锅号、浸泡时间、煎煮次数、包装袋数与配送波次作业；急煎优先；先扫描药包再煎煮</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-if="okMsg" class="alert alert-ok">{{ okMsg }}</div>

    <div class="grid grid-2">
      <div v-for="row in queue" :key="row.task.id" class="card">
        <div class="flex-between mb8">
          <div>
            <b>{{ row.task.prescription.rxNo }}</b>
            <span v-if="row.task.urgent" class="badge b-danger">夜间急煎</span>
            <span class="badge" :class="row.task.status === 'QUEUED' ? 'b-warn' : 'b-info'">
              {{ row.task.status === 'QUEUED' ? '排队 #' + row.task.queuePosition : '煎药中' }}
            </span>
          </div>
          <div class="muted">锅号 <b style="font-size:16px">{{ row.task.potNo }}</b></div>
        </div>
        <table class="mb8"><tbody>
          <tr><th>患者/剂数</th><td>{{ row.task.prescription.patientName }} · {{ row.task.prescription.doses }}剂 · {{ row.task.bagCount }}袋</td></tr>
          <tr><th>浸泡/煎煮</th><td>浸泡 {{ row.task.soakMinutes }} 分钟 · 煎煮 {{ row.task.decoctTimes }} 次 · {{ row.task.waveNo }}</td></tr>
          <tr><th>特殊煎法</th><td>
            <span v-for="it in specialItems(row.items)" :key="it.id" class="badge b-info" style="margin-right:4px">
              {{ it.herb.name }}·{{ SPECIAL_NAMES[it.specialHandling] }}
            </span>
            <span v-if="!specialItems(row.items).length" class="muted">无</span>
            <div v-if="row.task.prescription.specialDecoction" class="warn-text">{{ row.task.prescription.specialDecoction }}</div>
          </td></tr>
          <tr v-if="row.record?.scanCode"><th>已扫描</th><td>{{ row.record.scanCode }} {{ fmtTime(row.record.scanTime) }}</td></tr>
          <tr v-if="row.record?.startTime"><th>开始</th><td>{{ fmtTime(row.record.startTime) }}</td></tr>
        </tbody></table>

        <div class="flex wrap">
          <template v-if="row.task.status === 'QUEUED'">
            <input v-model="scanCodes[row.task.id]" placeholder="扫描/输入药包码" style="width:170px" />
            <button class="btn btn-sm" @click="scan(row.task.id)">扫描药包</button>
            <button class="btn btn-sm btn-warn" :disabled="!row.record?.scanCode" @click="start(row.task.id)">开始煎煮</button>
          </template>
          <button v-else class="btn btn-sm" @click="openFinish(row)">结束煎煮</button>
        </div>
      </div>
      <div v-if="!queue.length" class="card muted" style="text-align:center">煎药队列为空</div>
    </div>

    <div v-if="finishTarget" class="modal-mask" @click.self="finishTarget = null">
      <div class="modal">
        <div class="modal-title">结束煎煮：{{ finishTarget.task.prescription.rxNo }}(锅号 {{ finishTarget.task.potNo }})</div>
        <div class="form-item mb8">
          <label class="checkbox-line"><input type="checkbox" v-model="finishForm.abnormalSmell" /> 发现异常气味</label>
          <input v-if="finishForm.abnormalSmell" v-model="finishForm.smellNote" placeholder="气味说明(如焦糊味)" />
        </div>
        <div class="form-row">
          <div class="form-item"><label>漏袋数</label><input v-model.number="finishForm.leakedBags" type="number" min="0" /></div>
          <div class="form-item"><label>复核人 *</label><input v-model="finishForm.reviewerName" placeholder="复核人姓名" /></div>
        </div>
        <div v-if="hasSpecial" class="form-item mb8">
          <label class="checkbox-line warn-text">
            <input type="checkbox" v-model="finishForm.specialHandledConfirmed" />
            已按处方执行先煎/后下等特殊煎法(未确认将自动登记"特殊煎法遗漏"异常)
          </label>
        </div>
        <div class="form-item mb16"><label>备注</label><input v-model="finishForm.note" /></div>
        <div class="flex">
          <button class="btn" @click="finish">确认完成</button>
          <button class="btn btn-ghost" @click="finishTarget = null">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../api'
import { SPECIAL_NAMES, fmtTime } from '../dict'

const queue = ref([])
const scanCodes = reactive({})
const error = ref('')
const okMsg = ref('')
const finishTarget = ref(null)
const finishForm = reactive({
  abnormalSmell: false, smellNote: '', leakedBags: 0,
  reviewerName: '', specialHandledConfirmed: false, note: ''
})

const hasSpecial = computed(() =>
  finishTarget.value ? specialItems(finishTarget.value.items).length > 0 : false)

function specialItems(items) {
  return (items || []).filter(i => i.specialHandling !== 'NONE')
}

async function load() {
  try {
    queue.value = await api.get('/decoct/queue')
  } catch (e) {
    error.value = e.message
  }
}

async function scan(taskId) {
  error.value = ''
  try {
    await api.post(`/decoct/tasks/${taskId}/scan`, null, { params: { code: scanCodes[taskId] || '' } })
    okMsg.value = '扫描成功'
    await load()
  } catch (e) { error.value = e.message }
}

async function start(taskId) {
  try {
    await api.post(`/decoct/tasks/${taskId}/start`)
    okMsg.value = '已开始煎煮'
    await load()
  } catch (e) { error.value = e.message }
}

function openFinish(row) {
  finishTarget.value = row
  finishForm.abnormalSmell = false
  finishForm.smellNote = ''
  finishForm.leakedBags = 0
  finishForm.reviewerName = ''
  finishForm.specialHandledConfirmed = false
  finishForm.note = ''
}

async function finish() {
  try {
    await api.post(`/decoct/tasks/${finishTarget.value.task.id}/finish`, finishForm)
    finishTarget.value = null
    okMsg.value = '煎煮完成'
    await load()
  } catch (e) { error.value = e.message }
}

onMounted(load)
</script>
