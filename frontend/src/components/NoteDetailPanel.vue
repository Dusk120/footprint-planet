<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'
import { useToast } from '../composables/useToast'
import NoteCover from './NoteCover.vue'
import NoteComments from './NoteComments.vue'

const props = defineProps({
  note: { type: Object, required: true },
  backText: { type: String, default: '返回' },
  authorClickable: { type: Boolean, default: false },
  canDelete: { type: Boolean, default: false },
  loginRedirect: { type: String, default: '/community' }
})

const emit = defineEmits(['back', 'delete', 'author-click', 'like'])

const store = useAppStore()
const router = useRouter()
const { show: toast } = useToast()

const currentPhoto = ref(0)
const photoList = computed(() => {
  if (!props.note) return []
  if (Array.isArray(props.note.photos) && props.note.photos.length) return props.note.photos
  return props.note.image ? [props.note.image] : []
})
watch(photoList, () => { currentPhoto.value = 0 })

function prevPhoto() {
  if (!photoList.value.length) return
  currentPhoto.value = (currentPhoto.value - 1 + photoList.value.length) % photoList.value.length
}

function nextPhoto() {
  if (!photoList.value.length) return
  currentPhoto.value = (currentPhoto.value + 1) % photoList.value.length
}

function requireLogin(action) {
  if (store.isLoggedIn) return true
  router.push({ name: 'login', query: { redirect: props.loginRedirect } })
  toast(`请先登录后再${action}`)
  return false
}

function isNoteAuthor() {
  return store.isMyNote(props.note)
}

// note.liked 由后端按当前登录用户算好，前端不再比对用户名
function isLikedByMe() {
  return !!props.note.liked
}

// 交给父组件的 handleLike 统一处理：它持有 communityNotes 里的同一对象引用，
// 更新后能同时刷新详情页与本页卡片（store 只维护「我的笔记」那份列表，这里改了它不会跟着变）
function handleLike() {
  if (!requireLogin('点赞')) return
  emit('like')
}
</script>

<template>
  <div class="detail-view">
    <div class="detail-topbar">
      <button class="back-btn" @click="emit('back')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
        {{ backText }}
      </button>
      <button v-if="canDelete && isNoteAuthor()" class="delete-note-btn" @click="emit('delete')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        删除笔记
      </button>
    </div>

    <div class="detail-layout">
      <!-- 左侧：笔记内容 -->
      <div class="detail-main glass-panel">
        <div class="detail-scroll">
          <div v-if="photoList.length" class="detail-image">
            <img :src="photoList[currentPhoto]" alt="" />
            <template v-if="photoList.length > 1">
              <button class="photo-arrow prev" @click="prevPhoto">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
              </button>
              <button class="photo-arrow next" @click="nextPhoto">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
              </button>
              <span class="photo-counter">{{ currentPhoto + 1 }} / {{ photoList.length }}</span>
            </template>
          </div>
          <div v-else class="detail-image">
            <NoteCover :note="note" variant="detail" />
          </div>

          <div v-if="photoList.length > 1" class="detail-thumbs">
            <button
              v-for="(p, i) in photoList"
              :key="i"
              class="detail-thumb"
              :class="{ active: i === currentPhoto }"
              @click="currentPhoto = i"
            >
              <img :src="p" alt="" />
            </button>
          </div>

          <div class="detail-body">
            <h1 class="detail-title">{{ note?.title || '无标题' }}</h1>

            <div class="detail-tags">
              <span v-if="note?.location" class="tag-location">
                📍 {{ note.location.name }}，{{ note.location.province }}
              </span>
            </div>

            <p class="detail-content">{{ note?.content || '无内容' }}</p>
          </div>
        </div>
      </div>

      <!-- 右侧：作者 + 点赞 + 评论 -->
      <div class="detail-sidebar">
        <!-- 作者卡片 -->
        <div class="sidebar-card glass-panel">
          <div class="sidebar-author" :class="{ clickable: authorClickable }" @click="authorClickable && emit('author-click', note?.authorId)">
            <img class="sidebar-author-avatar" :src="note?.authorAvatar || '/default-avatar.png'" :alt="note?.author || ''" />
            <div class="sidebar-author-info">
              <span class="sidebar-author-name">{{ note?.author || '未知作者' }}</span>
              <span class="sidebar-author-label">作者</span>
            </div>
          </div>
        </div>

        <!-- 点赞卡片 -->
        <div class="sidebar-card glass-panel">
          <h3 class="sidebar-card-title">点赞</h3>
          <div class="sidebar-like-row">
            <button class="detail-like-btn" :class="{ liked: isLikedByMe() }" @click="handleLike">
              <svg width="16" height="16" viewBox="0 0 24 24" :fill="isLikedByMe() ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
              {{ note?.likes || 0 }}
            </button>
          </div>
        </div>

        <!-- 评论卡片：列表/回复/点赞/删除都在 NoteComments 里 -->
        <div class="sidebar-card glass-panel">
          <h3 class="sidebar-card-title">评论</h3>
          <NoteComments :note-id="note.id" :login-redirect="loginRedirect" />
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.detail-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  padding-top: 8px;
}

