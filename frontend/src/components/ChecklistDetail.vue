<script setup>
import { ref } from 'vue'

const emit = defineEmits(['edit', 'delete', 'back', 'use'])

const props = defineProps({
  item: { type: Object, required: true },
  hideTopbar: { type: Boolean, default: false },
  backText: { type: String, default: '返回列表' },
  // 只读分享模式：顶栏显示「使用」按钮，替代编辑/删除
  useAction: { type: Boolean, default: false }
})

const expandedDays = ref(new Set(props.item.days?.map((_, i) => i) || []))

function toggleDay(i) {
  if (expandedDays.value.has(i)) {
    expandedDays.value.delete(i)
  } else {
    expandedDays.value.add(i)
  }
}

function formatDate(d) {
  if (!d) return '未设定'
  return d
}
</script>

<template>
  <div class="detail">
    <div v-if="!hideTopbar" class="detail-topbar">
      <button class="back-btn" @click="emit('back')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
        {{ backText }}
      </button>
      <div class="topbar-actions">
        <button v-if="useAction" class="action-btn use" @click="emit('use')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l2.4 4.86 5.37.78-3.88 3.78.92 5.34L12 14.63 7.19 16.76l.92-5.34L4.23 7.64l5.37-.78L12 2z"/></svg>
          使用这份规划
        </button>
        <template v-else>
          <button class="action-btn" @click="emit('edit')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            编辑
          </button>
          <button class="action-btn delete" @click="emit('delete')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            删除
          </button>
        </template>
      </div>
    </div>

    <!-- 基础信息 -->
    <div class="info-card glass-panel">
      <h2 class="info-name">{{ item.name }}</h2>
      <div class="info-meta">
        <span class="meta-tag" :class="{ 'meta-private': !item.isPublic }">
          {{ item.isPublic ? '🌐 公开' : '🔒 仅自己可见' }}
        </span>
        <span class="meta-tag">📍 {{ item.destination }}</span>
        <span v-if="item.location" class="meta-tag">{{ item.location }}</span>
        <span class="meta-tag">📅 {{ formatDate(item.startDate) }}</span>
        <span class="meta-tag">{{ item.days?.length || 0 }} 天行程</span>
      </div>
    </div>

    <!-- 途径点 -->
    <div v-if="item.waypoints?.length" class="section">
      <h3 class="section-title">途径点 / 中转站</h3>
      <div class="wp-list">
        <div v-for="(wp, i) in item.waypoints" :key="i" class="wp-item">
          <span class="wp-dot">📍</span>
          <div class="wp-info">
            <span class="wp-name">{{ wp.name }}</span>
            <span v-if="wp.note" class="wp-note">{{ wp.note }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 每日行程 -->
    <div class="section">
      <h3 class="section-title">每日行程</h3>
      <div v-if="!item.days?.length" class="empty-hint">暂未规划行程</div>
      <div v-for="(day, di) in item.days" :key="di" class="day-block">
        <button class="day-header" @click="toggleDay(di)">
          <div class="day-left">
            <span class="day-num">{{ di + 1 }}</span>
            <span class="day-title">{{ day.title }}</span>
            <span class="day-count">{{ day.items.length }} 个行程</span>
          </div>
          <svg class="day-arrow" :class="{ open: expandedDays.has(di) }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9l6 6 6-6"/></svg>
        </button>

        <div v-show="expandedDays.has(di)" class="day-items">
          <div v-for="(item, ii) in day.items" :key="ii" class="timeline-item">
            <div class="tl-time">{{ item.time || '--:--' }}</div>
            <div class="tl-dot"></div>
            <div class="tl-content">
              <span class="tl-place">{{ item.place }}</span>
              <span v-if="item.note" class="tl-note">{{ item.note }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
  overflow-y: auto;
}

.detail-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  color: var(--text-secondary);
  font-size: 14px;
  padding: 8px 12px;
  border-radius: var(--radius-xs);
  transition: var(--transition);
}
.back-btn:hover { background: var(--bg-card-hover); color: var(--text-primary); }

.topbar-actions { display: flex; gap: 8px; }

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--radius-xs);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 13px;
  transition: var(--transition);
}
.action-btn:hover { background: var(--bg-card-hover); color: var(--text-primary); }
.action-btn.delete:hover { color: var(--danger); }
.action-btn.use {
  background: var(--accent-gradient);
  color: white;
}
.action-btn.use:hover { background: var(--accent-gradient); opacity: 0.9; color: white; }

.info-card { padding: 24px; }
.info-name { font-size: 22px; font-weight: 800; margin-bottom: 12px; }
.info-meta { display: flex; flex-wrap: wrap; gap: 8px; }
.meta-tag {
  padding: 5px 12px;
  border-radius: var(--radius-pill);
  background: rgba(56, 189, 248, 0.1);
  color: var(--accent-blue);
  font-size: 13px;
  font-weight: 500;
}
.meta-tag.meta-private {
  background: rgba(var(--overlay-rgb), 0.07);
  color: var(--text-secondary);
}

.section { display: flex; flex-direction: column; gap: 12px; }
.section-title { font-size: 16px; font-weight: 700; }
.empty-hint { color: var(--text-muted); font-size: 14px; padding: 12px 0; }

.wp-list { display: flex; flex-direction: column; gap: 8px; }
.wp-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  background: rgba(var(--overlay-rgb), 0.04);
}
.wp-dot { font-size: 16px; }
.wp-info { display: flex; flex-direction: column; }
.wp-name { font-size: 14px; font-weight: 600; }
.wp-note { font-size: 12px; color: var(--text-muted); }

.day-block {
  border-radius: var(--radius-sm);
  background: rgba(var(--overlay-rgb), 0.03);
  border: 1px solid rgba(var(--overlay-rgb), 0.06);
  overflow: hidden;
}

.day-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: none;
  color: var(--text-primary);
  transition: var(--transition);
}
.day-header:hover { background: rgba(var(--overlay-rgb), 0.03); }

.day-left { display: flex; align-items: center; gap: 10px; }

.day-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent-gradient);
  color: white;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.day-title { font-size: 15px; font-weight: 700; }
.day-count { font-size: 12px; color: var(--text-muted); }

.day-arrow { transition: transform 0.2s; color: var(--text-muted); }
.day-arrow.open { transform: rotate(180deg); }

.day-items {
  padding: 0 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.timeline-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(var(--overlay-rgb), 0.04);
}
.timeline-item:last-child { border-bottom: none; }

.tl-time {
  font-size: 13px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
  width: 50px;
  flex-shrink: 0;
  padding-top: 2px;
}

.tl-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent-blue);
  margin-top: 6px;
  flex-shrink: 0;
}

.tl-content { display: flex; flex-direction: column; gap: 2px; }
.tl-place { font-size: 14px; font-weight: 600; }
.tl-note { font-size: 12px; color: var(--text-muted); }
</style>
