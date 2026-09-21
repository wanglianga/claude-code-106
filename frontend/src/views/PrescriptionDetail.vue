<template>
  <div v-if="d.prescription">
    <div class="flex-between">
      <div>
        <div class="page-title">处方 {{ p.rxNo }}
          <span class="badge" :class="STATUS_CLASS[p.status]">{{ STATUS_NAMES[p.status] }}</span>
          <span v-if="p.urgent" class="badge b-danger">夜间急煎</span>
        </div>
        <div class="page-sub">提交于 {{ fmtTime(p.createdAt) }}<span v-if="p.batchNo"> · 批次 {{ p.batchNo }}</span></div>
      </div>
      <div class="flex wrap">
        <router-link v-if="canReview && p.status === 'PENDING_REVIEW'" to="/review" class="btn">去审方</router-link>
        <button v-if="canDispense" class="btn" @click="dispense">抓药完成 → 代煎排队</button>
        <button v-if="canPickupSign" class="btn" @click="pickupSign">取药签收</button>
        <router-link v-if="canFollowUp && p.status === 'SIGNED'" to="/followup" class="btn">去回访</router-link>
        <button class="btn btn-ghost" @click="showReport = true">上报异常</button>
      </div>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-if="okMsg" class="alert alert-ok">{{ okMsg }}</div>

    <div class="grid grid-2">
      <div class="card">
        <div class="card-title">处方信息</div>
        <table>
          <tbody>
            <tr><th>患者</th><td>{{ p.patientName }}（{{ p.patientGender || '—' }} / {{ p.patientAge ?? '—' }}岁）{{ p.patientPhone }}</td></tr>
            <tr><th>患者禁忌</th><td>{{ p.contraindications || '无' }}</td></tr>
            <tr><th>诊所/医生</th><td>{{ p.clinic?.name || '患者直投' }}<span v-if="p.doctor"> / {{ p.doctor.name }}({{ p.doctor.title }})</span></td></tr>
            <tr><th>剂数</th><td>{{ p.doses }} 剂</td></tr>
            <tr><th>特殊煎法</th><td>{{ p.specialDecoction || '无' }}</td></tr>
            <tr><th>是否加糖</th><td>{{ p.addSugar ? '加糖' : '不加糖' }}</td></tr>
            <tr><th>取药方式</th><td>{{ PICKUP_NAMES[p.pickupMethod] }}</td></tr>
            <tr v-if="p.pickupMethod === 'DELIVERY'"><th>配送地址</th><td>{{ p.deliveryAddress }}</td></tr>
            <tr v-if="p.pickupMethod === 'PROXY_PICKUP'"><th>代取人</th><td>{{ p.proxyName }} {{ p.proxyPhone }}</td></tr>
            <tr><th>结算方式</th><td>{{ p.settlementType ? SETTLE_NAMES[p.settlementType] : '待审方确定' }}</td></tr>
            <tr><th>费用</th><td>总额 {{ fmtMoney(p.totalAmount) }} / 医保 {{ fmtMoney(p.insuranceAmount) }} / 自费 {{ fmtMoney(p.selfPayAmount) }}</td></tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <div class="card-title">履约进度</div>
        <ul class="timeline">
          <li v-for="s in steps" :key="s.title" :class="{ done: s.done }">
            <div class="t-title">{{ s.title }}</div>
            <div class="t-time">{{ s.time || (s.done ? '' : '待处理') }}</div>
            <div v-if="s.desc" class="muted" style="font-size:12px">{{ s.desc }}</div>
          </li>
        </ul>
      </div>
    </div>

    <div class="card">
      <div class="card-title">药味明细({{ d.items.length }} 味)</div>
      <table>
        <thead><tr><th>药材</th><th>单剂剂量</th><th>特殊煎法</th><th>缺药替代</th><th>单价</th><th>小计</th></tr></thead>
        <tbody>
          <tr v-for="it in d.items" :key="it.id">
            <td>{{ it.herb.name }}<span v-if="!it.herb.inInsurance" class="badge b-warn">自费</span></td>
            <td>{{ it.dosageG }}g</td>
            <td><span v-if="it.specialHandling !== 'NONE'" class="badge b-info">{{ SPECIAL_NAMES[it.specialHandling] }}</span><span v-else class="muted">无</span></td>
            <td><span v-if="it.substituted" class="badge b-warn">{{ it.substituteNote }}</span><span v-else class="muted">—</span></td>
            <td>¥{{ it.unitPrice }}/g</td>
            <td>{{ fmtMoney(it.unitPrice * it.dosageG * p.doses) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="grid grid-2">
      <div class="card" v-if="d.review">
        <div class="card-title">审方记录</div>
        <table><tbody>
          <tr><th>结论</th><td><span class="badge" :class="d.review.conclusion === 'REJECTED' ? 'b-danger' : 'b-ok'">{{ CONCLUSION_NAMES[d.review.conclusion] }}</span></td></tr>
          <tr><th>配伍禁忌核对</th><td :class="{ 'danger-text': hasText(d.review.conflictResult) }">{{ d.review.conflictResult }}</td></tr>
          <tr><th>剂量核对</th><td :class="{ 'warn-text': hasText(d.review.doseResult) }">{{ d.review.doseResult }}</td></tr>
          <tr><th>缺药核对</th><td :class="{ 'warn-text': hasText(d.review.shortageResult) }">{{ d.review.shortageResult }}</td></tr>
          <tr><th>医保目录核对</th><td>{{ d.review.insuranceResult }}</td></tr>
          <tr><th>药师</th><td>{{ d.review.pharmacist?.name }} · {{ fmtTime(d.review.createdAt) }}</td></tr>
          <tr><th>备注</th><td>{{ d.review.note || '—' }}</td></tr>
        </tbody></table>
      </div>

      <div class="card" v-if="d.decoctTask">
        <div class="card-title">煎药任务</div>
        <table><tbody>
          <tr><th>锅号</th><td><b>{{ d.decoctTask.potNo }}</b> · 队列 #{{ d.decoctTask.queuePosition }}</td></tr>
          <tr><th>浸泡/煎煮</th><td>浸泡 {{ d.decoctTask.soakMinutes }} 分钟 · 煎煮 {{ d.decoctTask.decoctTimes }} 次</td></tr>
          <tr><th>包装袋数</th><td>{{ d.decoctTask.bagCount }} 袋</td></tr>
          <tr><th>配送波次</th><td>{{ d.decoctTask.waveNo }}</td></tr>
        </tbody></table>
        <div v-for="r in d.decoctRecords" :key="r.id" class="mt8" style="border-top:1px dashed var(--line);padding-top:8px">
          <div class="muted" style="font-size:12px">煎药记录 #{{ r.id }}</div>
          <table><tbody>
            <tr><th>扫描</th><td>{{ r.scanCode || '—' }} {{ fmtTime(r.scanTime) }}</td></tr>
            <tr><th>起止</th><td>{{ fmtTime(r.startTime) }} ~ {{ fmtTime(r.endTime) }}</td></tr>
            <tr><th>煎药员/复核人</th><td>{{ r.operator?.name || '—' }} / {{ r.reviewerName || '—' }}</td></tr>
            <tr><th>异常气味</th><td><span v-if="r.abnormalSmell" class="badge b-danger">{{ r.smellNote || '有' }}</span><span v-else>无</span></td></tr>
            <tr><th>漏袋</th><td><span v-if="r.leakedBags > 0" class="badge b-danger">{{ r.leakedBags }} 袋</span><span v-else>无</span></td></tr>
          </tbody></table>
        </div>
      </div>

      <div class="card" v-if="d.delivery">
        <div class="card-title">配送</div>
        <table><tbody>
          <tr><th>波次</th><td>{{ d.delivery.waveNo }}</td></tr>
          <tr><th>配送员</th><td>{{ d.delivery.courier?.name || '待认领' }}</td></tr>
          <tr><th>地址/收件人</th><td>{{ d.delivery.address }} · {{ d.delivery.receiverName }} {{ d.delivery.receiverPhone }}</td></tr>
          <tr><th>状态</th><td>
            <span class="badge" :class="d.delivery.status === 'SIGNED' ? 'b-ok' : 'b-info'">{{ DELIVERY_STATUS_NAMES[d.delivery.status] }}</span>
            <span v-if="d.delivery.timeout" class="badge b-danger">超时</span>
          </td></tr>
          <tr><th>派送/签收</th><td>{{ fmtTime(d.delivery.dispatchedAt) }} / {{ fmtTime(d.delivery.signedAt) }} {{ d.delivery.signedBy ? '· ' + d.delivery.signedBy : '' }}</td></tr>
        </tbody></table>
      </div>

      <div class="card" v-if="d.invoice">
        <div class="card-title">发票</div>
        <table><tbody>
          <tr><th>发票号</th><td>{{ d.invoice.invoiceNo }}</td></tr>
          <tr><th>金额/类型</th><td>{{ fmtMoney(d.invoice.amount) }} · {{ SETTLE_NAMES[d.invoice.type] }}</td></tr>
          <tr><th>状态</th><td><span class="badge" :class="d.invoice.status === 'ISSUED' ? 'b-ok' : 'b-warn'">{{ INVOICE_STATUS_NAMES[d.invoice.status] }}</span></td></tr>
          <tr v-if="d.invoice.reissueReason"><th>补开原因</th><td>{{ d.invoice.reissueReason }}</td></tr>
        </tbody></table>
      </div>
    </div>

    <div class="card" v-if="d.exceptions.length">
      <div class="card-title">异常事件({{ d.exceptions.length }})</div>
      <table>
        <thead><tr><th>类型</th><th>描述</th><th>状态</th><th>赔付</th><th>上报人</th><th>时间</th><th>处理</th></tr></thead>
        <tbody>
          <tr v-for="e in d.exceptions" :key="e.id">
            <td><span class="badge b-warn">{{ EX_TYPE_NAMES[e.type] }}</span></td>
            <td>{{ e.description }}</td>
            <td><span class="badge" :class="e.status === 'RESOLVED' ? 'b-ok' : 'b-danger'">{{ EX_STATUS_NAMES[e.status] }}</span></td>
            <td>{{ fmtMoney(e.compensation) }}</td>
            <td>{{ e.reportedBy?.name }}</td>
            <td class="muted">{{ fmtTime(e.createdAt) }}</td>
            <td>
              <span v-if="e.status === 'RESOLVED'" class="muted">{{ e.resolution }} · {{ e.handler?.name }}</span>
              <button v-else-if="canResolve" class="btn btn-sm" @click="openResolve(e)">处理</button>
              <span v-else class="muted">待处理</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="grid grid-2">
      <div class="card" v-if="d.followUps.length">
        <div class="card-title">回访记录</div>
        <table>
          <thead><tr><th>结果</th><th>满意度</th><th>说明</th><th>回访人</th><th>时间</th></tr></thead>
          <tbody>
            <tr v-for="f in d.followUps" :key="f.id">
              <td><span class="badge" :class="f.result === 'DISCOMFORT' ? 'b-danger' : 'b-ok'">{{ FOLLOWUP_NAMES[f.result] }}</span></td>
              <td>{{ f.satisfaction ? '★'.repeat(f.satisfaction) : '—' }}</td>
              <td>{{ f.discomfortDesc || f.note || '—' }}</td>
              <td>{{ f.createdBy?.name }}</td>
              <td class="muted">{{ fmtTime(f.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card" v-if="d.reminders.length">
        <div class="card-title">用药提醒</div>
        <table>
          <thead><tr><th>内容</th><th>提醒时间</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="r in d.reminders" :key="r.id">
              <td>{{ r.message }}</td>
              <td class="muted">{{ fmtTime(r.remindAt) }}</td>
              <td><span class="badge" :class="r.status === 'SENT' ? 'b-ok' : 'b-info'">{{ REMINDER_STATUS_NAMES[r.status] }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 上报异常弹窗 -->
    <div v-if="showReport" class="modal-mask" @click.self="showReport = false">
      <div class="modal">
        <div class="modal-title">上报异常</div>
        <div class="form-item mb8"><label>异常类型</label>
          <select v-model="report.type">
            <option v-for="(n, k) in EX_TYPE_NAMES" :key="k" :value="k">{{ n }}</option>
          </select>
        </div>
        <div v-if="report.type === 'ADDRESS_CHANGE'" class="form-item mb8">
          <label>新配送地址 *</label><input v-model="report.newAddress" />
        </div>
        <div class="form-item mb16"><label>情况说明</label><textarea v-model="report.description"></textarea></div>
        <div class="flex">
          <button class="btn" @click="submitReport">提交</button>
          <button class="btn btn-ghost" @click="showReport = false">取消</button>
        </div>
      </div>
    </div>

    <!-- 处理异常弹窗 -->
    <div v-if="resolveTarget" class="modal-mask" @click.self="resolveTarget = null">
      <div class="modal">
        <div class="modal-title">处理异常：{{ EX_TYPE_NAMES[resolveTarget.type] }}</div>
        <div class="muted mb8">{{ resolveTarget.description }}</div>
        <div class="form-item mb8"><label>处理结果</label><textarea v-model="resolveForm.resolution"></textarea></div>
        <div class="form-item mb16"><label>药房赔付金额(元，无则0)</label><input v-model.number="resolveForm.compensation" type="number" min="0" step="0.01" /></div>
        <div class="flex">
          <button class="btn" @click="submitResolve">确认处理</button>
          <button class="btn btn-ghost" @click="resolveTarget = null">取消</button>
        </div>
      </div>
    </div>
  </div>
  <div v-else-if="error" class="alert alert-error">{{ error }}</div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../api'
import { store } from '../store'
import {
  STATUS_NAMES, STATUS_CLASS, PICKUP_NAMES, SPECIAL_NAMES, EX_TYPE_NAMES, EX_STATUS_NAMES,
  SETTLE_NAMES, CONCLUSION_NAMES, FOLLOWUP_NAMES, DELIVERY_STATUS_NAMES, INVOICE_STATUS_NAMES,
  REMINDER_STATUS_NAMES, fmtTime, fmtMoney
} from '../dict'

const props = defineProps({ id: String })
const d = ref({ items: [], exceptions: [], followUps: [], reminders: [], decoctRecords: [] })
const error = ref('')
const okMsg = ref('')
const showReport = ref(false)
const report = reactive({ type: 'ADDRESS_CHANGE', description: '', newAddress: '' })
const resolveTarget = ref(null)
const resolveForm = reactive({ resolution: '', compensation: 0 })

const p = computed(() => d.value.prescription || {})
const role = store.user?.role

const canReview = computed(() => ['PHARMACIST', 'ADMIN'].includes(role))
const canDispense = computed(() => ['PHARMACIST', 'ADMIN'].includes(role) && p.value.status === 'DISPENSING')
const canPickupSign = computed(() => ['PHARMACIST', 'ADMIN'].includes(role) && p.value.status === 'READY_PICKUP')
const canFollowUp = computed(() => ['PHARMACIST', 'ADMIN'].includes(role))
const canResolve = computed(() => ['PHARMACIST', 'ADMIN'].includes(role))

const steps = computed(() => {
  const r = d.value.review
  const t = d.value.decoctTask
  const rec = (d.value.decoctRecords || [])[0]
  const del = d.value.delivery
  const fu = (d.value.followUps || [])[0]
  const isDelivery = p.value.pickupMethod === 'DELIVERY'
  return [
    { title: '处方提交', done: true, time: fmtTime(p.value.createdAt) },
    { title: '药师审方', done: !!r, time: r && fmtTime(r.createdAt), desc: r ? CONCLUSION_NAMES[r.conclusion] : '' },
    { title: '抓药 / 代煎排队', done: !!t, time: t && fmtTime(t.createdAt), desc: t ? `锅号 ${t.potNo} · ${t.waveNo}` : '' },
    { title: '煎药', done: !!rec?.endTime, time: rec?.endTime ? fmtTime(rec.endTime) : (rec?.startTime ? '煎药中…' : ''), desc: rec?.reviewerName ? `复核人 ${rec.reviewerName}` : '' },
    isDelivery
      ? { title: '配送', done: !!del?.signedAt, time: del?.signedAt ? fmtTime(del.signedAt) : (del?.dispatchedAt ? '配送中…' : ''), desc: del?.courier ? `配送员 ${del.courier.name}` : '' }
      : { title: '取药签收', done: ['SIGNED', 'FOLLOWED_UP', 'COMPLETED'].includes(p.value.status), time: '' },
    { title: '回访', done: !!fu, time: fu && fmtTime(fu.createdAt), desc: fu ? FOLLOWUP_NAMES[fu.result] : '' }
  ]
})

function hasText(s) {
  return s && s !== '未见异常'
}

async function load() {
  error.value = ''
  try {
    d.value = await api.get('/prescriptions/' + props.id)
  } catch (e) {
    error.value = e.message
  }
}

async function dispense() {
  try {
    await api.post(`/prescriptions/${props.id}/dispense`)
    okMsg.value = '抓药完成，已生成煎药任务并排队'
    await load()
  } catch (e) { error.value = e.message }
}

async function pickupSign() {
  const signedBy = prompt('请输入取药人姓名', p.value.proxyName || p.value.patientName)
  if (!signedBy) return
  try {
    await api.post(`/prescriptions/${props.id}/pickup-sign`, { signedBy })
    okMsg.value = '取药签收完成'
    await load()
  } catch (e) { error.value = e.message }
}

async function submitReport() {
  try {
    await api.post('/exceptions', {
      prescriptionId: Number(props.id), type: report.type,
      description: report.description, newAddress: report.newAddress || null
    })
    showReport.value = false
    okMsg.value = '异常已上报'
    report.description = ''; report.newAddress = ''
    await load()
  } catch (e) { error.value = e.message }
}

function openResolve(e) {
  resolveTarget.value = e
  resolveForm.resolution = ''
  resolveForm.compensation = 0
}

async function submitResolve() {
  try {
    await api.post(`/exceptions/${resolveTarget.value.id}/resolve`, resolveForm)
    resolveTarget.value = null
    okMsg.value = '异常已处理完结'
    await load()
  } catch (e) { error.value = e.message }
}

onMounted(load)
</script>
