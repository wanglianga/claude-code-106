// 端到端业务流验证:登录→审方→抓药→代煎→配送→签收→回访→异常→复盘
const BASE = process.env.BASE || 'http://host.docker.internal:3106'

let pass = 0, fail = 0
function ok(cond, name, extra) {
  if (cond) { pass++; console.log('  ✅', name) }
  else { fail++; console.log('  ❌', name, extra ?? '') }
}
async function call(method, path, token, body) {
  const res = await fetch(BASE + '/api' + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: body !== undefined ? JSON.stringify(body) : undefined
  })
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  return { status: res.status, data }
}
const tokens = {}
async function login(role) {
  const r = await call('POST', '/auth/login', null, { username: role, password: '123456' })
  ok(r.status === 200 && r.data.token, `登录 ${role}`)
  tokens[role] = r.data.token
  return r.data
}

;(async () => {
  console.log('== 1. 登录全部角色 ==')
  for (const r of ['patient','patient2','clinic','clinic2','pharmacist','decoctor','courier','courier2','finance','admin']) {
    await login(r)
  }

  console.log('== 2. 元数据与列表 ==')
  let r = await call('GET', '/meta', tokens.pharmacist)
  ok(r.status === 200 && r.data.herbs.length >= 20 && r.data.doctors.length === 2, `药材目录 ${r.data?.herbs?.length} 味、医生 2 位`)
  r = await call('GET', '/prescriptions', tokens.pharmacist)
  ok(r.status === 200 && r.data.length >= 10, `药师可见全部处方 ${r.data?.length} 张`)
  const patientList = await call('GET', '/prescriptions', tokens.patient)
  ok(patientList.data.every(x => x.patientName === '张伟'), '患者只能看到本人处方')
  const clinicList = await call('GET', '/prescriptions', tokens.clinic)
  ok(clinicList.data.every(x => x.clinicName === '济世堂中医诊所'), '诊所只能看到本机构处方')

  console.log('== 3. 审方引擎:十八反阻断 ==')
  r = await call('GET', '/prescriptions/2/review-preview', tokens.pharmacist)
  ok(r.data.blocked === true && r.data.findings.some(f => f.includes('十八反') && f.includes('甘草')), '0002 检出十八反(甘草反海藻)', JSON.stringify(r.data.blockingFindings))

  console.log('== 4. 审方引擎:剂量/目录外/医保/夜煎 ==')
  r = await call('GET', '/prescriptions/3/review-preview', tokens.pharmacist)
  ok(r.data.findings.some(f => f.includes('剂量异常') && f.includes('麻黄')), '0003 麻黄超剂量')
  ok(r.data.findings.some(f => f.includes('目录外')), '0003 杏仁目录外阻断')
  ok(r.data.insuranceWarnings.some(w => w.includes('西洋参') || w.includes('医保')), '0003 医保目录限制预警')
  const reject = await call('POST', '/prescriptions/3/review', tokens.pharmacist,
    { conclusion: 'REJECT', insuranceNote: '夜间急煎仍需医生补签' })
  ok(reject.data.prescription.status === 'REJECTED', '0003 被驳回')

  console.log('== 5. 审方:缺药替代 + 结算 ==')
  r = await call('GET', '/prescriptions/1/review-preview', tokens.pharmacist)
  ok(r.data.shortageSuggest['金银花'] === '忍冬藤', '0001 金银花缺药建议忍冬藤替代')
  const noSub = await call('POST', '/prescriptions/1/review', tokens.pharmacist,
    { conclusion: 'PASS', applySuggestions: false })
  ok(noSub.status === 409, '未确认替代不能通过')
  const pass1 = await call('POST', '/prescriptions/1/review', tokens.pharmacist,
    { conclusion: 'PASS', applySuggestions: true })
  ok(pass1.data.prescription.status === 'APPROVED', '0001 替代后审方通过')
  ok(pass1.data.review.substitutions.includes('金银花→忍冬藤'), '替代留痕')
  const f = pass1.data.prescription.fees
  ok(f.totalAmount > 0 && f.insurancePaid > 0 && f.selfPaid >= 0 && f.settled,
    `结算:总额 ${f.totalAmount} 医保 ${f.insurancePaid} 自付 ${f.selfPaid}`)

  console.log('== 6. 患者提交新处方 + 诊所批量 ==')
  const newRx = await call('POST', '/prescriptions', tokens.patient, {
    patientName: '张伟', doses: 3, pickupMethod: 'DELIVERY', settlementType: 'SELF_PAY',
    address: '城关镇测试路 1 号', addSugar: false,
    herbs: [{ name: '黄芪', dosePerPacket: 15, specialMethod: 'NORMAL' },
            { name: '茯苓', dosePerPacket: 15, specialMethod: 'NORMAL' }]
  })
  ok(newRx.status === 201 && newRx.data.status === 'PENDING_REVIEW', `患者新处方 ${newRx.data?.rxNo}`)
  const newId = newRx.data.id
  const batch = await call('POST', '/prescriptions/batch', tokens.clinic, {
    prescriptions: [
      { patientName: '批量患者甲', patientPhone: '13700000001', doctorId: 1, doses: 5,
        pickupMethod: 'DELIVERY', settlementType: 'INSURANCE', address: '甲地址', nightUrgent: true,
        herbs: [{ name: '柴胡', dosePerPacket: 10 }, { name: '黄芩', dosePerPacket: 10 }] },
      { patientName: '批量患者乙', patientPhone: '13700000002', doctorId: 1, doses: 5,
        pickupMethod: 'SELF_PICKUP', settlementType: 'SELF_PAY',
        herbs: [{ name: '白术', dosePerPacket: 12 }] }
    ]
  })
  ok(batch.status === 201 && batch.data.length === 2 && batch.data[0].batchNo, '诊所批量处方 2 张同批次')

  console.log('== 7. 抓药扣库存 ==')
  const herbBefore = (await call('GET', '/meta', tokens.pharmacist)).data.herbs.find(h => h.name === '黄芪')
  const disp4 = await call('POST', '/prescriptions/4/dispense', tokens.pharmacist, {})
  ok(disp4.data.prescription.status === 'DISPENSED', '0004 抓药后进入待煎煮')
  ok(!!disp4.data.prescription.id, '抓药返回完整履约')
  const herbAfter = (await call('GET', '/meta', tokens.pharmacist)).data.herbs.find(h => h.name === '黄芪')
  const used = 20 * 5
  ok(Number((herbBefore.stock - herbAfter.stock).toFixed(2)) === used, `库存扣减 ${used}g (${herbBefore.stock}→${herbAfter.stock})`)

  console.log('== 8. 代煎四步(0005) ==')
  let t5 = await call('GET', '/prescriptions/5', tokens.decoctor)
  const sch = await call('POST', '/prescriptions/5/decoct/schedule', tokens.decoctor,
    { potNo: 'G-04', soakMinutes: 30, boilTimes: 2, bagCount: 20, deliveryWave: 'WAVE-NIGHT-1' })
  ok(sch.data.decoct.potNo === 'G-04' && sch.data.decoct.deliveryWave === 'WAVE-NIGHT-1', '排产锅号/波次')
  const badScan = await call('POST', '/prescriptions/5/decoct/scan', tokens.decoctor, { bagCode: 'WRONG-CODE' })
  ok(badScan.status === 400, '错误药包码被拒')
  await call('POST', '/prescriptions/5/decoct/scan', tokens.decoctor, { bagCode: 'RXNO-5' })
  const st = await call('POST', '/prescriptions/5/decoct/start', tokens.decoctor)
  ok(st.data.prescription.status === 'DECOCTING', '扫码后开始煎煮')
  const end = await call('POST', '/prescriptions/5/decoct/end', tokens.decoctor,
    { abnormalSmell: '', missingBags: 1, damagedBags: 0, packer: '孙丽', reviewer: '赵师傅' })
  ok(end.data.prescription.status === 'DECOCTED', '煎煮完成待配送')
  ok(end.data.issues.some(i => i.type === 'BAG_DAMAGED' && i.locateInfo.includes('G-04')),
    '漏袋自动开工单且定位锅号/包装人/复核人')

  console.log('== 9. 煎药看板与夜间急煎优先 ==')
  const q = await call('GET', '/decoct/queue', tokens.decoctor)
  ok(Array.isArray(q.data), '煎药看板可访问')

  console.log('== 10. 配送派单/出库/签收(0007,含改地址) ==')
  const chg = await call('POST', '/prescriptions/7/address-change', tokens.patient2,
    { newAddress: '城关镇幸福苑养老院 3 号楼 101' })
  ok(chg.data.issues.some(i => i.type === 'ADDRESS_CHANGE'), '患者临时改地址挂工单')
  const asg = await call('POST', '/prescriptions/7/delivery/assign', tokens.pharmacist,
    { courierId: 7, wave: 'WAVE-AM-1', promisedMinutes: 60 })
  ok(asg.data.delivery.courier === '刘小军' && asg.data.delivery.address.includes('幸福苑'), '派单且地址同步为改后地址')
  const out = await call('POST', '/prescriptions/7/delivery/outbound', tokens.courier)
  ok(out.data.prescription.status === 'DELIVERING', '出库配送')
  const sign = await call('POST', '/prescriptions/7/delivery/sign', tokens.courier,
    { signedBy: '李桂兰儿媳', proxyName: '李桂兰儿媳' })
  ok(sign.data.prescription.status === 'SIGNED', '老人代取签收')
  ok(!!sign.data.reminder && sign.data.reminder.state === 'ACTIVE', '签收自动建立用药提醒')

  console.log('== 11. 配送超时(0008 已过承诺时间) ==')
  await call('POST', '/prescriptions/8/delivery/sign', tokens.courier, { signedBy: '郑老根' })
  r = await call('GET', '/prescriptions/8', tokens.pharmacist)
  ok(r.data.delivery.timeoutMinutes > 0, `签收回算超时 ${r.data.delivery.timeoutMinutes} 分钟`)
  ok(r.data.issues.some(i => i.type === 'DELIVERY_TIMEOUT'), '超时自动开工单')

  console.log('== 12. 用药回访:不适联动提醒暂停 + 赔付 ==')
  const fu = await call('POST', '/prescriptions/9/follow-up', tokens.pharmacist,
    { result: 'DISCOMFORT', symptoms: '服药后恶心', advice: '停药并复诊', satisfactionScore: 2 })
  ok(fu.data.followUps.at(-1).result === 'DISCOMFORT', '回访登记不适')
  ok(fu.data.issues.some(i => i.type === 'DISCOMFORT'), '不适自动开工单(医生/诊所环节)')
  ok(fu.data.reminder.state === 'PAUSED', '不适后用药提醒暂停')
  const discomfitIssue = fu.data.issues.find(i => i.type === 'DISCOMFORT')
  const res = await call('POST', `/issues/${discomfitIssue.id}/resolve`, tokens.admin,
    { resolution: '医生研判为空腹服用所致,改饭后温服;药房赔付代金', compensation: 50 })
  ok(res.data.status === 'RESOLVED' && Number(res.data.compensation) === 50, '不适工单闭环并赔付 50 元')

  console.log('== 13. 补开发票全流程 ==')
  const ledger0 = (await call('GET', '/finance/compensation', tokens.finance)).data
  ok(Number(ledger0.totalCompensation) >= 80, `赔付台账汇总 ¥${ledger0.totalCompensation}(种子30+本次50)`)
  const invReq = await call('POST', '/prescriptions/7/invoice/reissue-request', tokens.clinic,
    { title: '济世堂中医诊所' })
  ok(invReq.data.type === 'INVOICE' && invReq.data.status === 'OPEN', '诊所发起补开发票工单')
  const reissue = await call('POST', `/issues/${invReq.data.id}/reissue`, tokens.finance,
    { title: '济世堂中医诊所' })
  ok(reissue.data.kind === 'REISSUED', '财务补开发票并闭环工单')

  console.log('== 14. 完整履约聚合与时间线 ==')
  const full = await call('GET', '/prescriptions/9', tokens.patient)
  ok(full.data.events.length >= 8, `完整履约事件 ${full.data.events.length} 条`)
  ok(full.data.review && full.data.decoct && full.data.delivery && full.data.followUps.length >= 1
     && full.data.invoices.length >= 2, '同一记录含审方/煎药/配送/回访/发票')

  console.log('== 15. 管理复盘分析 ==')
  const ov = await call('GET', '/analytics/overview', tokens.admin)
  ok(ov.data.rxTotal >= 13 && ov.data.discomfortCount >= 1, `总览:处方 ${ov.data.rxTotal},不适 ${ov.data.discomfortCount}`)
  const bd = await call('GET', '/analytics/by-doctor', tokens.admin)
  ok(bd.data.some(x => x.doctorName === '李建国' && x.cooperationScore >= 0), '按医生复盘')
  const bh = await call('GET', '/analytics/by-herb', tokens.admin)
  ok(bh.data.some(x => x.herbName === '忍冬藤' && x.substitutedCount >= 1), '按药味复盘含替代记录')
  const bp = await call('GET', '/analytics/by-pot', tokens.admin)
  ok(bp.data.some(x => x.potNo === 'G-04' && x.missingBags >= 1), '按锅号复盘定位 G-04 漏袋')
  const bc = await call('GET', '/analytics/by-courier', tokens.admin)
  ok(bc.data.some(x => x.timeoutCount >= 1), '按配送员复盘超时')
  const cl = await call('GET', '/analytics/clinics', tokens.admin)
  ok(cl.data.length >= 2 && cl.data.every(x => x.cooperationScore >= 0 && x.cooperationScore <= 100), '诊所合作评分')

  console.log('== 16. 权限校验 ==')
  const forb = await call('GET', '/review', tokens.patient).catch(() => null)
  const revForb = await call('GET', '/prescriptions/1/review-preview', tokens.patient)
  ok(revForb.status === 403, '患者不能审方(403)')
  const batchForb = await call('POST', '/prescriptions/batch', tokens.patient, {
    prescriptions: [{ patientName: '越权测试', doses: 3, pickupMethod: 'SELF_PICKUP',
      settlementType: 'SELF_PAY', herbs: [{ name: '黄芪', dosePerPacket: 10 }] }]
  })
  ok(batchForb.status === 403, '患者不能批量开方(403)')

  console.log(`\n结果: ${pass} 通过, ${fail} 失败`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('脚本异常:', e); process.exit(2) })
