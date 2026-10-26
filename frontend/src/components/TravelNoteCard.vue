<script setup>
import NoteCover from './NoteCover.vue'
import { formatLikes } from '../utils/format'

const props = defineProps({
  note: { type: Object, required: true },
  liked: { type: Boolean, default: false }
})

const emit = defineEmits(['like', 'click'])
</script>

<template>
  <div class="note-card glass-panel" @click="emit('click')">
    <div class="note-image">
      <NoteCover :note="note" />
      <div class="note-overlay">
        <button class="like-btn" :class="{ liked: props.liked }" @click.stop="emit('like')">
          <svg width="16" height="16" viewBox="0 0 24 24" :style="{ fill: props.liked ? 'var(--danger)' : 'white' }" stroke="none">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
          </svg>
        </button>
      </div>
    </div>
    <div class="note-info">
      <h4 class="note-title">{{ note.title }}</h4>
      <span class="note-location" v-if="note.location">📍 {{ note.location.name }}</span>
      <p v-if="note.content" class="note-content">{{ note.content }}</p>
      <div class="note-meta">
        <span class="note-author">@{{ note.author }}</span>
        <div class="note-stats">
          <span class="note-likes" :class="{ liked: props.liked }">
            <svg class="stat-icon" width="12" height="12" viewBox="0 0 24 24" stroke="none">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
            </svg>
            {{ formatLikes(note.likes) }}
          </span>
          <span class="note-comments">
            <svg class="stat-icon" width="12" height="12" viewBox="0 0 24 24" stroke="none">
              <path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/>
            </svg>
            {{ formatLikes(note.commentCount || 0) }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.note-card {
  border-radius: var(--radius);
  overflow: hidden;
  transition: var(--transition);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  max-height: 100%;
}
.note-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow);
}

.note-image {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
}

.note-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
}

.like-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}
.like-btn:hover {
  background: rgba(0, 0, 0, 0.5);
}

.note-info {
  padding: 16px;
  display: flex;
  flex-direction: column;
  flex: 1 0 auto;
  min-height: 0;
}

.note-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.note-content {
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-secondary);
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  overflow-wrap: anywhere;
  max-height: 45px;
}

.note-meta {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.note-author {
  font-size: 12px;
  color: var(--text-muted);
}

.note-location {
  display: inline-block;
  font-size: 12px;
  color: var(--accent-soft);
  background: rgba(var(--accent-rgb), 0.15);
  border: 1px solid rgba(var(--accent-rgb), 0.35);
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  margin-bottom: 8px;
}

.note-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.note-likes, .note-comments {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

/* 图标颜色必须跟随主题。之前这里写死成 rgba(var(--overlay-rgb), 0.4)，深色主题下没问题，
   一切到浅色主题，白图标就糊在白底上等于消失了。
   注意：SVG 的 fill="" 是 presentation attribute，**不认 var()**，
   所以只能写成 CSS 规则，不能直接在 fill 属性里用 CSS 变量 */
.note-likes .stat-icon,
.note-comments .stat-icon {
  fill: var(--text-muted);
}
.note-likes.liked .stat-icon { fill: var(--danger); }
</style>
