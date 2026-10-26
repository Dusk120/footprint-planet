<script setup>
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'
import { useToast } from '../composables/useToast'
import { formatDateTime } from '../utils/format'
import AdminConfirmDialog from './admin/AdminConfirmDialog.vue'

const props = defineProps({
  noteId: { type: Number, required: true },
  loginRedirect: { type: String, default: '/community' }
})

const store = useAppStore()
const router = useRouter()
const { show: toast } = useToast()

// 后端 LocalDateTime 序列化成 ISO（2026-09-24T09:31:39.5548682），统一格式化

const comments = ref([])
const loading = ref(false)
const hasMore = ref(true)
const page = ref(1)
const total = ref(0)
const PAGE_SIZE = 10 // 滚动加载，一次多拿几条，免得刚填满容器就又要加载

// 后端评论接口不存在时（code 404）静默降级：不弹 toast、隐藏输入框
const commentsReady = ref(true)

// 滚动容器 + 哨兵元素：滚到底自动加载下一页
const listWrapEl = ref(null)
const sentinelEl = ref(null)
let observer = null

function isEndpointMissing(e) {
  return e && (e.code === 404 || e.status === 404)
}

// ---------- 顶层评论输入 ----------
const newComment = ref('')
const submitting = ref(false)

// ---------- 回复 ----------
// replyTo 是「正在回复的那条顶层评论」；只支持一级回复，回复一律挂在顶层评论下
const replyTo = ref(null)
const replyText = ref('')
const replySubmitting = ref(false)

// ---------- 点赞 ----------
const likingIds = ref(new Set())

// ---------- 删除 ----------
const showDeleteConfirm = ref(false)
const deleteTarget = ref(null)

function requireLogin(action) {
  if (store.isLoggedIn) return true
  router.push({ name: 'login', query: { redirect: props.loginRedirect } })
  toast(`请先登录后再${action}`)
  return false
}

// 顶层评论与回复都在同一个数组/子数组里，按 id 找当前对象（点赞后要原地更新）
function findComment(id) {
  for (const c of comments.value) {
    if (c.id === id) return c
    const r = (c.replies || []).find(x => x.id === id)
    if (r) return r
  }
  return null
}

async function loadComments() {
  if (!commentsReady.value) return
  if (loading.value || !hasMore.value) return

  loading.value = true
  try {
    const res = await store.getComments(props.noteId, { page: page.value, pageSize: PAGE_SIZE })
    const list = res.list || []
    if (page.value === 1) {
      comments.value = list
    } else {
      comments.value.push(...list)
    }
    // 用 total 判「还有没有下一页」：比 list.length === PAGE_SIZE 准，
    // 否则顶层评论总数正好是 PAGE_SIZE 整数倍时会白空转一次。
    // total 缺失时退回「这一页有没有拿满」，免得后端没给 total 就只剩第一页。
    total.value = res.total ?? 0
    hasMore.value = res.total != null
      ? comments.value.length < res.total
      : list.length === PAGE_SIZE
    page.value++
  } catch (e) {
    if (isEndpointMissing(e)) {
      commentsReady.value = false
      hasMore.value = false
      return
    }
    toast(e.message || '加载评论失败', 'error')
  } finally {
    loading.value = false
  }
}

async function reloadFirstPage() {
  comments.value = []
  page.value = 1
  hasMore.value = true
  total.value = 0
  await loadComments()
  await fillContainer()
}

async function submitComment() {
  if (!commentsReady.value) return
  if (!requireLogin('评论')) return
  if (!newComment.value.trim()) {
    toast('请输入评论内容', 'warning')
    return
  }
  if (submitting.value) return

  submitting.value = true
  try {
    await store.createComment(props.noteId, newComment.value.trim())
    newComment.value = ''
    toast('评论成功')
    await reloadFirstPage()
  } catch (e) {
    if (isEndpointMissing(e)) { commentsReady.value = false; return }
    toast(e.message || '评论失败，请重试', 'error')
  } finally {
    submitting.value = false
  }
}

function startReply(comment) {
  if (!requireLogin('回复')) return
  replyTo.value = comment
  replyText.value = ''
}

