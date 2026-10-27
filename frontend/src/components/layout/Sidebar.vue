<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '../../stores/app'
import NotificationBell from '../NotificationBell.vue'

const route = useRoute()
const router = useRouter()
const store = useAppStore()

const baseItems = [
  { path: '/community', icon: 'community', label: '旅游社区' },
  { path: '/footprint', icon: 'footprint', label: '足迹点亮', requireAuth: true },
  { path: '/memories', icon: 'memories', label: '旅行回忆', requireAuth: true },
  { path: '/checklist', icon: 'checklist', label: '旅游规划', requireAuth: true },
  { path: '/my', icon: 'home', label: '我的', requireAuth: true },
  { path: '/feedback', icon: 'feedback', label: '建议与反馈' },
]

// 仅管理员展示「管理后台」入口
const navItems = computed(() => {
  const items = [...baseItems]
  if (store.isAdmin) {
    items.push({ path: '/admin/dashboard', icon: 'admin', label: '管理后台', requireAuth: true })
  }
  return items
})

function isActive(path) {
  return route.path === path
}

function handleNav(item) {
  if (item.requireAuth && !store.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: item.path } })
  } else {
    router.push(item.path)
  }
}

async function handleLogout() {
  await store.logout()
  router.push('/community')
}

function handleThemeToggle() {
  store.toggleTheme()
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar-logo">
      <span class="logo-icon">🌍</span>
      <span class="logo-text gradient-text">足迹星球</span>
    </div>

    <nav class="sidebar-nav">
      <button
        v-for="item in navItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
        @click="handleNav(item)"
      >
        <span class="nav-icon">
          <template v-if="item.icon === 'home'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12l9-9 9 9"/><path d="M9 21V12h6v9"/></svg>
          </template>
          <template v-else-if="item.icon === 'community'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
          </template>
          <template v-else-if="item.icon === 'footprint'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2C8 6 6 10 6 14c0 3.3 2.7 6 6 6s6-2.7 6-6c0-4-2-8-6-12z"/></svg>
          </template>
          <template v-else-if="item.icon === 'memories'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
          </template>
          <template v-else-if="item.icon === 'checklist'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
          </template>
          <template v-else-if="item.icon === 'feedback'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          </template>
          <template v-else-if="item.icon === 'admin'">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M12 8v4"/><circle cx="12" cy="14" r="0.5"/></svg>
          </template>
        </span>
        <span class="nav-label">{{ item.label }}</span>
      </button>
    </nav>

    <div class="sidebar-footer">
      <div class="footer-actions">
        <NotificationBell v-if="store.isLoggedIn" />
        <button class="theme-toggle" title="切换主题" @click="handleThemeToggle">
          <!-- 深色主题显示太阳（点击切到浅色），浅色主题显示月亮（点击切到深色） -->
          <svg v-if="store.theme === 'dark'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
        </button>
        <button v-if="store.isLoggedIn" class="logout-btn" @click="handleLogout" title="退出登录">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        </button>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--bg-card);
  border-right: 1px solid var(--bg-glass-border);
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  flex-shrink: 0;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 8px 28px;
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  font-size: 20px;
  font-weight: 800;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  background: transparent;
  border: none;
  color: var(--text-secondary);
  transition: var(--transition);
  position: relative;
  cursor: pointer;
  width: 100%;
  text-align: left;
  font-family: inherit;
  font-size: 15px;
}

.nav-item:hover {
  color: var(--text-primary);
  background: var(--bg-card-hover);
}

.nav-item.active {
  color: white;
  background: var(--accent-gradient);
  box-shadow: var(--shadow-glow);
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
}

.nav-label {
  font-size: 15px;
  font-weight: 500;
}

.sidebar-footer {
  padding-top: 16px;
  border-top: 1px solid var(--bg-glass-border);
}

.footer-actions {
  display: flex;
  gap: 4px;
}

.theme-toggle,
.logout-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-xs);
  background: var(--bg-card);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}
.theme-toggle:hover,
.logout-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.logout-btn:hover {
  color: var(--danger);
}

@media (max-width: 900px) {
  .nav-label,
  .logo-text {
    display: none;
  }
  .sidebar-logo {
    justify-content: center;
    padding-left: 0;
    padding-right: 0;
  }
  .nav-item {
    justify-content: center;
  }
}

</style>
