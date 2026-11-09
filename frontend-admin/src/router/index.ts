import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../pages/Login.vue') },
    {
      path: '/',
      component: () => import('../layouts/OrgLayout.vue'),
      meta: { auth: true },
      children: [
        {
          path: '',
          component: () => import('../pages/Dashboard.vue'),
          meta: { title: '全周期看板', subtitle: '女性健康、社区与机构协作的聚合视图' },
        },
        {
          path: 'members',
          component: () => import('../pages/Members.vue'),
          meta: { title: '用户管理', subtitle: '本机构会员的基础资料、线索状态与风险摘要' },
        },
        {
          path: 'alerts',
          component: () => import('../pages/Alerts.vue'),
          meta: { title: 'AI 风控告警', subtitle: '社区与情绪打卡的消极信号，AI 自动筛查' },
        },
        {
          path: 'partners',
          component: () => import('../pages/Partners.vue'),
          meta: { title: '合作机构', subtitle: '妇幼保健院、月子中心、产后康复与社区机构' },
        },
        {
          path: 'leads',
          component: () => import('../pages/Leads.vue'),
          meta: { title: '预约线索', subtitle: '用户主动提交的预约意向，不含健康档案' },
        },
        {
          path: 'tickets',
          component: () => import('../pages/Tickets.vue'),
          meta: { title: '风险跟进', subtitle: 'EPDS 筛查与私密咨询转介工单' },
        },
        {
          path: 'community',
          component: () => import('../pages/Moderation.vue'),
          meta: { title: '社区审核', subtitle: '危机词与导流内容人工复核' },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('org_token')
  if (to.meta.auth && !token) return '/login'
})

export default router
