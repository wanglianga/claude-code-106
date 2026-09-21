<template>
  <div>
    <div class="page-title">审方台</div>
    <div class="page-sub">核对十八反十九畏、剂量异常、缺药替代与医保/自费结算；审方结论影响抓药、收费与代煎排队</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-if="okMsg" class="alert alert-ok">{{ okMsg }}</div>

    <div class="grid" style="grid-template-columns: 340px 1fr">
      <div class="card">
        <div class="card-title">待审方({{ pending.length }})</div>
        <table>
          <thead><tr><th>编号</th><th>患者</th><th>剂数</th><th></th></tr></thead>
          <tbody>
            <tr v-for="p in pending" :key="p.id" class="clickable" @click="select(p)"
                :style="{ background: current?.id === p.id ? 'var(--green-light)' : '' }">
              <td>{{ p.rxNo }} <span v-if="p.urgent" class="badge b-danger">急</span></td>
              <td>{{ p.patientName }}</td>
              <td>{{ p.doses }}剂</td>
              <td><span class="badge b-warn">待审</span></td>
            </tr>
            <tr v-if="!pending.length"><td colspan="4" class="muted" style="text-align:center">暂无待审处方</td></tr>
          </tbody>
        </table>
      </div>

      <div v-if="current" class="card">
        <div class="card-title">审方：{{ current.rxNo }} · {{ current.patientName }}</div>
        <div class="muted mb8">
          {{ current.clinic?.name || '患者直投' }}<span v-if="current.doctor"> / {{ current.doctor.name }}</span>
          · {{ current.doses }}剂 · {{ PICKUP_NAMES[current.pickupMethod] }}
          <span v-if="current.addSugar"> · 加糖</span>
          <span v-if="current.contraindications"> · 禁忌：{{ current.contraindications }}</span>
        </div>
        <div v-if="current.specialDecoction" class="alert alert-warn">特殊煎法：{{ current.specialDecoction }}</div>

        <table class="mb16">
          <thead><tr><th>药材</th><th>剂量</th><th>特殊煎法</th><th>库存(需用量)</th><th>医保</th><th>缺药替代</th></tr></thead>
          <tbody>
            <tr v-for="it in items" :key="it.id">
              <td>{{ it.herb.name }}</td>
              <td :class="{ 'warn-text': it.dosageG > it.herb.maxDailyDoseGram }">{{ it.dosageG }}g</td>
              <td><span v-if="it.specialHandling !== 'NONE'" class="badge b-info">{{ SPECIAL_NAMES[it.specialHandling] }}</span><span v-else class="muted">无</span></td>
              <td :class="{ 'danger-text': it.dosageG * current.doses > it.herb.stockGram }">{{ it.herb.stockGram }}g (需{{ it.dosageG * current.doses }}g)</td>
              <td><span class="badge" :class="it.herb.inInsurance ? 'b-ok' : 'b-warn'">{{ it.herb.inInsurance ? '医保' : '自费' }}</span></td>
              <td>
                <label class="checkbox-line">
                  <input type="checkbox" v-model="subs[it.id].enabled" />
                  <input v-model="subs[it.id].note" placeholder="替代说明" style="width:150px" :disabled="!subs[it.id].enabled" />
                </label>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="mb16">
          <div v-if="check.conflicts?.length" class="alert alert-error"><b>配伍禁忌：</b>{{ check.conflicts.join('；') }}</div>
          <div v-if="check.doseIssues?.length" class="alert alert-warn"><b>剂量异常：</b>{{ check.doseIssues.join('；') }}</div>
          <div v-if="check.shortages?.length" class="alert alert-warn"><b>缺药：</b>{{ check.shortages.join('；') }}</div>
          <div v-if="check.notInCatalog?.length" class="alert alert-warn"><b>医保目录限制：</b>{{ check.notInCatalog.join('；') }}</div>
          <div v-if="!check.conflicts?.length && !check.doseIssues?.length && !check.shortages?.length" class="alert alert-ok">自动核对：配伍禁忌、剂量、库存均未见异常</div>
        </div>

        <div class="form-row">
          <div class="form-item"><label>审方结论</label>
            <select v-model="form.conclusion">
              <option value="PASS">通过</option>
              <option value="ADJUSTED_PASS">调整后通过(缺药替代等)</option>
              <option value="REJECTED">驳回</option>
            </select>
          </div>
          <div class="form-item"><label>结算方式</label>
            <select v-model="form.settlementType">
              <option value="MEDICAL_INSURANCE">医保结算</option>
              <option value="SELF_PAY">自费结算</option>
            </select>
          </div>
          <div class="form-item"><label>审方备注</label><input v-model="form.note" /></div>
        </div>
        <button class="btn" :disabled="saving" @click="submit">提交审方结论</button>
      </div>
      <div v-else class="card muted" style="display:flex;align-items:center;justify-content:center">从左侧选择待审处方</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../api'
import { PICKUP_NAMES, SPECIAL_NAMES } from '../dict'

const pending = ref([])
const current = ref(null)
const items = ref([])
const check = ref({})
const subs = reactive({})
const form = reactive({ conclusion: 'PASS', settlementType: 'SELF_PAY', note: '' })
const error = ref('')
const okMsg = ref('')
const saving = ref(false)

async function loadPending() {
  pending.value = await api.get('/prescriptions', { params: { status: 'PENDING_REVIEW' } })
}

async function select(p) {
  error.value = ''
  current.value = p
  const detail = await api.get('/prescriptions/' + p.id)
  items.value = detail.items
  check.value = await api.get(`/prescriptions/${p.id}/precheck`)
  Object.keys(subs).forEach(k => delete subs[k])
  items.value.forEach(it => { subs[it.id] = { enabled: false, note: '' } })
}

async function submit() {
  saving.value = true
  error.value = ''
  try {
    const substitutions = items.value
      .filter(it => subs[it.id]?.enabled && subs[it.id]?.note)
      .map(it => ({ itemId: it.id, note: subs[it.id].note }))
    await api.post(`/prescriptions/${current.value.id}/review`, { ...form, substitutions })
    okMsg.value = `处方 ${current.value.rxNo} 审方完成`
    current.value = null
    await loadPending()
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    await loadPending()
  } catch (e) {
    error.value = e.message
  }
})
</script>
