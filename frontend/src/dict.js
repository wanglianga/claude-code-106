// 枚举 → 中文与样式映射
export const STATUS_NAMES = {
  PENDING_REVIEW: '待审方',
  REVIEW_REJECTED: '审方驳回',
  DISPENSING: '抓药中',
  DECOCT_QUEUED: '代煎排队',
  DECOCTING: '煎药中',
  DECOCTED: '待配送',
  READY_PICKUP: '待取药',
  DELIVERING: '配送中',
  SIGNED: '已签收',
  FOLLOWED_UP: '已回访',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

export const STATUS_CLASS = {
  PENDING_REVIEW: 'b-warn',
  REVIEW_REJECTED: 'b-danger',
  DISPENSING: 'b-info',
  DECOCT_QUEUED: 'b-info',
  DECOCTING: 'b-info',
  DECOCTED: 'b-info',
  READY_PICKUP: 'b-info',
  DELIVERING: 'b-info',
  SIGNED: 'b-ok',
  FOLLOWED_UP: 'b-ok',
  COMPLETED: 'b-ok',
  CANCELLED: 'b-muted'
}

export const PICKUP_NAMES = {
  DELIVERY: '配送到家',
  SELF_PICKUP: '患者自提',
  PROXY_PICKUP: '老人代取'
}

export const SPECIAL_NAMES = {
  NONE: '无',
  PRE_DECOCT: '先煎',
  LATER_ADD: '后下',
  WRAPPED: '包煎',
  MELTED: '烊化',
  SEPARATE: '另煎',
  TAKEN_WITH: '冲服'
}

export const EX_TYPE_NAMES = {
  SHORTAGE: '缺药',
  ADDRESS_CHANGE: '患者改地址',
  SPECIAL_MISSED: '特殊煎法遗漏',
  BAG_DAMAGED: '包装破损/漏袋',
  DELIVERY_TIMEOUT: '配送超时',
  DISCOMFORT: '服药不适',
  INVOICE_REISSUE: '补开发票',
  QUALITY_ABNORMAL: '煎煮质量异常'
}

export const EX_STATUS_NAMES = {
  OPEN: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决'
}

export const SETTLE_NAMES = {
  MEDICAL_INSURANCE: '医保结算',
  SELF_PAY: '自费结算'
}

export const CONCLUSION_NAMES = {
  PASS: '通过',
  ADJUSTED_PASS: '调整后通过',
  REJECTED: '驳回'
}

export const FOLLOWUP_NAMES = {
  NORMAL: '服药正常',
  DISCOMFORT: '服药不适',
  UNREACHABLE: '未联系上'
}

export const DELIVERY_STATUS_NAMES = {
  PENDING: '待配送',
  DELIVERING: '配送中',
  SIGNED: '已签收'
}

export const INVOICE_STATUS_NAMES = {
  ISSUED: '已开具',
  REISSUE_REQUESTED: '补开申请中',
  REISSUED: '已补开'
}

export const REMINDER_STATUS_NAMES = {
  PENDING: '待提醒',
  SENT: '已提醒',
  CANCELLED: '已取消'
}

export function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}

export function fmtMoney(n) {
  return n != null ? '¥' + Number(n).toFixed(2) : '—'
}
