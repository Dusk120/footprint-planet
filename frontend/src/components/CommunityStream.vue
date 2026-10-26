<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useAppStore } from '../stores/app'
import { useRouter } from 'vue-router'
import TravelNoteCard from './TravelNoteCard.vue'
import { useToast } from '../composables/useToast'
import PublishNoteDialog from './PublishNoteDialog.vue'

const store = useAppStore()
const router = useRouter()
const { show: toast } = useToast()
// 笔记详情与作者主页都由 CommunityPage 负责切视图，这里只把点击事件抛上去
const emit = defineEmits(['note-click'])

const searchKeyword = ref('')
const showPublish = ref(false)
const NOTE_PAGE_SIZE = 4
const notePage = ref(1)
const noteSortBy = ref('latest')

// 社区笔记流：真·后端分页
const communityNotes = ref([])
const noteTotal = ref(0)

const noteTotalPages = computed(() => Math.max(1, Math.ceil(noteTotal.value / NOTE_PAGE_SIZE)))

// 计算要显示的页码。数组里会出现字符串 '...'（代表被省略的区间），
// 渲染时必须和数字分开处理 —— 否则省略号会被当成一个可点的页码。
const displayedPages = computed(() => {
  const total = noteTotalPages.value
  const cur = notePage.value

  // 页数少就全列出来，不出现省略号
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)

  const pages = [1]
  const from = Math.max(2, cur - 1)
  const to = Math.min(total - 1, cur + 1)
  if (from > 2) pages.push('...')
  for (let i = from; i <= to; i++) pages.push(i)
  if (to < total - 1) pages.push('...')
  pages.push(total)
  return pages
})

async function refreshNotes() {
  try {
    const res = await store.fetchNotes({
      sort: noteSortBy.value,
      keyword: searchKeyword.value.trim(),
      page: notePage.value,
      pageSize: NOTE_PAGE_SIZE
    })
    communityNotes.value = res.list
    noteTotal.value = res.total
  } catch (e) {
    toast(e.message || '加载失败，请重试', 'error')
  }
}

function goToNotePage(p) {
  // 兜一层：非数字、越界、或点的就是当前页，都不发请求
  if (typeof p !== 'number' || p < 1 || p > noteTotalPages.value || p === notePage.value) return
  notePage.value = p
  refreshNotes()
}

watch(searchKeyword, () => {
  notePage.value = 1
})

// 切换排序：回到第 1 页并按新排序重拉
watch(noteSortBy, () => {
  notePage.value = 1
  refreshNotes()
})

// 组件挂载时加载笔记和热门数据
onMounted(() => {
  refreshNotes()
  store.loadHot()
})

function requireAuth(action) {
  if (!store.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: '/community' } })
    toast(`请先登录后再${action}`)
    return false
  }
  return true
}

function handlePublish() {
  if (requireAuth('发布笔记')) {
    showPublish.value = true
  }
}

async function handlePublishConfirm(payload) {
  try {
    await store.publishNote(payload)
    showPublish.value = false
    toast('发布成功')
    notePage.value = 1
    refreshNotes()
  } catch (e) {
    toast(e.message || '发布失败，请重试', 'error')
  }
}

function handleSearch() {
  notePage.value = 1
  refreshNotes()
}

// 清空输入并立即按「无关键词」重查一次
function handleClearSearch() {
  searchKeyword.value = ''
  handleSearch()
}

// 处理笔记点击
function handleNoteClick(note) {
  emit('note-click', note)
}

async function handleLike(note) {
  if (!requireAuth('点赞')) return
  try {
    const res = await store.likeNote(note.id, !isNoteLiked(note))
    // 共享列表可能不含该条（搜索态），同步更新页内列表的点赞状态
    const local = communityNotes.value.find(n => n.id === note.id)
    if (local) {
      local.likes = res.likes
      local.liked = res.liked
    }
  } catch (e) {
    toast(e.message || '操作失败，请重试', 'error')
  }
}

// note.liked 由后端按当前登录用户算好，前端不再比对用户名
function isNoteLiked(note) {
  return !!note.liked
}
</script>

