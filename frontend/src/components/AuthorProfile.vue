<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useAppStore } from '../stores/app'
import { useToast } from '../composables/useToast'
import StatCard from './StatCard.vue'
import ChecklistDetail from './ChecklistDetail.vue'
import { formatLikes } from '../utils/format'

const props = defineProps({
  // 社区页用 v-show 保留本组件状态，所以没选中作者时它也是挂载着的（此时 authorId 为 null）
  authorId: { type: Number, default: null },
  backText: { type: String, default: '返回社区' }
})

const emit = defineEmits(['back', 'note-click'])

const store = useAppStore()
const { show: toast } = useToast()

const NOTE_PAGE_SIZE = 4
const PLAN_PAGE_SIZE = 4

const authorData = ref(null)

// 作者主页：笔记与规划都走后端真分页，页内只存当前页
const notePage = ref(1)
const planPage = ref(1)

const noteList = ref([])
const noteTotal = ref(0)
const planList = ref([])
const planTotal = ref(0)

// 规划详情（页内切换）
const selectedPlan = ref(null)

const noteTotalPages = computed(() => Math.max(1, Math.ceil(noteTotal.value / NOTE_PAGE_SIZE)))
const planTotalPages = computed(() => Math.max(1, Math.ceil(planTotal.value / PLAN_PAGE_SIZE)))

// 首次渲染时作者资料还没拉回来，这里必须给全字段：
// 少了 totalKm 会让 formatKm(undefined) 抛 toLocaleString 错，整个页面白屏
const EMPTY_PROFILE = {
  avatar: null, nickname: '', motto: '',
  visitedCities: 0, visitedProvinces: 0, totalKm: 0, totalDays: 0
}

const profileUser = computed(() => authorData.value?.user || EMPTY_PROFILE)

function formatKm(n) {
  return (n ?? 0).toLocaleString()
}

async function loadAuthor() {
  if (props.authorId == null) return
  try {
    authorData.value = null
    authorData.value = await store.loadAuthorProfile(props.authorId)
  } catch (e) {
    toast(e.message || '加载作者信息失败', 'error')
  }
}

async function loadNotes() {
  if (props.authorId == null) return
  try {
    const res = await store.fetchNotes({
      authorId: props.authorId,
      page: notePage.value,
      pageSize: NOTE_PAGE_SIZE
    })
    noteList.value = res.list
    noteTotal.value = res.total
  } catch (e) {
    toast(e.message || '加载笔记失败', 'error')
  }
}

async function loadPlans() {
  if (props.authorId == null) return
  try {
    const res = await store.fetchAuthorPlans(props.authorId, {
      page: planPage.value,
      pageSize: PLAN_PAGE_SIZE
    })
    planList.value = res.list
    planTotal.value = res.total
  } catch (e) {
    toast(e.message || '加载旅游规划失败', 'error')
  }
}

function goToNotePage(p) {
  if (p < 1 || p > noteTotalPages.value) return
  notePage.value = p
  loadNotes()
}

function goToPlanPage(p) {
  if (p < 1 || p > planTotalPages.value) return
  planPage.value = p
  loadPlans()
}

function handlePlanClick(plan) {
  selectedPlan.value = plan
}

function handlePlanBack() {
  selectedPlan.value = null
}

// 复制这份公开规划到我的旅游规划（后端会把副本置为私有）
async function handleUsePlan() {
  const plan = selectedPlan.value
  if (!plan) return
  if (!store.isLoggedIn) {
    toast('请先登录后再使用这份规划')
    return
  }
  try {
    await store.copyAuthorPlan(plan)
    toast('已复制到「我的旅游规划」')
  } catch (e) {
    toast(e.message || '复制失败，请重试', 'error')
  }
}

watch(() => props.authorId, (id) => {
  // 返回社区时 authorId 会被置空，此时保留现有数据即可（组件只是被隐藏）
  if (id == null) return
  notePage.value = 1
  planPage.value = 1
  selectedPlan.value = null
  loadAuthor()
  loadNotes()
  loadPlans()
})

onMounted(() => {
  loadAuthor()
  loadNotes()
  loadPlans()
})
</script>

