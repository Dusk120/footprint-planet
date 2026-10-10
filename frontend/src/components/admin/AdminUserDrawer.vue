<script setup>
import { ref, watch } from 'vue'
import { adminApi } from '../../api'
import { formatDateTime } from '../../utils/format'

const props = defineProps({
  userId: { type: Number, default: null }
})
const emit = defineEmits(['close'])

const detail = ref(null)
const loading = ref(false)
const error = ref('')

watch(() => props.userId, async (id) => {
  if (!id) return
  detail.value = null
  error.value = ''
  loading.value = true
  try {
    detail.value = await adminApi.getUser(id)
  } catch (e) {
    error.value = e.message || '加载用户详情失败'
  } finally {
    loading.value = false
  }
}, { immediate: true })

function formatKm(n) {
  return (Number(n) || 0).toLocaleString()
}
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="userId" class="drawer-overlay" @click.self="emit('close')">
        <div class="user-drawer">
          <div class="dialog-header">
            <span class="dialog-emoji">👤</span>
            <div class="dialog-title-wrap">
              <h2 class="dialog-title">用户详情</h2>
              <span class="dialog-sub">ID {{ userId }}</span>
            </div>
            <button class="drawer-close" title="关闭" @click="emit('close')">×</button>
          </div>

          <div class="dialog-body">
            <div v-if="loading" class="drawer-tip">加载中…</div>
            <div v-else-if="error" class="drawer-tip err">{{ error }}</div>

            <template v-else-if="detail">
              <div class="u-head">
                <img class="u-avatar" :src="detail.avatar" :alt="detail.nickname" />
                <div class="u-meta">
                  <span class="u-name">{{ detail.nickname || detail.username }}</span>
                  <span class="u-sub">@{{ detail.username }}</span>
                  <div class="u-badges">
                    <span class="admin-badge" :class="detail.role === 'ADMIN' ? 'admin-badge-admin' : 'admin-badge-user'">{{ detail.role }}</span>
                    <span class="admin-badge" :class="detail.status === 0 ? 'admin-badge-ok' : 'admin-badge-warn'">
                      {{ detail.status === 0 ? '正常' : '禁用' }}
                    </span>
                  </div>
                </div>
              </div>

              <div class="u-stats">
                <div class="u-stat">
                  <span class="u-num">{{ detail.visitedCount ?? 0 }}</span>
                  <span class="u-lbl">点亮城市</span>
                </div>
                <div class="u-stat">
                  <span class="u-num">{{ detail.visitedProvinces ?? 0 }}</span>
                  <span class="u-lbl">累计省份</span>
                </div>
                <div class="u-stat">
                  <span class="u-num">{{ detail.noteCount ?? 0 }}</span>
                  <span class="u-lbl">发布笔记</span>
                </div>
                <div class="u-stat">
                  <span class="u-num">{{ formatKm(detail.totalKm) }}</span>
                  <span class="u-lbl">飞行里程 km</span>
                </div>
                <div class="u-stat">
                  <span class="u-num">{{ detail.totalDays ?? 0 }}</span>
                  <span class="u-lbl">旅行天数</span>
                </div>
              </div>

              <div class="u-rows">
                <div class="u-row">
                  <span class="u-key">用户 ID</span>
                  <span class="u-val">{{ detail.id }}</span>
                </div>
                <div class="u-row">
                  <span class="u-key">注册时间</span>
                  <span class="u-val">{{ formatDateTime(detail.createdAt) }}</span>
                </div>
                <div class="u-row">
                  <span class="u-key">个性签名</span>
                  <span class="u-val">{{ detail.motto || '—' }}</span>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(5, 8, 30, 0.72);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  justify-content: flex-end;
  z-index: 10000;
}

.user-drawer {
  width: 420px;
  max-width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--dialog-bg);
  border-left: 1px solid var(--dialog-border);
  box-shadow: var(--dialog-shadow);
  overflow: hidden;
}

.drawer-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--dialog-cancel-bg);
  color: var(--text-secondary);
  font-size: 20px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: var(--transition);
}
.drawer-close:hover { background: var(--dialog-cancel-hover); color: var(--text-primary); }

.drawer-tip {
  text-align: center;
  padding: 40px 0;
  color: var(--text-muted);
  font-size: 14px;
}
.drawer-tip.err { color: #f87171; }

.u-head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.u-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid var(--dialog-border);
  background: var(--dialog-input-bg);
}

.u-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.u-name { font-size: 17px; font-weight: 700; }
.u-sub { font-size: 12px; color: var(--text-muted); }
.u-badges { display: flex; gap: 6px; margin-top: 2px; }

.u-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.u-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 12px 8px;
  border-radius: var(--radius-xs);
  background: var(--dialog-item-bg);
  border: 1px solid var(--dialog-day-border);
}

.u-num { font-size: 17px; font-weight: 800; }
.u-lbl { font-size: 11px; color: var(--text-muted); }

.u-rows { display: flex; flex-direction: column; }

.u-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 11px 0;
  border-bottom: 1px solid var(--dialog-divider);
  font-size: 13px;
}
.u-row:last-child { border-bottom: none; }

.u-key { color: var(--text-muted); flex-shrink: 0; }
.u-val { color: var(--text-secondary); text-align: right; word-break: break-word; }

.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.25s ease;
}
.drawer-enter-active .user-drawer,
.drawer-leave-active .user-drawer {
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}
.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}
.drawer-enter-from .user-drawer,
.drawer-leave-to .user-drawer {
  transform: translateX(100%);
}
</style>