function cancelReply() {
  replyTo.value = null
  replyText.value = ''
}

async function submitReply() {
  if (!replyTo.value) return
  if (!replyText.value.trim()) {
    toast('请输入回复内容', 'warning')
    return
  }
  if (replySubmitting.value) return

  replySubmitting.value = true
  try {
    const created = await store.createComment(props.noteId, replyText.value.trim(), replyTo.value.id)
    // 回复一律挂在顶层评论下：replyTo 就是那条顶层评论，直接推进它的 replies
    if (!replyTo.value.replies) replyTo.value.replies = []
    replyTo.value.replies.push(created)
    toast('回复成功')
    cancelReply()
  } catch (e) {
    if (isEndpointMissing(e)) { commentsReady.value = false; return }
    toast(e.message || '回复失败，请重试', 'error')
  } finally {
    replySubmitting.value = false
  }
}

async function toggleLike(comment) {
  if (!requireLogin('点赞')) return
  if (likingIds.value.has(comment.id)) return

  likingIds.value.add(comment.id)
  try {
    const res = await store.likeComment(props.noteId, comment.id, !comment.liked)
    // 原地更新，列表里其它地方引用的是同一个对象
    const target = findComment(comment.id) || comment
    target.likes = res.likes
    target.liked = res.liked
  } catch (e) {
    if (isEndpointMissing(e)) { commentsReady.value = false; return }
    toast(e.message || '操作失败，请重试', 'error')
  } finally {
    likingIds.value.delete(comment.id)
  }
}

function askDelete(comment) {
  deleteTarget.value = comment
  showDeleteConfirm.value = true
}

async function confirmDelete() {
  const target = deleteTarget.value
  if (!target) return
  try {
    await store.removeComment(props.noteId, target.id)
    toast('评论已删除')
    if (target.parentId == null) {
      comments.value = comments.value.filter(c => c.id !== target.id)
    } else {
      const parent = comments.value.find(c => c.id === target.parentId)
      if (parent) parent.replies = (parent.replies || []).filter(r => r.id !== target.id)
    }
    if (replyTo.value && replyTo.value.id === target.id) cancelReply()
  } catch (e) {
    if (isEndpointMissing(e)) { commentsReady.value = false; return }
    toast(e.message || '删除失败，请重试', 'error')
  } finally {
    showDeleteConfirm.value = false
    deleteTarget.value = null
  }
}

// ---------- 滚动分页 ----------

// 第一页撑不满容器时用户根本滚不动，也就永远触发不了加载 —— 主动补到填满为止
async function fillContainer() {
  for (let i = 0; i < 5; i++) {
    await nextTick()
    const el = listWrapEl.value
    if (!el || el.clientHeight === 0) return // 容器还没渲染出来
    if (!hasMore.value || loading.value) return
    if (el.scrollHeight > el.clientHeight) return // 已经能滚了，剩下的交给滚动触发
    const before = comments.value.length
    await loadComments()
    if (comments.value.length === before) return // 没拿到新数据（请求失败/已到底），别空转
  }
}

function setupObserver() {
  teardownObserver()
  if (!sentinelEl.value || !listWrapEl.value) return
  observer = new IntersectionObserver((entries) => {
    if (entries.some(e => e.isIntersecting) && hasMore.value && !loading.value) {
      loadComments()
    }
  }, {
    // root 必须显式指定成滚动容器：默认 root 是浏览器视口，
    // 而这里滚的是内层 div，哨兵相对视口始终可见 → 会一进页面就无限加载
    root: listWrapEl.value,
    rootMargin: '0px 0px 60px 0px'
  })
  observer.observe(sentinelEl.value)
}

function teardownObserver() {
  if (observer) {
    observer.disconnect()
    observer = null
  }
}

// 首次进入 / 切换笔记：状态全部复位后重新加载
async function init() {
  teardownObserver()
  comments.value = []
  page.value = 1
  hasMore.value = true
  total.value = 0
  commentsReady.value = true
  await loadComments()
  await fillContainer()
  if (listWrapEl.value) listWrapEl.value.scrollTop = 0
  setupObserver()
}

