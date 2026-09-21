<template>
  <div>
    <div class="page-title">回访管理</div>
    <div class="page-sub">回访结果影响后续用药提醒、药房赔付与诊所合作评分</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-if="okMsg" class="alert alert-ok">{{ okMsg }}</div>

    <div class="card">
      <div class="card-title">待回访(已签收 {{ pending.length }})</div>
      <table>
        <thead><tr><th>处方编号</th><th>患者</th><th>电话</th><th>诊所</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="p in pending" :key="p.id">
            <td><router-link :to="'/prescriptions/' + p.id">{{ p.rxNo }}</router-link></td>
            <td>{{ p.patientName }}</td>
            <td>{{ p.patientPhone }}</td>
            <td>{{ p.clinic?.name || '患者直投' }}</td>
            <td><button class="btn btn-sm" @click="openFollow(p)">回访登记</button></td>
          </tr>
          <tr v-if="!pending.length"><td colspan="5" class="muted" style="text-align:center">暂无待回访处方</td></tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <div class="card-title">回访记录</div>
      <table>
        <thead><tr><th>处方</th><th>患者</th><th>结果</th><th>满意度</th><th>说明</th><th>回访人</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-for="f in history" :key="f.id">
            <td>{{ f.prescription.rxNo }}</td>
            <td>{{ f.prescription.patientName }}</td>
            <td><span class="badge" :class="f.result === 'DISCOMFORT' ? 'b-danger' : f.result === 'UNREACHABLE' ? 'b-muted' : 'b-ok'">{{ FOLLOWUP_NAMES[f.result] }}</span></td>
            <td>{{ f.satisfaction ? '★'.repeat(f.satisfaction) : '—' }}</td>
            <td>{{ f.discomfortDesc || f.note || '—' }}</td>
            <td>{{ f.createdBy?.name }}</td>
            <td class="muted">{{ fmtTime(f.createdAt) }}</td>
          </tr>
          <tr v-if="!history.length"><td colspan="7" class="muted" style="text-align:center">暂无回访记录</td></tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <div class="card-title">用药提醒</div>
      <table>
        <thead><tr><th>处方</th><th>内容</th><th>提醒时间</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="r in reminders" :key="r.id">
            <td>{{ r.prescription.rxNo }}</td>
            <td>{{ r.message }}</td>
            <td class="muted">{{ fmtTime(r.remindAt) }}</td>
            <td><span class="badge" :class="r.status === 'SENT' ? 'b-ok' : 'b-info'">{{ REMINDER_STATUS_NAMES[r.status] }}</span></td>
          </tr>
          <tr v-if="!reminders.length"><td colspan="4" class="muted" style="text-align:center">暂无提醒</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="target" class="modal-mask" @click.self="target = null">
      <div class="modal">
        <div class="modal-title">回访登记：{{ target.patientName }}({{ target.rxNo }})</div>
        <div class="form-item mb8"><label>回访结果</label>
          <select v-model="form.result">
            <option v-for="(n, k) in FOLLOWUP_NAMES" :key="k" :value="k">{{ n }}</option>
          </select>
        </div>
        <div v-if="form.result === 'DISCOMFORT'" class="form-item mb8">
          <label>不适情况(将自动登记异常并进入赔付流程)</label>
          <textarea v-model="form.discomfortDesc"></textarea>
        </div>
        <div class="form-row">
          <div class="form-item"><label>满意度(1-5，影响诊所合作评分)</label>
            <select v-model.number="form.satisfaction">
              <option v-for="n in 5" :key="n" :value="n">{{ n }} 分</option>
            </select>
          </div>
          <div class="form-item" style="justify-content:flex-end">
            <label class="checkbox-line"><input type="checkbox" v-model="form.needReminder" /> 生成后续3天用药提醒</label>
          </div>
        </div>
        <div class="form-item mb16"><label>备注</label><input v-model="form.note" /></div>
        <div class="flex">
          <button class="btn" @click="submit">提交回访</button>
          <button class="btn btn-ghost" @click="target = null">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../api'
import { FOLLOWUP_NAMES, REMINDER_STATUS_NAMES, fmtTime } from '../dict'

const pending = ref([])
const history = ref([])
const reminders = ref([])
const error = ref('')
const okMsg = ref('')
const target = ref(null)
const form = reactive({ result: 'NORMAL', discomfortDesc: '', satisfaction: 5, needReminder: false, note: '' })

async function load() {
  try {
    ;[pending.value, history.value, reminders.value] = await Promise.all([
      api.get('/followups/pending'),
      api.get('/followups'),
      api.get('/reminders')
    ])
  } catch (e) {
    error.value = e.message
  }
}

function openFollow(p) {
  target.value = p
  form.result = 'NORMAL'
  form.discomfortDesc = ''
  form.satisfaction = 5
  form.needReminder = false
  form.note = ''
}

async function submit() {
  try {
    await api.post('/followups', { prescriptionId: target.value.id, ...form })
    target.value = null
    okMsg.value = '回访已登记'
    await load()
  } catch (e) { error.value = e.message }
}

onMounted(load)
</script>
