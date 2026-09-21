<template>
  <div>
    <div class="page-title">✍️ {{ batch ? '诊所批量开方' : '提交中药处方' }}</div>

    <div v-for="(f, fi) in forms" :key="fi" class="card">
      <div class="flex-between">
        <h3 v-if="batch">处方 {{ fi + 1 }} · 患者 {{ f.patientName || '未填' }}</h3>
        <h3 v-else>处方信息</h3>
        <button v-if="batch && forms.length > 1" class="btn sm danger" @click="forms.splice(fi,1)">移除</button>
      </div>
      <div class="form-row">
        <div v-if="isClinic">
          <label>患者姓名 *</label>
          <input v-model="f.patientName" placeholder="患者姓名" />
        </div>
        <div v-if="isClinic">
          <label>患者电话</label>
          <input v-model="f.patientPhone" />
        </div>
        <div>
          <label>开方医生</label>
          <select v-model="f.doctorId">
            <option value="">(手工填写)</option>
            <option v-for="d in meta.doctors" :key="d.id" :value="d.id">
              {{ d.name }} {{ d.title }} · {{ d.clinicName }}{{ d.insuranceQualified ? '' : '(无医保资质)' }}
            </option>
          </select>
        </div>
        <div v-if="!f.doctorId">
          <label>医生姓名</label>
          <input v-model="f.doctorName" />
        </div>
        <div>
          <label>剂数 *</label>
          <input type="number" min="1" v-model.number="f.doses" />
        </div>
      </div>

      <div class="form-row">
        <div>
          <label>取药方式 *</label>
          <select v-model="f.pickupMethod">
            <option v-for="e in meta.enums?.pickupMethod||[]" :key="e.value" :value="e.value">{{ e.label }}</option>
          </select>
        </div>
        <div>
          <label>结算方式 *</label>
          <select v-model="f.settlementType">
            <option v-for="e in meta.enums?.settlementType||[]" :key="e.value" :value="e.value">{{ e.label }}</option>
          </select>
        </div>
        <div v-if="f.pickupMethod !== 'SELF_PICKUP'">
          <label>配送地址 *</label>
          <input v-model="f.address" placeholder="详细地址" />
        </div>
        <div v-if="f.pickupMethod === 'ELDER_PROXY'">
          <label>老人代取联系电话</label>
          <input v-model="f.contactPhone" />
        </div>
      </div>

      <div class="form-row">
        <div style="flex:2">
          <label>患者禁忌(过敏/妊娠/慢病)</label>
          <input v-model="f.contraindications" placeholder="如:糖尿病;对鱼腥草过敏" />
        </div>
        <div style="flex:2">
          <label>特殊煎法整体说明</label>
          <input v-model="f.specialDecoctionNote" placeholder="如:石膏先煎20分钟,砂仁后下" />
        </div>
      </div>

      <div class="flex" style="margin-top:8px;gap:24px">
        <label class="checkbox-line"><input type="checkbox" v-model="f.addSugar" /> 是否加糖</label>
        <label class="checkbox-line"><input type="checkbox" v-model="f.nightUrgent" /> 夜间急煎(+20元优先排锅)</label>
      </div>

      <h4 class="mt16">药味明细 *</h4>
      <table>
        <thead>
          <tr><th style="width:200px">药材</th><th style="width:120px">单剂剂量(g)</th>
            <th style="width:130px">特殊煎法</th><th>备注</th><th style="width:60px"></th></tr>
        </thead>
        <tbody>
          <tr v-for="(h, hi) in f.herbs" :key="hi">
            <td>
              <input v-model="h.name" list="herb-list" placeholder="输入或选择药材" />
            </td>
            <td><input type="number" step="0.1" min="0.1" v-model.number="h.dosePerPacket" /></td>
            <td>
              <select v-model="h.specialMethod">
                <option v-for="e in meta.enums?.specialMethod||[]" :key="e.value" :value="e.value">{{ e.label }}</option>
              </select>
            </td>
            <td><input v-model="h.note" placeholder="先煎/后下说明等" /></td>
            <td><button class="btn sm danger" @click="f.herbs.splice(hi,1)">删</button></td>
          </tr>
        </tbody>
      </table>
      <button class="btn sm secondary mt8" @click="f.herbs.push(blankHerb())">+ 添加药味</button>
    </div>

    <datalist id="herb-list">
      <option v-for="h in meta.herbs" :key="h.id" :value="h.name">
        库存{{ h.stock }}g · {{ h.insuranceCovered ? '医保' : '自费' }} · 上限{{ h.maxDosePerPacket }}g
      </option>
    </datalist>

    <button v-if="batch" class="btn secondary" @click="forms.push(blankForm())">+ 再加一张处方</button>
    <button class="btn" :disabled="submitting" @click="submit">
      {{ submitting ? '提交中…' : (batch ? `批量提交 ${forms.length} 张处方` : '提交处方') }}
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, getUser } from '../api.js'
import { showToast } from '../use.js'

const route = useRoute()
const router = useRouter()
const user = getUser()
const isClinic = user.role === 'CLINIC'
const batch = isClinic && route.query.batch === '1'
const meta = ref({ enums: {} })
const submitting = ref(false)

const blankHerb = () => ({ name: '', dosePerPacket: 10, specialMethod: 'NORMAL', note: '' })
const blankForm = () => ({
  patientName: '', patientPhone: '', doctorId: '', doctorName: '',
  doses: 7, pickupMethod: 'DELIVERY', settlementType: 'INSURANCE',
  address: '', contactPhone: '', contraindications: '', specialDecoctionNote: '',
  addSugar: false, nightUrgent: false,
  herbs: [blankHerb(), blankHerb()]
})
const forms = ref([blankForm()])

onMounted(async () => {
  meta.value = await api.get('/meta')
})

async function submit() {
  for (const f of forms.value) {
    if (!isClinic && !f.patientName) f.patientName = user.displayName
    if (isClinic && !f.patientName) return showToast('请填写每张处方的患者姓名', 'error')
    if (!f.doses || f.doses < 1) return showToast('剂数必须≥1', 'error')
    if (f.pickupMethod !== 'SELF_PICKUP' && !f.address) return showToast('请填写配送地址', 'error')
    if (!f.herbs.length || f.herbs.some(h => !h.name || !h.dosePerPacket)) {
      return showToast('请完善药味名称与剂量', 'error')
    }
  }
  const normalize = (f) => ({
    ...f,
    doctorId: f.doctorId ? Number(f.doctorId) : null,
    clinicName: isClinic ? user.organization : undefined
  })
  submitting.value = true
  try {
    if (batch) {
      await api.post('/prescriptions/batch', { prescriptions: forms.value.map(normalize) })
      showToast(`已批量提交 ${forms.value.length} 张处方,等待药师审方`, 'ok')
    } else {
      await api.post('/prescriptions', normalize(forms.value[0]))
      showToast('处方已提交,等待药师审方', 'ok')
    }
    router.push('/prescriptions')
  } catch (e) {
    showToast(e.message, 'error')
  } finally {
    submitting.value = false
  }
}
</script>
