import { ref } from 'vue'
import { getUser } from './api.js'

export const toast = ref(null)
let timer = null
export function showToast(msg, type = '') {
  toast.value = { msg, type }
  clearTimeout(timer)
  timer = setTimeout(() => (toast.value = null), 3200)
}

export function role() {
  return getUser()?.role
}

export function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').substring(0, 16)
}

export const STATUS_TAG = {
  PENDING_REVIEW: 'orange',
  REJECTED: 'red',
  APPROVED: 'blue',
  DISPENSED: 'blue',
  READY_PICKUP: 'green',
  DECOCTING: 'orange',
  DECOCTED: 'blue',
  DELIVERING: 'orange',
  SIGNED: 'green',
  FOLLOWED_UP: 'green'
}

export function tagClass(status) {
  return STATUS_TAG[status] || 'gray'
}

export const STAGE_ICON = {
  PATIENT: '🧑',
  CLINIC: '🏥',
  PHARMACIST: '💊',
  DECOCTION: '🍵',
  DELIVERY: '🛵',
  FINANCE: '🧾'
}

/** 安全执行 API,统一弹 toast */
export async function run(promiseOrFn, okMsg) {
  try {
    const r = typeof promiseOrFn === 'function' ? await promiseOrFn() : await promiseOrFn
    if (okMsg) showToast(okMsg, 'ok')
    return r
  } catch (e) {
    showToast(e.message, 'error')
    throw e
  }
}