onMounted(init)
// 组件被复用时（noteId 变了）要重新来一遍，否则会显示上一条笔记的评论
watch(() => props.noteId, init)
onBeforeUnmount(teardownObserver)
</script>

<template>
  <div class="comment-panel">
    <!-- 评论输入框（接口未就绪时整块隐藏，给一行提示） -->
    <p v-if="!commentsReady" class="comment-disabled-hint">评论功能暂未开放</p>
    <div v-else class="comment-input-section">
      <div class="comment-input-wrap">
        <textarea
          v-model="newComment"
          class="comment-input"
          placeholder="写下你的评论..."
          rows="2"
          :disabled="submitting"
        ></textarea>
      </div>
      <button
        class="comment-send-btn gradient-btn"
        :disabled="!newComment.trim() || submitting"
        @click="submitComment"
      >
        {{ submitting ? '发送中...' : '发送' }}
      </button>
    </div>

    <!-- 评论列表：滚到底自动加载下一页 -->
    <div ref="listWrapEl" class="comment-list-wrap">
      <div v-if="!comments.length" class="comment-empty">暂无评论</div>

      <div v-else class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-thread">
          <!-- 顶层评论 -->
          <div class="comment-item">
            <div class="comment-item-avatar">{{ (comment.author || '?')[0] }}</div>
            <div class="comment-item-body">
              <div class="comment-item-header">
                <span class="comment-item-name">{{ comment.author }}</span>
                <span class="comment-item-time">{{ formatDateTime(comment.createdAt) }}</span>
              </div>
              <div class="comment-item-text">{{ comment.content }}</div>
              <div class="comment-item-actions">
                <button
                  class="comment-action like"
                  :class="{ on: comment.liked }"
                  :disabled="likingIds.has(comment.id)"
                  @click="toggleLike(comment)"
                >
                  <svg width="12" height="12" viewBox="0 0 24 24"
                       :style="{ fill: comment.liked ? 'var(--danger)' : 'none' }" stroke="currentColor" stroke-width="2">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                  </svg>
                  {{ comment.likes || 0 }}
                </button>
                <button class="comment-action" @click="startReply(comment)">回复</button>
                <button
                  v-if="comment.canDelete"
                  class="comment-action delete"
                  @click="askDelete(comment)"
                >删除</button>
              </div>
            </div>
          </div>

          <!-- 一级回复 -->
          <div v-if="comment.replies && comment.replies.length" class="reply-list">
            <div v-for="reply in comment.replies" :key="reply.id" class="comment-item reply-item">
              <div class="comment-item-avatar small">{{ (reply.author || '?')[0] }}</div>
              <div class="comment-item-body">
                <div class="comment-item-header">
                  <span class="comment-item-name">{{ reply.author }}</span>
                  <span class="comment-item-time">{{ formatDateTime(reply.createdAt) }}</span>
                </div>
                <div class="comment-item-text">{{ reply.content }}</div>
                <div class="comment-item-actions">
                  <button
                    class="comment-action like"
                    :class="{ on: reply.liked }"
                    :disabled="likingIds.has(reply.id)"
                    @click="toggleLike(reply)"
                  >
                    <svg width="12" height="12" viewBox="0 0 24 24"
                         :style="{ fill: reply.liked ? 'var(--danger)' : 'none' }" stroke="currentColor" stroke-width="2">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                    </svg>
                    {{ reply.likes || 0 }}
                  </button>
                  <button
                    v-if="reply.canDelete"
                    class="comment-action delete"
                    @click="askDelete(reply)"
                  >删除</button>
                </div>
              </div>
            </div>
          </div>

          <!-- 回复输入框：只对顶层评论开放（一级回复） -->
          <div v-if="replyTo && replyTo.id === comment.id" class="reply-input-section">
            <textarea
              v-model="replyText"
              class="comment-input"
              :placeholder="`回复 @${comment.author}`"
              rows="2"
              :disabled="replySubmitting"
            ></textarea>
            <div class="reply-input-actions">
              <button class="comment-action" @click="cancelReply">取消</button>
              <button
                class="comment-send-btn gradient-btn small"
                :disabled="!replyText.trim() || replySubmitting"
                @click="submitReply"
              >{{ replySubmitting ? '发送中...' : '回复' }}</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 滚动哨兵：滚到它就加载下一页。始终渲染，否则 observer 没东西可观察 -->
      <div ref="sentinelEl" class="load-more-sentinel">
        <span v-if="loading" class="load-more-text">加载中...</span>
        <span v-else-if="hasMore && commentsReady" class="load-more-text">上滑加载更多</span>
        <span v-else-if="comments.length" class="load-more-text muted">没有更多评论了</span>
      </div>
    </div>

    <!-- 删除评论确认 -->
    <AdminConfirmDialog
      :visible="showDeleteConfirm"
      danger
      title="删除评论"
      confirm-text="确认删除"
      :message="deleteTarget ? `确认删除这条评论吗？\n「${deleteTarget.content}」` : ''"
      @cancel="showDeleteConfirm = false"
      @confirm="confirmDelete"
    />
  </div>
