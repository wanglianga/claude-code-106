<template>
  <div v-if="d">
    <div class="page-title">
      📋 {{ d.prescription.rxNo }}
      <span class="tag" :class="tagClass(d.prescription.status)">{{ d.prescription.statusLabel }}</span>
      <span v-if="d.prescription.nightUrgent" class="tag red">夜间急煎</span>
      <router-link to="/prescriptions" class="btn sm secondary" style="margin-left:auto">返回列表</router-link>
    </div>

    <!-- 处方基础信息 -->
    <div class="grid grid-2">
      <div class="card">
        <h3>处方信息</h3>
        <div class="kv">
          <span class="k">患者</span><span>{{ d.prescription.patientName }} {{ d.prescription.patientPhone }}</span>
          <span class="k">诊所</span><span>{{ d.prescription.clinicName || '—' }} {{ d.prescription.batchNo ? '(' + d.prescription.batchNo + ')' : '' }}</span>
          <span class="k">医生</span><span>{{ d.prescription.doctorName || '—' }}</span>
          <span class="k">剂数</span><span>{{ d.prescription.doses }} 剂</span>
          <span class="k">取药方式</span><span>{{ d.prescription.pickupMethodLabel }}</span>
          <span class="k">结算方式</span><span>{{ d.prescription.settlementLabel }}</span>
          <span class="k">地址/联系人</span><span>{{ d.prescription.address }} · {{ d.prescription.contactName }} {{ d.prescription.contactPhone }}</span>
          <span class="k">禁忌</span><span>{{ d.prescription.contraindications || '无' }}</span>
          <span class="k">加糖</span><span>{{ d.prescription.addSugar ? '是' : '否' }}</span>
          <span class="k">特殊煎法</span><span>{{ d.prescription.specialDecoctionNote || '无' }}</span>
        </div>
        <h4 class="mt16">药味({{ d.prescription.herbs.length }})</h4>
        <table>
          <tr v-for="h in d.prescription.herbs" :key="h.id">
            <td><b>{{ h.herbName }}</b>
              <span v-if="h.substitutedFrom" class="tag orange" style="margin-left:6px">{{ h.substitutedFrom }} 替代</span>
            </td>
            <td>{{ h.dosePerPacket }}g/剂 × {{ d.prescription.doses }} = {{ h.totalQuantity }}g</td>
            <td><span class="tag" :class="h.specialMethod==='NORMAL'?'gray':'blue'">{{ h.specialLabel }}</span></td>
            <td>
              <span class="tag" :class="h.insuranceCovered ? 'green' : 'orange'">{{ h.insuranceCovered === null ? '—' : (h.insuranceCovered ? '医保' : '自费药') }}</span>
            </td>
            <td class="small muted">{{ h.note }}</td>
          </tr>
        </table>
      </div>

      <div>
        <!-- 费用结算 -->
        <div class="card">
          <h3>💰 费用与结算</h3>
          <div v-if="d.prescription.fees.totalAmount">
            <div class="kv">
              <span class="k">药费</span><span>¥{{ d.prescription.fees.herbAmount }}</span>
              <span class="k">代煎费</span><span>¥{{ d.prescription.fees.decoctFee }}(3元/剂)</span>
              <span class="k">配送费</span><span>¥{{ d.prescription.fees.deliveryFee }}</span>
              <span class="k">夜间急煎</span><span>¥{{ d.prescription.fees.nightSurcharge }}</span>
              <span class="k">合计</span><b>¥{{ d.prescription.fees.totalAmount }}</b>
              <span class="k">医保支付</span><span style="color:var(--ok)">¥{{ d.prescription.fees.insurancePaid }}</span>
              <span class="k">患者自付</span><span style="color:var(--danger)">¥{{ d.prescription.fees.selfPaid }}</span>
            </div>
          </div>
          <div v-else class="muted">审方通过后生成结算</div>
        </div>

        <!-- 审方 -->
        <div class="card">
          <h3>🔍 药师审方</h3>
          <div v-if="d.review">
            <div class="flex">
              <span class="tag" :class="d.review.conclusion==='REJECT'?'red':'green'">{{ d.review.conclusionLabel }}</span>
              <span class="muted small">{{ d.review.pharmacist }} · {{ fmt(d.review.reviewedAt) }}</span>
            </div>
            <ul class="small mt8" style="margin:6px 0 0 18px;padding:0">
              <li v-for="(f, i) in d.review.findings" :key="i"
                  :style="{color: f.includes('反')||f.includes('畏')||f.includes('剂量')||f.includes('目录外') ? 'var(--danger)' : 'inherit'}">{{ f }}</li>
            </ul>
            <div v-if="d.review.substitutions" class="small mt8">缺药替代:{{ d.review.substitutions }}</div>
            <div v-if="d.review.insuranceNote" class="small muted mt8">医保:{{ d.review.insuranceNote }}</div>
          </div>

          <!-- 药师操作区 -->
          <div v-if="can(['PHARMACIST']) && d.prescription.status==='PENDING_REVIEW'" class="mt16">
            <button class="btn sm" @click="loadPreview">实时审方校验</button>
            <div v-if="preview" class="mt8 small">
              <div v-for="(f, i) in preview.findings" :key="i"
                   :style="{color: preview.blockingFindings.includes(f) ? 'var(--danger)' : 'var(--warn)'}">
                {{ preview.blockingFindings.includes(f) ? '⛔' : '⚠️' }} {{ f }}
              </div>
              <div v-if="!preview.findings.length" style="color:var(--ok)">✅ 未发现配伍/剂量/库存问题</div>
              <div v-for="(w, i) in preview.insuranceWarnings" :key="'w'+i" style="color:var(--info)">ℹ️ {{ w }}</div>
              <div v-if="Object.keys(preview.shortageSuggest).length" class="mt8">
                <label class="checkbox-line">
                  <input type="checkbox" v-model="applySub" /> 应用缺药替代建议
                  ({{ Object.entries(preview.shortageSuggest).map(([a,b])=>a+'→'+b).join(',') }})
                </label>
              </div>
            </div>
            <div class="mt8">
              <button class="btn sm" @click="doReview('PASS')">审方通过并结算</button>
              <button class="btn sm danger" @click="doReview('REJECT')">驳回处方</button>
            </div>
          </div>
        </div>

        <!-- 抓药 -->
        <div class="card" v-if="can(['PHARMACIST']) && d.prescription.status==='APPROVED'">
          <h3>💊 抓药收费</h3>
          <p class="small muted">审核通过且费用已结算,确认按剂数抓药并扣减库存(代煎方自动移交煎药房排队,自取方等待到店)。</p>
          <button class="btn" @click="act('dispense', {})">确认抓药</button>
        </div>
      </div>
    </div>

    <!-- 煎药 -->
    <div class="card">
      <h3>🍵 代煎作业
        <span v-if="d.decoct" class="tag blue" style="margin-left:8px">{{ d.decoct.statusLabel }}</span>
      </h3>
      <div v-if="d.decoct">
        <div class="kv" style="max-width:760px">
          <span class="k">锅号</span><span>{{ d.decoct.potNo || '未排锅' }}</span>
          <span class="k">浸泡时间</span><span>{{ d.decoct.soakMinutes ?? '—' }} 分钟</span>
          <span class="k">煎煮次数</span><span>{{ d.decoct.boilTimes ?? '—' }} 次</span>
          <span class="k">包装袋数</span><span>{{ d.decoct.bagCount ?? '—' }} 个</span>
          <span class="k">配送波次</span><span>{{ d.decoct.deliveryWave || '—' }}</span>
          <span class="k">扫码药包</span><span>{{ d.decoct.scannedBagCode || '未扫码' }}</span>
          <span class="k">开始/结束</span><span>{{ fmt(d.decoct.startedAt) }} ~ {{ fmt(d.decoct.endedAt) }}</span>
          <span class="k">异常气味</span><span>{{ d.decoct.abnormalSmell || '无' }}</span>
          <span class="k">漏袋/破损</span>
          <span><b :style="{color: d.decoct.missingBags ? 'var(--danger)':'inherit'}">{{ d.decoct.missingBags }}</b> /
            <b :style="{color: d.decoct.damagedBags ? 'var(--danger)':'inherit'}">{{ d.decoct.damagedBags }}</b></span>
          <span class="k">包装人/复核人</span><span>{{ d.decoct.packer || '—' }} / {{ d.decoct.reviewer || '—' }}</span>
        </div>
      </div>
      <div v-else-if="!['SELF_PICKUP'].includes('') && d.prescription.pickupMethod==='SELF_PICKUP'" class="muted small">
        自取处方不安排代煎
      </div>
      <div v-else class="muted small">尚未进入煎药环节</div>

      <!-- 煎药员操作 -->
      <template v-if="can(['DECOCTOR'])">
        <div v-if="d.prescription.status==='DISPENSED' && !d.decoct?.potNo" class="mt16">
          <h4>安排代煎</h4>
          <div class="form-row">
            <div><label>锅号</label><input v-model="sch.potNo" placeholder="G-01" /></div>
            <div><label>浸泡(分钟)</label><input type="number" v-model.number="sch.soakMinutes" /></div>
            <div><label>煎煮次数</label><input type="number" v-model.number="sch.boilTimes" /></div>
            <div><label>包装袋数</label><input type="number" v-model.number="sch.bagCount" /></div>
            <div><label>配送波次</label><input v-model="sch.deliveryWave" placeholder="WAVE-PM-1" /></div>
          </div>
          <button class="btn mt8" @click="act('decoct/schedule', sch)">排产入队</button>
        </div>
        <div v-if="d.decoct && !d.decoct.scannedBagCode" class="mt8">
          <label>扫描药包条码(处方码 {{ d.prescription.rxNo }} 或 RXNO-{{ d.prescription.id }})</label>
          <div class="flex" style="max-width:420px">
            <input v-model="bagCode" placeholder="扫码/输入药包码" />
            <button class="btn" @click="act('decoct/scan', { bagCode })">扫码核对</button>
          </div>
        </div>
        <div v-if="d.prescription.status==='DISPENSED' && d.decoct?.scannedBagCode" class="mt8">
          <button class="btn" @click="act('decoct/start', null, 'POST')">开始煎煮</button>
        </div>
        <div v-if="d.prescription.status==='DECOCTING'" class="mt16">
          <h4>煎煮结束 · 包装与复核</h4>
          <div class="form-row">
            <div style="flex:2"><label>异常气味</label><input v-model="end.abnormalSmell" placeholder="无则留空" /></div>
            <div><label>漏袋数</label><input type="number" v-model.number="end.missingBags" /></div>
            <div><label>破损袋数</label><input type="number" v-model.number="end.damagedBags" /></div>
          </div>
          <div class="form-row">
            <div><label>包装人 *</label><input v-model="end.packer" /></div>
            <div><label>复核人 *</label><input v-model="end.reviewer" /></div>
          </div>
          <button class="btn mt8" @click="act('decoct/end', end)">完成包装复核</button>
        </div>
      </template>
    </div>

    <!-- 配送 -->
    <div class="card">
      <h3>🛵 配送与签收
        <span v-if="d.delivery" class="tag blue" style="margin-left:8px">{{ d.delivery.statusLabel }}</span>
      </h3>
      <div v-if="d.delivery">
        <div class="kv" style="max-width:760px">
          <span class="k">波次</span><span>{{ d.delivery.wave }}</span>
          <span class="k">配送员</span><span>{{ d.delivery.courier || '未指派' }} {{ d.delivery.courierPhone || '' }}</span>
          <span class="k">送达地址</span><span>{{ d.delivery.address }}</span>
          <span class="k">老人代取</span><span>{{ d.delivery.proxyName || '—' }}</span>
          <span class="k">承诺/签收</span><span>{{ fmt(d.delivery.promisedAt) }} / {{ fmt(d.delivery.signedAt) }}</span>
          <span class="k">签收人</span><span>{{ d.delivery.signedBy || '—' }}</span>
          <span class="k">超时</span>
          <span :style="{color: d.delivery.timeoutMinutes ? 'var(--danger)':'inherit'}">
            {{ d.delivery.timeoutMinutes ? d.delivery.timeoutMinutes + ' 分钟' : '无' }}</span>
        </div>
      </div>
      <div v-else class="muted small">尚未进入配送环节</div>

      <div class="mt16">
        <template v-if="can(['PHARMACIST','ADMIN','COURIER']) && d.prescription.status==='DECOCTED'">
          <h4>派单</h4>
          <div class="form-row">
            <div><label>配送员</label>
              <select v-model="assign.courierId">
                <option value="">暂不指派</option>
                <option v-for="c in meta.couriers" :key="c.id" :value="c.id">{{ c.displayName }} {{ c.phone }}</option>
              </select>
            </div>
            <div><label>波次</label><input v-model="assign.wave" placeholder="WAVE-PM-1" /></div>
            <div><label>承诺送达(分钟)</label><input type="number" v-model.number="assign.promisedMinutes" /></div>
          </div>
          <button class="btn mt8" @click="act('delivery/assign', assign)">派单</button>
        </template>
        <button v-if="can(['COURIER']) && d.delivery?.status==='ASSIGNED'" class="btn" @click="act('delivery/outbound')">出库配送</button>
        <button v-if="can(['COURIER','PATIENT','ADMIN']) && ['ASSIGNED','DELIVERING'].includes(d.delivery?.status)"
                class="btn" @click="openSign">患者签收</button>
        <button v-if="can(['PATIENT','CLINIC','COURIER']) && d.delivery?.status!=='SIGNED' && d.prescription.pickupMethod!=='SELF_PICKUP'"
                class="btn warn" @click="changeAddr">患者临时改地址</button>
      </div>
    </div>

    <!-- 回访 -->
    <div class="card">
      <h3>📞 用药回访(影响用药提醒/赔付/诊所评分)</h3>
      <table v-if="d.followUps.length">
        <tr v-for="(f, i) in d.followUps" :key="i">
          <td><span class="tag" :class="f.result==='DISCOMFORT'?'red':'green'">{{ f.resultLabel }}</span></td>
          <td>症状:{{ f.symptoms || '—' }}</td>
          <td>建议:{{ f.advice || '—' }}</td>
          <td>满意度:{{ f.satisfactionScore || '—' }}</td>
          <td class="small muted">{{ f.operator }} {{ fmt(f.followedAt) }}</td>
        </tr>
      </table>
      <div v-if="can(['PHARMACIST','ADMIN','CLINIC']) && ['SIGNED','FOLLOWED_UP'].includes(d.prescription.status)" class="mt8">
        <div class="form-row">
          <div><label>回访结果</label>
            <select v-model="fu.result">
              <option value="GOOD">服药正常</option>
              <option value="DISCOMFORT">服药后不适</option>
              <option value="NO_ANSWER">未联系上</option>
              <option value="REFUSED">拒绝回访</option>
            </select>
          </div>
          <div style="flex:2"><label>不适症状</label><input v-model="fu.symptoms" /></div>
          <div style="flex:2"><label>处置建议</label><input v-model="fu.advice" /></div>
          <div><label>满意度1-5</label><input type="number" min="1" max="5" v-model.number="fu.satisfactionScore" /></div>
        </div>
        <button class="btn mt8" @click="act('follow-up', fu)">提交回访</button>
      </div>
      <div v-if="d.reminder" class="mt8 small">
        用药提醒:<span class="tag" :class="d.reminder.state==='ACTIVE'?'green':'orange'">
          {{ {ACTIVE:'正常',PAUSED:'不适暂停',DONE:'疗程结束'}[d.reminder.state] }}
        </span>
        每日 {{ d.reminder.timesPerDay }} 次 · 下次 {{ fmt(d.reminder.nextRemindAt) }} · {{ d.reminder.note }}
      </div>
    </div>

    <!-- 异常工单 -->
    <div class="card">
      <h3>⚠️ 同处方异常工单({{ d.issues.length }})</h3>
      <table>
        <tr v-for="i in d.issues" :key="i.id">
          <td>{{ i.issueNo }}</td>
          <td><span class="tag orange">{{ i.typeLabel }}</span></td>
          <td><span class="tag" :class="i.status==='RESOLVED'?'green':'red'">{{ i.statusLabel }}</span></td>
          <td class="small">{{ i.description }}</td>
          <td class="small muted">{{ i.ownerStageLabel }} · 定位:{{ i.locateInfo }}</td>
          <td class="small" v-if="i.status==='RESOLVED'">
            解决:{{ i.resolution }}
            <span v-if="i.compensation>0" style="color:var(--danger)">赔付¥{{ i.compensation }}</span>
          </td>
        </tr>
      </table>
      <div class="mt8">
        <select v-model="issueType" style="max-width:220px;display:inline-block">
          <option v-for="e in meta.enums?.issueType||[]" :key="e.value" :value="e.value">{{ e.label }}</option>
        </select>
        <input v-model="issueDesc" placeholder="异常描述(如漏袋、不适症状、改地址详情)" style="max-width:420px" />
        <button class="btn sm" @click="reportIssue">登记异常</button>
      </div>
    </div>

    <!-- 发票 -->
    <div class="card" v-if="d.invoices.length || can(['FINANCE','CLINIC','PATIENT'])">
      <h3>🧾 发票</h3>
      <table v-if="d.invoices.length">
        <tr v-for="(v, i) in d.invoices" :key="i">
          <td>{{ v.invoiceNo }}</td><td>{{ v.title }}</td><td>¥{{ v.amount }}</td>
          <td><span class="tag" :class="v.kind==='REISSUED'?'blue':'gray'">{{ v.kind==='REISSUED'?'补开':'正常' }}</span></td>
          <td class="small muted">{{ fmt(v.issuedAt) }}</td>
        </tr>
      </table>
      <div class="mt8">
        <button v-if="can(['FINANCE'])" class="btn sm" @click="issueInvoice">开具发票</button>
        <button v-if="can(['CLINIC','PATIENT'])" class="btn sm secondary" @click="reissueReq">申请补开发票</button>
      </div>
    </div>

    <!-- 履约时间线 -->
    <div class="card">
      <h3>🧭 完整履约链(审方→抓药→代煎→配送→签收→回访)</h3>
      <ul class="timeline">
        <li v-for="(e, i) in d.events" :key="i">
          <div>
            <span class="tag gray">{{ STAGE_ICON[e.stage] }} {{ e.stageLabel }}</span>
            <b style="margin-left:6px">{{ e.action }}</b>
          </div>
          <div class="small">{{ e.detail }}</div>
          <div class="when">{{ e.operator }} · {{ fmt(e.occurredAt) }}</div>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api, getUser } from '../api.js'