<template>
  <div class="community-stream">
    <!-- 顶部导航 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <span class="topbar-logo">旅游社区</span>
      </div>
      <div class="topbar-right">
        <button
          class="publish-btn gradient-btn"
          @click="handlePublish"
        >
          发布笔记
        </button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-section">
      <div class="search-bar">
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="搜索标题、内容、作者、地点..."
          @keyup.enter="handleSearch"
        />
        <button class="search-btn" @click="handleSearch">
          搜索
        </button>
        <button
          v-if="searchKeyword"
          class="clear-btn"
          @click="handleClearSearch"
        >
          ×
        </button>
      </div>

      <div class="filter-section">
        <div class="sort-tabs">
          <button
            :class="{ active: noteSortBy === 'latest' }"
            @click="noteSortBy = 'latest'"
          >
            最新
          </button>
          <button
            :class="{ active: noteSortBy === 'hot' }"
            @click="noteSortBy = 'hot'"
          >
            最热
          </button>
        </div>
      </div>
    </div>

    <!-- 主要内容区域：左边笔记，右边排行榜 -->
    <div class="main-content">
      <!-- 左侧笔记列表 -->
      <div class="notes-container">
        <div class="notes-grid">
          <div
            v-for="note in communityNotes"
            :key="note.id"
            class="note-card"
          >
            <TravelNoteCard
              :note="note"
              :liked="note.liked"
              @like="handleLike(note)"
              @click="emit('note-click', note)"
            />
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="noteTotalPages > 1" class="pagination">
          <button
            class="page-btn"
            :disabled="notePage <= 1"
            @click="goToNotePage(notePage - 1)"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 18l-6-6 6-6"/>
            </svg>
          </button>

          <div class="page-numbers">
            <!-- 用下标做 key：省略号可能出现两次，拿它当 key 会重复 -->
            <template v-for="(p, i) in displayedPages" :key="i">
              <!-- 省略号只是视觉分隔，绝不能是按钮 —— 做成按钮就会被当成页码去请求 ?page=... -->
              <span v-if="p === '...'" class="page-gap">…</span>
              <button
                v-else
                class="page-num"
                :class="{ active: p === notePage }"
                @click="goToNotePage(p)"
              >
                {{ p }}
              </button>
            </template>
          </div>

          <button
            class="page-btn"
            :disabled="notePage >= noteTotalPages"
            @click="goToNotePage(notePage + 1)"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 18l6-6-6-6"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 右侧热门城市排行榜 -->
      <div class="sidebar">
        <div class="hot-section">
          <h3 class="hot-title">🔥 热门打卡城市</h3>
          <div class="hot-cities">
            <div
              v-for="(city, index) in store.hotDestinations"
              :key="city.name"
              class="hot-city"
            >
              <span class="hot-rank">{{ index + 1 }}</span>
              <span class="hot-name">{{ city.name }}</span>
              <span class="hot-count">{{ city.count }} 人打卡</span>
            </div>
            <div v-if="!store.hotDestinations.length" class="hot-empty">
              暂无热门城市数据
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 发布笔记对话框 -->
    <PublishNoteDialog
      v-if="showPublish"
      @cancel="showPublish = false"
      @confirm="handlePublishConfirm"
    />

  </div>
</template>

<style scoped>
.community-stream {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.topbar-logo {
  font-size: 24px;
  font-weight: 700;
  background: var(--accent-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.publish-btn {
  padding: 8px 24px;
  font-size: 14px;
}

.search-section {
  margin-bottom: 32px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
  padding: 12px 20px;
  border: 1px solid var(--dialog-border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 14px;
  transition: var(--transition);
}

.search-input:focus {
  outline: none;
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(var(--accent-rgb), 0.15);
}

.search-btn, .clear-btn {
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  cursor: pointer;
  transition: var(--transition);
}

.search-btn {
  background: var(--accent-gradient);
  color: white;
  border: none;
}

.search-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(var(--accent-rgb), 0.4);
}

.clear-btn {
  background: var(--bg-card);
  color: var(--text-secondary);
  border: 1px solid var(--dialog-border);
}

.clear-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.filter-section {
  display: flex;
  justify-content: flex-end;
}

.sort-tabs {
  display: flex;
  gap: 8px;
}

.sort-tabs button {
  padding: 6px 16px;
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: var(--transition);
}

.sort-tabs button:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.sort-tabs button.active {
  background: var(--accent-gradient);
  color: white;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.note-card {
  height: 420px;
  overflow: hidden;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 32px;
}

.page-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--bg-card);
  border: 1px solid var(--dialog-border);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: var(--accent-gradient);
  color: white;
  border-color: transparent;
  transform: translateY(-1px);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.page-numbers {
  display: flex;
  gap: 6px;
}

/* 省略号是分隔符不是控件，所以不做成按钮的样子 */
.page-gap {
  min-width: 24px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 14px;
  user-select: none;
}

.page-num {
  min-width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--bg-card);
  border: 1px solid var(--dialog-border);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.page-num:hover {
  background: var(--bg-card-hover);
  border-color: var(--accent-primary);
  color: var(--text-primary);
}

.page-num.active {
  background: var(--accent-gradient);
  color: white;
  border-color: transparent;
  font-weight: 600;
}

.page-num:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: var(--bg-card);
  color: var(--text-secondary);
}

/* 主要内容区域布局 */
.main-content {
  display: flex;
  gap: 24px;
  margin-top: 32px;
}

/* 左侧笔记容器 */
.notes-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 右侧边栏 */
.sidebar {
  width: 300px;
  position: sticky;
  top: 20px;
  height: fit-content;
}

/* 热门城市样式调整 */
.hot-section {
  padding: 20px;
  background: var(--bg-card);
  border-radius: var(--radius);
}

.hot-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.hot-cities {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.hot-city {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: var(--bg-card-hover);
  border-radius: var(--radius-sm);
  transition: var(--transition);
}

.hot-city:hover {
  background: var(--bg-card);
  transform: translateY(-1px);
}

.hot-rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--accent-gradient);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.hot-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.hot-count {
  font-size: 12px;
  color: var(--text-secondary);
}

.hot-empty {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
  font-size: 14px;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    position: static;
  }
}

</style>