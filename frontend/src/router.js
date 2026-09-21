import { createRouter, createWebHashHistory } from 'vue-router'
import { getUser } from './api.js'
import Login from './views/Login.vue'
import Layout from './views/Layout.vue'
import Dashboard from './views/Dashboard.vue'
import RxList from './views/RxList.vue'
import RxCreate from './views/RxCreate.vue'
import RxDetail from './views/RxDetail.vue'
import ReviewQueue from './views/ReviewQueue.vue'
import DecoctQueue from './views/DecoctQueue.vue'
import DeliveryBoard from './views/DeliveryBoard.vue'
import Issues from './views/Issues.vue'
import Reminders from './views/Reminders.vue'
import Finance from './views/Finance.vue'
import Analytics from './views/Analytics.vue'

const routes = [
  { path: '/login', component: Login },
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', component: Dashboard },
      { path: 'prescriptions', component: RxList },
      { path: 'prescriptions/new', component: RxCreate },
      { path: 'prescriptions/:id', component: RxDetail, props: true },
      { path: 'review', component: ReviewQueue },
      { path: 'decoct', component: DecoctQueue },
      { path: 'delivery', component: DeliveryBoard },
      { path: 'issues', component: Issues },
      { path: 'reminders', component: Reminders },
      { path: 'finance', component: Finance },
      { path: 'analytics', component: Analytics }
    ]
  }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to) => {
  if (to.path !== '/login' && !getUser()) return '/login'
  if (to.path === '/login' && getUser()) return '/'
})

export default router