import { tagClass, fmt, STAGE_ICON, showToast } from '../use.js'

const props = defineProps({ id: String })
const user = getUser()
const can = (roles) => roles.includes(user.role)

const d = ref(null)
const meta = ref({ enums: {} })
const preview = ref(null)
const applySub = ref(false)
const bagCode = ref('RXNO-' + props.id)
const sch = ref({ potNo: '', soakMinutes: 30, boilTimes: 2, bagCount: 14, deliveryWave: 'WAVE-PM-1' })
const end = ref({ abnormalSmell: '', missingBags: 0, damagedBags: 0, packer: '', reviewer: '', remark: '' })
const assign = ref({ courierId: '', wave: 'WAVE-PM-1', promisedMinutes: 60 })
const fu = ref({ result: 'GOOD', symptoms: '', advice: '', satisfactionScore: 5 })
const issueType = ref('BAG_DAMAGED')
const issueDesc = ref('')

async function load() {
  d.value = await api.get('/prescriptions/' + props.id)
  sch.value.bagCount = d.value.prescription.doses * 2
  bagCode.value = 'RXNO-' + d.value.prescription.id
}
onMounted(async () => {
  meta.value = await api.get('/meta')
  await load()
})

async function act(path, body) {
  try {
    d.value = await api.post(`/prescriptions/${props.id}/${path}`, body ?? {})
    showToast('操作成功', 'ok')
  } catch (e) { showToast(e.message, 'error') }
}

