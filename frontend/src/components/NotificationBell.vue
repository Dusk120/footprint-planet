<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useNotificationStore } from '../stores/notification'
import { useRouter } from 'vue-router'
import { formatDateTime } from '../utils/format'

const store = useNotificationStore()
const router = useRouter()
const open = ref(false)

// 滚动容器 + 哨兵元素：滚到底自动加载下一页
const listWrapEl = ref(null)
const sentinelEl = ref(null)
let observer = null

function toggle() {
  open.value = !open.value
  if (open.value) refreshActive()
}

function close() {
  open.value = false
}

function switchTab(tab) {
  if (store.activeTab === tab) return
  store.activeTab = tab
  refreshActive()
}

// 打开面板 / 切换 tab 时：重置拉第一页（拿最新数据），再补到可滚动、回顶、挂 observer
function refreshActive() {
  const tab = store.activeTab
  store.fetchTab(tab, { reset: true }).then(async () => {
    await fillContainer()
    await nextTick()
    if (listWrapEl.value) listWrapEl.value.scrollTop = 0
    setupObserver()
  }).catch(() => {})
}

// 第一页撑不满容器时用户滚不动，也就永远触发不了加载——主动补到填满为止
async function fillContainer() {
  for (let i = 0; i < 5; i++) {
    await nextTick()
    const el = listWrapEl.value
    if (!el || el.clientHeight === 0) return
    const s = store.tabs[store.activeTab]
    if (!s.hasMore || s.loading) return
    if (el.scrollHeight > el.clientHeight) return
    const before = s.items.length
    await store.fetchTab(store.activeTab)
    if (s.items.length === before) return
  }
}

function setupObserver() {
  teardownObserver()
  if (!sentinelEl.value || !listWrapEl.value) return
  observer = new IntersectionObserver((entries) => {
    const s = store.tabs[store.activeTab]
    if (entries.some((e) => e.isIntersecting) && s.hasMore && !s.loading) {
      store.fetchTab(store.activeTab)
    }
  }, {
    // root 必须显式指定成滚动容器，否则哨兵相对视口始终可见，会一进面板就无限加载
    root: listWrapEl.value,
    rootMargin: '0px 0px 60px 0px',
  })
  observer.observe(sentinelEl.value)
}

function teardownObserver() {
  if (observer) {
    observer.disconnect()
    observer = null
  }
}

function onClickItem(n) {
  if (n.isRead !== 1) store.markRead(n.id, store.activeTab).catch(() => {})
  open.value = false
  if (n.noteId) {
    router.push({ path: '/community', query: { noteId: n.noteId } })
  } else if (router.currentRoute.value.path !== '/community') {
    router.push('/community')
  }
}

function onMarkAll() {
  store.markAllRead(store.activeTab).catch(() => {})
}

// 挂载时拉默认 tab（like），用后端 totalUnreadCount 反推出全局未读，红点/角标即时正确
onMounted(() => {
  store.fetchTab('like', { reset: true }).catch(() => {})
})
onBeforeUnmount(teardownObserver)
</script>

<template>
  <div class="notif-bell">
    <button class="bell-btn" title="通知" @click.stop="toggle">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
      </svg>
      <span v-if="store.totalUnread > 0" class="badge">{{ store.totalUnread > 99 ? '99+' : store.totalUnread }}</span>
    </button>

    <div v-if="open" class="panel-mask" @click="close"></div>
    <div v-if="open" class="panel">
      <div class="panel-head">
        <span class="panel-title">通知</span>
        <button v-if="store.unread[store.activeTab] > 0" class="mark-all" @click="onMarkAll">全部已读</button>
      </div>

      <div class="tabs">
        <button class="tab" :class="{ active: store.activeTab === 'like' }" @click="switchTab('like')">
          点赞
          <span v-if="store.unread.like > 0" class="tab-badge">{{ store.unread.like }}</span>
        </button>
        <button class="tab" :class="{ active: store.activeTab === 'comment' }" @click="switchTab('comment')">
          评论
          <span v-if="store.unread.comment > 0" class="tab-badge">{{ store.unread.comment }}</span>
        </button>
      </div>

      <div ref="listWrapEl" class="list">
        <div v-if="!store.tabs[store.activeTab].items.length" class="empty">暂无通知</div>
        <div v-else class="items">
          <div
            v-for="n in store.tabs[store.activeTab].items"
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

        <!-- 滚动哨兵：滚到它就加载下一页。始终渲染，否则 observer 没东西可观察 -->
        <div ref="sentinelEl" class="load-more-sentinel">
          <span v-if="store.tabs[store.activeTab].loading" class="load-more-text">加载中...</span>
          <span v-else-if="store.tabs[store.activeTab].hasMore" class="load-more-text">上滑加载更多</span>
          <span v-else-if="store.tabs[store.activeTab].items.length" class="load-more-text muted">没有更多通知了</span>
        </div>
      </div>
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

.tabs {
  display: flex;
  padding: 0 18px;
  border-bottom: 1px solid var(--dialog-divider);
}
.tab {
  position: relative;
  flex: none;
  padding: 10px 14px;
  background: none;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  border-bottom: 2px solid transparent;
  transition: var(--transition);
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.tab:hover {
  color: var(--text-primary);
}
.tab.active {
  color: var(--accent-primary);
  border-bottom-color: var(--accent-primary);
}
.tab-badge {
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

.list {
  overflow-y: auto;
  max-height: 512px;
}
.item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  height: 64px;
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

.load-more-sentinel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 4px 0;
  flex-shrink: 0;
}
.load-more-text {
  font-size: 11px;
  color: var(--text-secondary);
}
.load-more-text.muted {
  color: var(--text-muted);
}

.empty {
  padding: 40px 0;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
