import { createRouter, createWebHashHistory } from 'vue-router'
import { store } from './store'

const routes = [
  { path: '/login', component: () => import('./views/Login.vue'), meta: { public: true } },
  { path: '/', component: () => import('./views/Dashboard.vue') },
  { path: '/prescriptions', component: () => import('./views/PrescriptionList.vue') },
  { path: '/prescriptions/new', component: () => import('./views/PrescriptionCreate.vue'), meta: { roles: ['PATIENT', 'CLINIC', 'ADMIN'] } },
  { path: '/prescriptions/:id', component: () => import('./views/PrescriptionDetail.vue'), props: true },
  { path: '/review', component: () => import('./views/ReviewBoard.vue'), meta: { roles: ['PHARMACIST', 'ADMIN'] } },
  { path: '/decoct', component: () => import('./views/DecoctBoard.vue'), meta: { roles: ['DECOCTER', 'ADMIN', 'PHARMACIST'] } },
  { path: '/delivery', component: () => import('./views/DeliveryBoard.vue'), meta: { roles: ['COURIER', 'ADMIN', 'PHARMACIST'] } },
  { path: '/exceptions', component: () => import('./views/ExceptionCenter.vue'), meta: { roles: ['ADMIN', 'PHARMACIST', 'DECOCTER', 'COURIER', 'FINANCE'] } },
  { path: '/followup', component: () => import('./views/FollowUpBoard.vue'), meta: { roles: ['PHARMACIST', 'ADMIN'] } },
  { path: '/finance', component: () => import('./views/FinanceBoard.vue'), meta: { roles: ['FINANCE', 'ADMIN', 'PHARMACIST', 'CLINIC', 'PATIENT'] } },
  { path: '/stats', component: () => import('./views/StatsBoard.vue'), meta: { roles: ['ADMIN', 'PHARMACIST'] } },
  { path: '/herbs', component: () => import('./views/HerbList.vue') }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(to => {
  if (to.meta.public) return true
  if (!store.token) return '/login'
  if (to.meta.roles && !to.meta.roles.includes(store.user?.role)) return '/'
  return true
})

export default router
