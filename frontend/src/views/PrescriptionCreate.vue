<template>
  <div>
    <div class="page-title">提交处方</div>
    <div class="page-sub">患者或诊所提交中药处方，药师审方后进入抓药与代煎流程</div>

    <div v-if="okMsg" class="alert alert-ok">{{ okMsg }}</div>
    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <div class="card">
      <div class="card-title">患者与就诊信息</div>
      <div class="form-row">
        <div class="form-item"><label>患者姓名 *</label><input v-model="form.patientName" /></div>
        <div class="form-item"><label>联系电话 *</label><input v-model="form.patientPhone" /></div>
        <div class="form-item"><label>年龄</label><input v-model.number="form.patientAge" type="number" /></div>
        <div class="form-item"><label>性别</label>
          <select v-model="form.patientGender"><option value="男">男</option><option value="女">女</option></select>
        </div>
      </div>
      <div class="form-row">
        <div class="form-item" v-if="store.user.role !== 'CLINIC'">
          <label>诊所(可选)</label>
          <select v-model="form.clinicId" @change="loadDoctors">
            <option :value="null">患者直投</option>
            <option v-for="c in clinics" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="form-item">
          <label>开方医生</label>
          <select v-model="form.doctorId">
            <option :value="null">未指定</option>
            <option v-for="doc in doctors" :key="doc.id" :value="doc.id">{{ doc.name }}({{ doc.title }})</option>
          </select>
        </div>
        <div class="form-item"><label>剂数 *</label><input v-model.number="form.doses" type="number" min="1" max="30" /></div>
        <div class="form-item" style="justify-content:flex-end">
          <label class="checkbox-line"><input type="checkbox" v-model="form.addSugar" /> 是否加糖</label>
          <label class="checkbox-line"><input type="checkbox" v-model="form.urgent" /> 夜间急煎</label>
        </div>
      </div>
      <div class="form-row">
        <div class="form-item"><label>患者禁忌(过敏史/妊娠/慢病等)</label>
          <textarea v-model="form.contraindications" placeholder="如：高血压病史、青霉素过敏、孕期等"></textarea>
        </div>
        <div class="form-item"><label>特殊煎法说明</label>
          <textarea v-model="form.specialDecoction" placeholder="如：附子先煎40分钟、文火慢煎等"></textarea>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-title">药味明细</div>
      <table>
        <thead><tr><th style="width:34%">药材</th><th>单剂剂量(g)</th><th>特殊煎法</th><th>库存/医保</th><th></th></tr></thead>
        <tbody>
          <tr v-for="(row, i) in form.items" :key="i">
            <td>
              <select v-model="row.herbId">
                <option :value="null">请选择药材</option>
                <option v-for="h in herbs" :key="h.id" :value="h.id">{{ h.name }}(¥{{ h.unitPrice }}/g)</option>
              </select>
            </td>
            <td><input v-model.number="row.dosageG" type="number" min="1" style="width:90px" /></td>
            <td>
              <select v-model="row.specialHandling">
                <option v-for="(n, k) in SPECIAL_NAMES" :key="k" :value="k">{{ n }}</option>
              </select>
            </td>
            <td>
              <span v-if="herbOf(row.herbId)" class="muted">
                库存 {{ herbOf(row.herbId).stockGram }}g
                <span v-if="!herbOf(row.herbId).inInsurance" class="badge b-warn">自费</span>
                <span v-else class="badge b-ok">医保</span>
              </span>
            </td>
            <td><button class="btn btn-danger btn-sm" @click="form.items.splice(i, 1)" v-if="form.items.length > 1">删</button></td>
          </tr>
        </tbody>
      </table>
      <button class="btn btn-ghost btn-sm mt8" @click="form.items.push({ herbId: null, dosageG: 10, specialHandling: 'NONE' })">+ 添加药味</button>
    </div>

    <div class="card">
      <div class="card-title">取药方式与配送</div>
      <div class="form-row">
        <div class="form-item">
          <label>取药方式 *</label>
          <select v-model="form.pickupMethod">
            <option v-for="(n, k) in PICKUP_NAMES" :key="k" :value="k">{{ n }}</option>
          </select>
        </div>
        <div class="form-item" v-if="form.pickupMethod === 'DELIVERY'">
          <label>配送地址 *</label><input v-model="form.deliveryAddress" placeholder="详细到门牌号" />
        </div>
        <template v-if="form.pickupMethod === 'PROXY_PICKUP'">
          <div class="form-item"><label>代取人姓名 *</label><input v-model="form.proxyName" /></div>
          <div class="form-item"><label>代取人电话</label><input v-model="form.proxyPhone" /></div>
        </template>
      </div>
      <div v-if="store.user.role === 'CLINIC'" class="flex wrap">
        <label class="checkbox-line"><input type="checkbox" v-model="batchMode" /> 诊所批量模式(连续录入同一批次)</label>
        <span v-if="batchMode" class="muted">批次号：{{ batchNo }}</span>
      </div>
    </div>

    <div class="flex">
      <button class="btn" :disabled="saving" @click="submit(false)">{{ saving ? '提交中…' : '提交处方' }}</button>
      <button v-if="batchMode" class="btn btn-warn" :disabled="saving" @click="submit(true)">提交并录入下一张</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { store } from '../store'
import { PICKUP_NAMES, SPECIAL_NAMES } from '../dict'

const router = useRouter()
const herbs = ref([])
const clinics = ref([])
const doctors = ref([])
const error = ref('')
const okMsg = ref('')
const saving = ref(false)
const batchMode = ref(false)
const batchNo = ref('B' + new Date().toISOString().slice(2, 10).replaceAll('-', '') + '-01')

const form = reactive({
  patientName: '', patientPhone: '', patientAge: null, patientGender: '男',
  contraindications: '', clinicId: null, doctorId: null, doses: 3,
  specialDecoction: '', addSugar: false, pickupMethod: 'DELIVERY',
  deliveryAddress: '', proxyName: '', proxyPhone: '', urgent: false,
  items: [{ herbId: null, dosageG: 10, specialHandling: 'NONE' }]
})

function herbOf(id) {
  return herbs.value.find(h => h.id === id)
}

async function loadDoctors() {
  const clinicId = store.user.role === 'CLINIC' ? store.user.clinic?.id : form.clinicId
  doctors.value = clinicId ? await api.get('/meta/doctors', { params: { clinicId } }) : []
}

async function submit(nextOne) {
  error.value = ''
  okMsg.value = ''
  if (!form.patientName || !form.patientPhone) { error.value = '请填写患者姓名与电话'; return }
  if (form.items.some(i => !i.herbId || !i.dosageG)) { error.value = '请完整填写药味与剂量'; return }
  saving.value = true
  try {
    const payload = { ...form, batchNo: batchMode.value ? batchNo.value : null }
    const created = await api.post('/prescriptions', payload)
    okMsg.value = `处方 ${created.rxNo} 提交成功，等待药师审方`
    if (nextOne) {
      form.patientName = ''; form.patientPhone = ''; form.patientAge = null
      form.contraindications = ''
      form.items = [{ herbId: null, dosageG: 10, specialHandling: 'NONE' }]
    } else {
      setTimeout(() => router.push('/prescriptions/' + created.id), 800)
    }
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  herbs.value = await api.get('/herbs')
  clinics.value = await api.get('/meta/clinics')
  await loadDoctors()
})
</script>
