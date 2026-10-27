<script setup>
import { ref, onMounted } from 'vue'
import { useNotificationStore } from '../stores/notification'
import { useRouter } from 'vue-router'
import { formatDateTime } from '../utils/format'

const store = useNotificationStore()
const router = useRouter()
const open = ref(false)

function toggle() {
  open.value = !open.value
  if (open.value) store.fetchNotifications().catch(() => {})
}

function close() {
  open.value = false
}

function onClickItem(n) {
  if (n.isRead !== 1) store.markRead(n.id).catch(() => {})
  open.value = false
  // 跳转社区页并定位到具体笔记（无 noteId 的旧通知只跳社区）
  if (n.noteId) {
    router.push({ path: '/community', query: { noteId: n.noteId } })
  } else if (router.currentRoute.value.path !== '/community') {
    router.push('/community')
  }
}

function onMarkAll() {
  store.markAllRead().catch(() => {})
}

onMounted(() => {
  store.fetchNotifications().catch(() => {})
})
</script>

<template>
  <div class="notif-bell">
    <button class="bell-btn" title="通知" @click.stop="toggle">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
      </svg>
      <span v-if="store.unreadCount > 0" class="badge">{{ store.unreadCount > 99 ? '99+' : store.unreadCount }}</span>
    </button>

    <!-- 透明遮罩：点击面板外任意处关闭 -->
    <div v-if="open" class="panel-mask" @click="close"></div>
    <div v-if="open" class="panel">
      <div class="panel-head">
        <span class="panel-title">通知</span>
        <button v-if="store.unreadCount > 0" class="mark-all" @click="onMarkAll">全部已读</button>
      </div>
      <div v-if="store.notifications.length" class="list">
        <div
          v-for="n in store.notifications"
          :key="n.id"
          class="item"
          :class="{ unread: n.isRead !== 1 }"
          @click="onClickItem(n)"
        >
          <div class="item-avatar">{{ (n.senderNickname || '?')[0] }}</div>
          <div class="item-body">
            <div class="item-text"><b>{{ n.senderNickname }}</b> {{ n.content }}</div>
            <div class="item-meta">{{ n.noteTitle }} · {{ formatDateTime(n.createdAt) }}</div>
          </div>
          <span v-if="n.isRead !== 1" class="dot"></span>
        </div>
      </div>
      <div v-else class="empty">暂无通知</div>
    </div>
  </div>
</template>

<style scoped>
.notif-bell {
  position: relative;
}

.bell-btn {
  position: relative;
  z-index: 10000;
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
.bell-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--danger);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
  text-align: center;
}

.panel-mask {
  position: fixed;
  inset: 0;
  z-index: 9998;
  background: transparent;
}

.panel {
  position: fixed;
  top: 64px;
  right: 24px;
  width: 360px;
  max-width: calc(100vw - 32px);
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  background: var(--dialog-bg);
  border: 1px solid var(--dialog-border);
  border-radius: var(--radius);
  box-shadow: var(--dialog-shadow);
  overflow: hidden;
  z-index: 9999;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid var(--dialog-divider);
}
.panel-title {
  font-size: 15px;
  font-weight: 700;
}
.mark-all {
  background: none;
  color: var(--accent-primary);
  font-size: 13px;
}

.list {
  overflow-y: auto;
  flex: 1;
}
.item {
  display: flex;
  gap: 10px;
  padding: 12px 18px;
  cursor: pointer;
  transition: var(--transition);
  border-bottom: 1px solid var(--dialog-divider);
}
.item:hover {
  background: rgba(var(--overlay-rgb), 0.04);
}
.item.unread {
  background: rgba(var(--accent-rgb), 0.06);
}
.item-avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--accent-gradient);
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.item-body {
  flex: 1;
  min-width: 0;
}
.item-text {
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-primary);
}
.item-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dot {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--danger);
}

.empty {
  padding: 40px 0;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
