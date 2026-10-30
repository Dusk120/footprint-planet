import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../components/layout/AppLayout.vue'
import AdminLayout from '../components/admin/AdminLayout.vue'
import { useAppStore } from '../stores/app'
import { useToast } from '../composables/useToast'
import { getToken } from '../api/http'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginPage.vue'),
    meta: { guest: true }
  },
  {
    path: '/invite/:token',
    name: 'invite-accept',
    component: () => import('../views/InvitePage.vue'),
    meta: { title: '接受邀请', requireAuth: true }
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', redirect: '/community' },
      { path: 'my', name: 'my', component: () => import('../views/MyPage.vue'), meta: { title: '我的', requireAuth: true } },
      { path: 'footprint', name: 'footprint', component: () => import('../views/FootprintPage.vue'), meta: { title: '足迹点亮', requireAuth: true } },
      { path: 'memories', name: 'memories', component: () => import('../views/MemoriesPage.vue'), meta: { title: '旅行回忆', requireAuth: true } },
      { path: 'checklist', name: 'checklist', component: () => import('../views/ChecklistPage.vue'), meta: { title: '旅游规划', requireAuth: true } },
      { path: 'checklist/:id', name: 'plan-editor', component: () => import('../views/PlanEditorPage.vue'), meta: { title: '规划编辑', requireAuth: true } },
      { path: 'community', name: 'community', component: () => import('../views/CommunityPage.vue'), meta: { title: '旅游社区' } },
      { path: 'feedback', name: 'feedback', component: () => import('../views/FeedbackPage.vue'), meta: { title: '建议与反馈' } },
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requireAuth: true, requireAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('../views/admin/AdminDashboard.vue'), meta: { title: '数据概览' } },
      { path: 'users', name: 'admin-users', component: () => import('../views/admin/AdminUsers.vue'), meta: { title: '用户管理' } },
      { path: 'notes', name: 'admin-notes', component: () => import('../views/admin/AdminNotes.vue'), meta: { title: '笔记管理' } },
      { path: 'feedbacks', name: 'admin-feedbacks', component: () => import('../views/admin/AdminFeedback.vue'), meta: { title: '反馈管理' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundPage.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const store = useAppStore()

  // 需登录（普通 requireAuth 与 requireAdmin 均要求）
  if (to.meta.requireAuth || to.meta.requireAdmin) {
    // 冷启动：本地有 token 但登录态还没恢复时先等初始化，
    // 避免已登录用户刷新受保护页被误踢回登录页
    if (!store.isLoggedIn && getToken()) {
      try { await store.ensureInit() } catch (e) { /* bootstrap 内部已兜底登出 */ }
    }
    if (!store.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  // 管理端额外要求 role=ADMIN；冷启动时 profile 尚未回填，先等一次初始化再判角色
  if (to.meta.requireAdmin && !store.isAdmin) {
    if (!store.currentUser) {
      try { await store.ensureInit() } catch (e) { /* bootstrap 内部已兜底登出 */ }
    }
    if (!store.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    if (!store.isAdmin) {
      useToast().show('无权访问管理后台', 'error')
      return { name: 'community' }
    }
  }
})

export default router