.detail-topbar {
  display: flex;
  align-items: center;
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

.delete-note-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  background: rgba(248, 113, 113, 0.14);
  border: 1px solid rgba(248, 113, 113, 0.5);
  color: var(--danger);
  font-size: 13px;
  font-weight: 600;
  padding: 8px 14px;
  border-radius: var(--radius-xs);
  transition: var(--transition);
}
.delete-note-btn:hover {
  background: rgba(248, 113, 113, 0.28);
  border-color: var(--danger);
  box-shadow: 0 0 12px rgba(248, 113, 113, 0.35);
}

.detail-layout {
  flex: 1;
  display: flex;
  gap: 20px;
  min-height: 0;
}

.detail-main {
  flex: 1;
  border-radius: var(--radius);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.detail-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.detail-image {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
}
.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 多图切换 */
.photo-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
  z-index: 2;
}
.photo-arrow:hover {
  background: rgba(0, 0, 0, 0.65);
}
.photo-arrow.prev { left: 12px; }
.photo-arrow.next { right: 12px; }

.photo-counter {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  z-index: 2;
}

.detail-thumbs {
  display: flex;
  gap: 8px;
  padding: 10px 28px 0;
  overflow-x: auto;
}

.detail-thumb {
  width: 64px;
  height: 48px;
  flex-shrink: 0;
  border-radius: var(--radius-xs);
  overflow: hidden;
  border: 2px solid transparent;
  opacity: 0.55;
  transition: var(--transition);
}
.detail-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.detail-thumb:hover {
  opacity: 0.8;
}
.detail-thumb.active {
  border-color: var(--accent-primary);
  opacity: 1;
}

.detail-body {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-title {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.3;
}

.detail-tags {
  display: flex;
  gap: 8px;
}

.tag-location {
  padding: 4px 14px;
  border-radius: var(--radius-pill);
  background: rgba(var(--accent-rgb), 0.1);
  border: 1px solid rgba(var(--accent-rgb), 0.25);
  color: var(--accent-primary);
  font-size: 13px;
  font-weight: 500;
}

.detail-content {
  font-size: 15px;
  line-height: 1.9;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

/* Sidebar */
.detail-sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.sidebar-card {
  padding: 20px;
  border-radius: var(--radius);
}

.sidebar-card-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 14px;
}

.sidebar-author {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: var(--transition);
  padding: 4px;
  border-radius: var(--radius-xs);
}
.sidebar-author.clickable:hover {
  background: rgba(var(--overlay-rgb), 0.06);
}
.sidebar-author:not(.clickable) {
  cursor: default;
}

.sidebar-author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--accent-gradient);
  object-fit: cover;
  flex-shrink: 0;
}

.sidebar-author-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.sidebar-author-name {
  font-size: 14px;
  font-weight: 600;
}

.sidebar-author-label {
  font-size: 11px;
  color: var(--text-muted);
}

.sidebar-like-row {
  display: flex;
  gap: 8px;
}

.detail-like-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  background: rgba(var(--overlay-rgb), 0.06);
  border: 1px solid rgba(var(--overlay-rgb), 0.1);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  transition: var(--transition);
}
.detail-like-btn:hover {
  background: rgba(248, 113, 113, 0.1);
  color: var(--danger);
}
.detail-like-btn.liked {
  background: rgba(248, 113, 113, 0.15);
  border-color: rgba(248, 113, 113, 0.3);
  color: var(--danger);
}

@media (max-width: 900px) {
  .detail-layout {
    flex-direction: column;
  }
  .detail-sidebar {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
    overflow-y: visible;
  }
  .sidebar-card {
    flex: 1;
    min-width: 200px;
  }
}
</style>