<template>
  <div class="author-profile">
    <!-- 规划详情（页内）：用 ChecklistDetail 自带的顶栏，才有「使用这份规划」按钮 -->
    <template v-if="selectedPlan">
      <div class="plan-detail-wrap glass-panel">
        <ChecklistDetail
          :item="selectedPlan"
          use-action
          back-text="返回主页"
          @back="handlePlanBack"
          @use="handleUsePlan"
        />
      </div>
    </template>

    <template v-else>
      <!-- 顶部作者信息 -->
      <div class="profile-section">
        <div class="back-bar">
          <button class="back-btn" @click="emit('back')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
            {{ backText }}
          </button>
        </div>
        <div class="profile-header">
          <div class="avatar-wrapper">
            <div class="avatar">
              <img :src="profileUser.avatar" :alt="profileUser.nickname" />
            </div>
            <div class="avatar-ring"></div>
          </div>
          <div class="profile-info">
            <h1 class="nickname">{{ profileUser.nickname }}</h1>
            <p class="motto">{{ profileUser.motto }}</p>
          </div>
        </div>
      </div>

      <!-- TA的旅行数据 -->
      <section class="section">
        <h2 class="section-title">TA的旅行数据</h2>
        <div class="stats-grid">
          <StatCard label="已点亮城市" :value="profileUser.visitedCities" />
          <StatCard label="累计省份" :value="profileUser.visitedProvinces" />
          <StatCard label="飞行里程" :value="formatKm(profileUser.totalKm)" unit="km" />
          <StatCard label="旅行天数" :value="profileUser.totalDays" />
        </div>
      </section>

      <!-- TA的笔记 -->
      <section class="section">
        <div class="section-header">
          <h2 class="section-title">TA的笔记</h2>
        </div>
        <div v-if="noteTotal" class="notes-list">
          <div
            v-for="note in noteList"
            :key="note.id"
            class="note-card glass-panel"
            @click="emit('note-click', note)"
          >
            <div v-if="note.image" class="note-card-image">
              <img :src="note.image" alt="" />
            </div>
            <div v-else class="note-card-placeholder">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke-width="1.5" style="stroke: var(--text-muted); opacity: 0.5">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                <circle cx="8.5" cy="8.5" r="1.5"/>
                <path d="M21 15l-5-5L5 21"/>
              </svg>
            </div>
            <div class="note-card-body">
              <h4 class="note-card-title">{{ note.title }}</h4>
              <div class="note-card-meta">
                <span v-if="note.location" class="note-card-location">📍 {{ note.location.name }}</span>
                <!-- 和社区卡片保持一致：点赞 + 评论数都显示 -->
                <span class="note-card-stats">
                  <span class="note-card-likes" :class="{ liked: note.liked }">
                    <svg class="stat-icon" width="12" height="12" viewBox="0 0 24 24" stroke="none">
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                    </svg>
                    {{ formatLikes(note.likes) }}
                  </span>
                  <span class="note-card-comments">
                    <svg class="stat-icon" width="12" height="12" viewBox="0 0 24 24" stroke="none">
                      <path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/>
                    </svg>
                    {{ formatLikes(note.commentCount) }}
                  </span>
                </span>
              </div>
            </div>
          </div>
        </div>
        <div v-if="noteTotalPages > 1" class="my-pagination">
          <button class="page-btn" :disabled="notePage <= 1" @click="goToNotePage(notePage - 1)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
          <button v-for="p in noteTotalPages" :key="p" class="page-num" :class="{ active: p === notePage }" @click="goToNotePage(p)">{{ p }}</button>
          <button class="page-btn" :disabled="notePage >= noteTotalPages" @click="goToNotePage(notePage + 1)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        </div>
        <div v-if="!noteTotal" class="empty-notes glass-panel">
          <p>暂无笔记</p>
        </div>
      </section>

      <!-- TA的旅游规划 -->
      <section class="section">
        <div class="section-header">
          <h2 class="section-title">TA的旅游规划</h2>
        </div>
        <div v-if="planList.length" class="plans-list">
          <div
            v-for="plan in planList"
            :key="plan.id"
            class="plan-card glass-panel"
            @click="handlePlanClick(plan)"
          >
            <div class="plan-card-top">
              <span class="plan-emoji">✈️</span>
              <div class="plan-info">
                <h4 class="plan-name">{{ plan.name }}</h4>
                <span class="plan-dest">📍 {{ plan.destination }}</span>
              </div>
            </div>
            <div class="plan-meta">
              <span class="plan-meta-item">📅 {{ plan.startDate || '未设定' }}</span>
              <span class="plan-meta-item">{{ plan.days?.length || 0 }} 天</span>
              <span class="plan-meta-item">{{ (plan.days || []).reduce((s, d) => s + (d.items?.length || 0), 0) }} 个行程</span>
            </div>
            <div v-if="plan.waypoints?.length" class="plan-waypoints">
              <span v-for="(wp, i) in plan.waypoints.slice(0, 3)" :key="i" class="plan-wp-pill">
                📍 {{ wp.name }}
              </span>
              <span v-if="plan.waypoints.length > 3" class="plan-wp-pill more">
                +{{ plan.waypoints.length - 3 }}
              </span>
            </div>
          </div>
        </div>
        <div v-if="planTotalPages > 1" class="my-pagination">
          <button class="page-btn" :disabled="planPage <= 1" @click="goToPlanPage(planPage - 1)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
          <button v-for="p in planTotalPages" :key="p" class="page-num" :class="{ active: p === planPage }" @click="goToPlanPage(p)">{{ p }}</button>
          <button class="page-btn" :disabled="planPage >= planTotalPages" @click="goToPlanPage(planPage + 1)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        </div>
        <div v-if="!planList.length" class="empty-notes glass-panel">
          <p>暂无旅游规划</p>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
