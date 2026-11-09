import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../pages/Login.vue') },
    { path: '/onboarding', component: () => import('../pages/Onboarding.vue'), meta: { auth: true } },
    { path: '/', component: () => import('../pages/Home.vue'), meta: { auth: true } },
    { path: '/health', component: () => import('../pages/Health.vue'), meta: { auth: true } },
    { path: '/cycle', component: () => import('../pages/Cycle.vue'), meta: { auth: true } },
    { path: '/meal', component: () => import('../pages/Meal.vue'), meta: { auth: true } },
    { path: '/consult', component: () => import('../pages/Consult.vue'), meta: { auth: true } },
    { path: '/community', component: () => import('../pages/Community.vue'), meta: { auth: true } },
    { path: '/community/:id', component: () => import('../pages/PostDetail.vue'), meta: { auth: true } },
    { path: '/care', component: () => import('../pages/Care.vue'), meta: { auth: true } },
    { path: '/partners', component: () => import('../pages/Partners.vue'), meta: { auth: true } },
    { path: '/records', component: () => import('../pages/Records.vue'), meta: { auth: true } },
    { path: '/checkups', component: () => import('../pages/Checkups.vue'), meta: { auth: true } },
    { path: '/reports', component: () => import('../pages/Reports.vue'), meta: { auth: true } },
    { path: '/family', component: () => import('../pages/Family.vue'), meta: { auth: true } },
    { path: '/knowledge', component: () => import('../pages/Knowledge.vue'), meta: { auth: true } },
    { path: '/knowledge/:id', component: () => import('../pages/Article.vue'), meta: { auth: true } },
    { path: '/me', component: () => import('../pages/Me.vue'), meta: { auth: true } },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.token) return '/login'
})

export default router