</template>

<style scoped>
.comment-disabled-hint {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--bg-glass-border);
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
}

/* 输入区 */
.comment-input-section {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--bg-glass-border);
}

.comment-input-wrap { position: relative; }

.comment-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--dialog-border);
  border-radius: var(--radius-sm);
  background: var(--dialog-bg);
  color: var(--text-primary);
  font-size: 12px;
  line-height: 1.4;
  font-family: inherit;
  resize: vertical;
  transition: var(--transition);
  outline: none;
}
.comment-input:focus {
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(var(--accent-rgb), 0.15);
}
.comment-input::placeholder { color: var(--text-muted); }

.comment-send-btn {
  margin-top: 8px;
  padding: 6px 16px;
  border-radius: var(--radius-pill);
  background: var(--accent-gradient);
  color: white;
  font-size: 12px;
  font-weight: 600;
  width: 100%;
  transition: var(--transition);
}
.comment-send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(var(--accent-rgb), 0.4);
}
.comment-send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.comment-send-btn.small { width: auto; margin-top: 0; padding: 5px 14px; }

/* 列表 */
.comment-list-wrap {
  max-height: 420px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-thread {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-item {
  display: flex;
  gap: 8px;
  padding: 8px;
  background: rgba(var(--overlay-rgb), 0.04);
  border-radius: var(--radius-xs);
}

.comment-item-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--accent-gradient);
  color: white;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.comment-item-avatar.small { width: 20px; height: 20px; font-size: 10px; }

.comment-item-body { flex: 1; min-width: 0; }

.comment-item-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.comment-item-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent-primary);
}

.comment-item-time {
  font-size: 11px;
  color: var(--text-muted);
}

.comment-item-text {
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-primary);
  word-break: break-word;
  overflow-wrap: anywhere;
}

/* 操作行：点赞 / 回复 / 删除 */
.comment-item-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 5px;
}

.comment-action {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--text-muted);
  background: none;
  cursor: pointer;
  transition: var(--transition);
  padding: 0;
}
.comment-action:hover { color: var(--accent-primary); }
.comment-action.like.on { color: var(--danger); }
.comment-action.like:hover { color: var(--danger); }
.comment-action.delete:hover { color: var(--danger); }
.comment-action:disabled { opacity: 0.5; cursor: not-allowed; }

/* 回复列表：缩进显示在父评论下方 */
.reply-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-left: 16px;
  padding-left: 8px;
  border-left: 2px solid rgba(var(--accent-rgb), 0.2);
}

.reply-item { background: rgba(var(--overlay-rgb), 0.02); }

.reply-input-section {
  margin-left: 16px;
  padding-left: 8px;
  border-left: 2px solid rgba(var(--accent-rgb), 0.35);
}

.reply-input-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}

.comment-empty {
  text-align: center;
  padding: 16px 8px;
  color: var(--text-muted);
  font-size: 12px;
}

/* 滚动哨兵：高度不能为 0，否则容器刚好填满时会观察不到它就永远不加载 */
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
.load-more-text.muted { color: var(--text-muted); }
</style>