/* 版面与「我的」页面的他人模式保持一致（MyPage.vue 里的对应样式） */
.author-profile {
  padding: 40px;
  display: flex;
  flex-direction: column;
  gap: 40px;
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

/* Profile Section */
.profile-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.back-bar {
  display: flex;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  flex-shrink: 0;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.3), rgba(14, 165, 233, 0.3));
  overflow: hidden;
  border: 2px solid rgba(var(--overlay-rgb), 0.15);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid transparent;
  border-top-color: var(--accent-blue);
  border-right-color: var(--accent-purple);
  animation: spin 6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nickname {
  font-size: 26px;
  font-weight: 800;
}

.motto {
  font-size: 14px;
  color: var(--text-muted);
}

/* Sections */
.section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

/* Notes List */
.notes-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.note-card {
  display: flex;
  gap: 14px;
  border-radius: var(--radius);
  overflow: hidden;
  transition: var(--transition);
  cursor: pointer;
  width: calc(50% - 8px);
  min-width: 300px;
  /* 固定高度：否则有封面的卡片被图片原始比例撑高、没封面的只剩 32px 图标，
     同一行靠 align-items:stretch 能拉齐、行与行之间却参差不齐 */
  height: 104px;
}
.note-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow);
}

.note-card-image,
.note-card-placeholder {
  height: 100%;
}

.note-card-image {
  width: 120px;
  flex-shrink: 0;
  overflow: hidden;
}
.note-card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.note-card-placeholder {
  width: 120px;
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.12), rgba(14, 165, 233, 0.12));
  display: flex;
  align-items: center;
  justify-content: center;
}

.note-card-body {
  flex: 1;
  padding: 14px 14px 14px 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.note-card-title {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.note-card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}

.note-card-location {
  font-size: 12px;
  color: var(--accent-blue);
}

/* 点赞 + 评论并排，跟社区卡片的 .note-stats 一个样式 */
.note-card-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.note-card-likes,
.note-card-comments {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

/* 图标颜色跟随主题（原来写死 rgba(var(--overlay-rgb), 0.4)，浅色主题下不可见）。
   SVG 的 fill 属性不认 var()，只能这样用 CSS 规则设 */
.note-card-likes .stat-icon,
.note-card-comments .stat-icon { fill: var(--text-muted); }
.note-card-likes.liked .stat-icon { fill: #f87171; }

.empty-notes {
  padding: 40px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
  border-radius: var(--radius);
}

/* Plan Cards */
.plan-detail-wrap {
  border-radius: var(--radius);
  padding: 24px;
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
}

.plans-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.plan-card {
  padding: 18px;
  border-radius: var(--radius);
  cursor: pointer;
  transition: var(--transition);
}
.plan-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow);
}

.plan-card-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.plan-emoji {
  font-size: 28px;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-xs);
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.15), rgba(14, 165, 233, 0.15));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.plan-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.plan-name {
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.plan-dest {
  font-size: 12px;
  color: var(--accent-blue);
}

.plan-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
}

.plan-meta-item {
  font-size: 12px;
  color: var(--text-muted);
}

.plan-waypoints {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.plan-wp-pill {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  background: rgba(56, 189, 248, 0.1);
  border: 1px solid rgba(56, 189, 248, 0.25);
  color: #a5b4fc;
}

/* Pagination */
.my-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 20px 0 8px;
}

.my-pagination .page-btn,
.my-pagination .page-num {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-xs);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  transition: var(--transition);
}

.my-pagination .page-btn {
  background: rgba(var(--overlay-rgb), 0.06);
  color: var(--text-secondary);
}
.my-pagination .page-btn:hover:not(:disabled) {
  background: rgba(var(--overlay-rgb), 0.1);
}
.my-pagination .page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.my-pagination .page-num {
  background: rgba(var(--overlay-rgb), 0.04);
  color: var(--text-muted);
}
.my-pagination .page-num:hover {
  background: rgba(var(--overlay-rgb), 0.08);
  color: var(--text-primary);
}
.my-pagination .page-num.active {
  background: var(--accent-gradient);
  color: white;
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .notes-list .note-card {
    width: 100%;
    min-width: 0;
  }
  .plans-list {
    grid-template-columns: 1fr;
  }
}
</style>
