<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '../../stores/app'

const route = useRoute()
const router = useRouter()
const store = useAppStore()

const menuItems = [
  { path: '/admin/dashboard', icon: 'dashboard', label: '数据概览' },
  { path: '/admin/users', icon: 'users', label: '用户管理' },
  { path: '/admin/notes', icon: 'notes', label: '笔记管理' },
  { path: '/admin/feedbacks', icon: 'feedbacks', label: '反馈管理' }
]

function isActive(path) {
  return route.path === path
}

function handleThemeToggle() {
  store.toggleTheme()
}

function goBackClient() {
  router.push('/community')
}

async function handleLogout() {
  await store.logout()
  router.push('/community')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-aside">
      <div class="admin-logo">
        <span class="admin-logo-icon">🌍</span>
        <div>
          <div class="admin-logo-title gradient-text">足迹星球</div>
          <div class="admin-logo-sub">管理后台</div>
        </div>
      </div>

      <nav class="admin-nav">
        <button
          v-for="item in menuItems"
          :key="item.path"
          class="admin-menu-item"
          :class="{ active: isActive(item.path) }"
          @click="router.push(item.path)"
        >
          <span class="admin-menu-icon">
            <template v-if="item.icon === 'dashboard'">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></svg>
            </template>
            <template v-else-if="item.icon === 'users'">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            </template>
            <template v-else-if="item.icon === 'notes'">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            </template>
            <template v-else-if="item.icon === 'feedbacks'">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><line x1="8" y1="9" x2="16" y2="9"/><line x1="8" y1="13" x2="13" y2="13"/></svg>
            </template>
          </span>
          <span class="admin-menu-label">{{ item.label }}</span>
        </button>
      </nav>

      <div class="admin-aside-footer">
        <button class="admin-back-btn" @click="goBackClient" title="返回客户端">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          <span>返回客户端</span>
        </button>
        <button class="admin-icon-btn" title="切换主题" @click="handleThemeToggle">
          <svg v-if="store.theme === 'dark'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
        </button>
        <button v-if="store.isLoggedIn" class="admin-icon-btn" title="退出登录" @click="handleLogout">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        </button>
      </div>
    </aside>

    <main class="admin-content">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </router-view>
    </main>
  </div>
</template>