async function loadPreview() {
  preview.value = await api.get(`/prescriptions/${props.id}/review-preview`)
}
async function doReview(conclusion) {
  try {
    d.value = await api.post(`/prescriptions/${props.id}/review`,
      { conclusion, applySuggestions: applySub.value, insuranceNote: '', remark: '' })
    preview.value = null
    showToast('审方完成', 'ok')
  } catch (e) { showToast(e.message, 'error') }
}
function openSign() {
  const signedBy = prompt('签收人姓名(老人代取可填代取人)', d.value.prescription.patientName)
  if (signedBy === null) return
  const proxyName = d.value.prescription.pickupMethod === 'ELDER_PROXY' ? (prompt('代取人姓名', signedBy) || signedBy) : null
  act('delivery/sign', { signedBy, proxyName, address: d.value.delivery?.address })
}
async function changeAddr() {
  const a = prompt('新配送地址')
  if (!a) return
  try {
    d.value = await api.post(`/prescriptions/${props.id}/address-change`, { newAddress: a })
    showToast('改地址已登记并同步配送单', 'ok')
  } catch (e) { showToast(e.message, 'error') }
}
async function reportIssue() {
  if (!issueDesc.value) return showToast('请填写异常描述', 'error')
  try {
    await api.post(`/prescriptions/${props.id}/issues`, { type: issueType.value, description: issueDesc.value })
    issueDesc.value = ''
    await load()
    showToast('异常已登记到本处方履约记录', 'ok')
  } catch (e) { showToast(e.message, 'error') }
}
async function issueInvoice() {
  try { await api.post(`/prescriptions/${props.id}/invoice`, { title: d.value.prescription.patientName }); await load(); showToast('发票已开具', 'ok') }
  catch (e) { showToast(e.message, 'error') }
}
async function reissueReq() {
  try {
    await api.post(`/prescriptions/${props.id}/invoice/reissue-request`, { title: d.value.prescription.clinicName })
    await load(); showToast('补开发票申请已提交财务', 'ok')
  } catch (e) { showToast(e.message, 'error') }
}
</script>
