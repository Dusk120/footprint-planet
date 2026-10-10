<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useAppStore } from '../stores/app'
import { useRouter } from 'vue-router'
import StatCard from '../components/StatCard.vue'
import EditProfileDialog from '../components/EditProfileDialog.vue'
import ChecklistDetail from '../components/ChecklistDetail.vue'
import AddChecklistDialog from '../components/AddChecklistDialog.vue'
import NoteDetailPanel from '../components/NoteDetailPanel.vue'
import AdminConfirmDialog from '../components/admin/AdminConfirmDialog.vue'
import { useToast } from '../composables/useToast'
import { formatLikes } from '../utils/format'

const store = useAppStore()
const router = useRouter()
const { show: toast } = useToast()
const showEditProfile = ref(false)
const selectedNote = ref(null)
const selectedPlan = ref(null)
const showPlanDialog = ref(false)
const editPlanTarget = ref(null)
const planDelConfirm = ref(false)
const NOTE_PAGE_SIZE = 4
const PLAN_PAGE_SIZE = 4
const notePage = ref(1)
const planPage = ref(1)

// 本页只显示「我」自己的数据；看别人的主页是社区页里的 AuthorProfile 组件
const profileUser = computed(() => store.user)

// 笔记：走后端真分页，只存当前页
const noteList = ref([])
const noteTotal = ref(0)

async function loadNotes() {
  // 冷启动（刷新页面）时 /auth/me 可能还没回来，user.id 尚未回填，先等一次初始化
  if (!store.user?.id) {
    try { await store.ensureInit() } catch (e) { /* bootstrap 内部已兜底登出 */ }
  }
  const uid = store.user?.id
  if (!uid) return
  try {
    const res = await store.fetchNotes({
      authorId: uid,
      page: notePage.value,
      pageSize: NOTE_PAGE_SIZE
    })
    noteList.value = res.list
    noteTotal.value = res.total
  } catch (e) {
    toast(e.message || '加载笔记失败', 'error')
  }
}

// 规划：走后端真分页
const planList = ref([])
const planTotal = ref(0)

async function loadPlans() {
  try {
    const res = await store.fetchPlans({ page: planPage.value, pageSize: PLAN_PAGE_SIZE })
    planList.value = res.list
    planTotal.value = res.total
  } catch (e) {
    toast(e.message || '加载旅游规划失败', 'error')
  }
}

watch(notePage, loadNotes)
watch(planPage, loadPlans)

onMounted(() => {
  loadNotes()
  loadPlans()
})

const noteTotalPages = computed(() => Math.max(1, Math.ceil(noteTotal.value / NOTE_PAGE_SIZE)))
const planTotalPages = computed(() => Math.max(1, Math.ceil(planTotal.value / PLAN_PAGE_SIZE)))

function formatKm(n) {
  return (n ?? 0).toLocaleString()
}

function formatDate(d) {
  if (!d) return '未设定'
  return d
}

function handleNoteClick(note) {
  selectedNote.value = note
}

function handleBack() {
  selectedNote.value = null
}

async function handleDeleteNote() {
  if (!selectedNote.value) return
  try {
    await store.removeNote(selectedNote.value.id)
    selectedNote.value = null
    // 删掉的是本页最后一条且不在首页时回退一页，watch 会重新拉取
    if (noteList.value.length === 1 && notePage.value > 1) notePage.value--
    else await loadNotes()
    toast('笔记已删除')
  } catch (e) {
    toast(e.message || '删除失败', 'error')
  }
}

async function handleLikeNote() {
  const note = selectedNote.value
  if (!note) return
  try {
    const res = await store.likeNote(note.id, !note.liked)
    note.likes = res.likes
    note.liked = res.liked
  } catch (e) {
    toast(e.message || '操作失败', 'error')
  }
}

// 旅游规划
function handlePlanClick(plan) {
  selectedPlan.value = plan
}

function handlePlanBack() {
  selectedPlan.value = null
}

function handlePlanEdit() {
  if (!selectedPlan.value) return
  editPlanTarget.value = { ...selectedPlan.value }
  showPlanDialog.value = true
}

async function handlePlanDelete() {
  if (!selectedPlan.value) return
  planDelConfirm.value = true
}

async function confirmPlanDelete() {
  const plan = selectedPlan.value
  if (!plan) return
  try {
    await store.removePlan(plan.id)
    selectedPlan.value = null
    planDelConfirm.value = false
    // 删掉的是本页最后一条且不在首页时回退一页，watch 会重新拉取
    if (planList.value.length === 1 && planPage.value > 1) planPage.value--
    else await loadPlans()
    toast('规划已删除')
  } catch (e) {
    planDelConfirm.value = false
    toast(e.message || '删除失败', 'error')
  }
}

async function handlePlanDialogConfirm(data) {
  try {
    if (editPlanTarget.value) {
      await store.updatePlan(editPlanTarget.value.id, data)
      toast('规划已更新')
      await loadPlans()
      selectedPlan.value = planList.value.find(w => w.id === editPlanTarget.value.id) || null
    }
  } catch (e) {
    toast(e.message || '保存失败', 'error')
  }
  showPlanDialog.value = false
  editPlanTarget.value = null
}
</script>

<template>
  <div class="my-page">
    <!-- 笔记详情视图 -->
    <NoteDetailPanel
      v-if="selectedNote"
      :note="selectedNote"
      back-text="返回主页"
      :can-delete="true"
      login-redirect="/my"
      @back="handleBack"
      @delete="handleDeleteNote"
      @like="handleLikeNote"
    />

    <!-- 规划详情视图 -->
    <template v-else-if="selectedPlan">
      <div class="detail-topbar">
        <button class="back-btn" @click="handlePlanBack">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
          返回主页
        </button>
        <div style="display:flex;gap:8px;">
          <button class="delete-note-btn" @click="handlePlanEdit">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            编辑
          </button>
          <button class="delete-note-btn" @click="handlePlanDelete">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            删除
          </button>
        </div>
      </div>
      <div class="plan-detail-wrap glass-panel">
        <ChecklistDetail :item="selectedPlan" :hide-topbar="true" @edit="handlePlanEdit" @delete="handlePlanDelete" @back="handlePlanBack" />
      </div>
    </template>

    <!-- 主页视图 -->
    <template v-else>
    <div class="profile-scroll">
    <!-- 顶部用户信息 -->
    <div class="profile-section">
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
          <div class="profile-actions">
            <button class="edit-btn" @click="showEditProfile = true">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              编辑资料
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 旅行数据 -->
    <section class="section">
      <h2 class="section-title">我的旅行数据</h2>
      <div class="stats-grid">
        <StatCard label="已点亮城市" :value="profileUser.visitedCities" />
        <StatCard label="累计省份" :value="profileUser.visitedProvinces" />
        <StatCard label="飞行里程" :value="formatKm(profileUser.totalKm)" unit="km" />
        <StatCard label="旅行天数" :value="profileUser.totalDays" />
      </div>
    </section>

    <!-- 我的笔记 -->
    <section class="section">
      <div class="section-header">
        <h2 class="section-title">我的笔记</h2>
        <button class="view-all-btn" @click="router.push('/community')">去社区发布 →</button>
      </div>
      <div v-if="noteTotal" class="notes-list">
        <div
          v-for="note in noteList"
          :key="note.id"
          class="note-card glass-panel"
          @click="handleNoteClick(note)"
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
        <button class="page-btn" :disabled="notePage <= 1" @click="notePage--">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
        <button v-for="p in noteTotalPages" :key="p" class="page-num" :class="{ active: p === notePage }" @click="notePage = p">{{ p }}</button>
        <button class="page-btn" :disabled="notePage >= noteTotalPages" @click="notePage++">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
        </button>
      </div>
      <div v-if="!noteTotal" class="empty-notes glass-panel">
        <p>还没有发布过笔记</p>
        <button class="empty-btn" @click="router.push('/community')">去社区看看</button>
      </div>
    </section>

    <!-- 我的旅游规划 -->
    <section class="section">
      <div class="section-header">
        <h2 class="section-title">我的旅游规划</h2>
        <button class="view-all-btn" @click="router.push('/checklist')">去规划旅行 →</button>
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
        <button class="page-btn" :disabled="planPage <= 1" @click="planPage--">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
        <button v-for="p in planTotalPages" :key="p" class="page-num" :class="{ active: p === planPage }" @click="planPage = p">{{ p }}</button>
        <button class="page-btn" :disabled="planPage >= planTotalPages" @click="planPage++">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
        </button>
      </div>
      <div v-if="!planList.length" class="empty-notes glass-panel">
        <p>还没有旅游规划</p>
        <button class="empty-btn" @click="router.push('/checklist')">去规划旅行</button>
      </div>
    </section>

    <EditProfileDialog v-if="showEditProfile" @cancel="showEditProfile = false" />
    <AddChecklistDialog
      v-if="showPlanDialog"
      :edit-data="editPlanTarget"
      @confirm="handlePlanDialogConfirm"
      @cancel="showPlanDialog = false; editPlanTarget = null"
    />

    <AdminConfirmDialog
      :visible="planDelConfirm"
      title="删除旅游规划"
      :message="`确定要删除「${selectedPlan?.name || '这份规划'}」吗？删除后不可恢复。`"
      confirm-text="删除"
      danger
      @confirm="confirmPlanDelete"
      @cancel="planDelConfirm = false"
    />
    </div>
    </template>
  </div>
</template>

<style scoped>
.my-page {
  padding: 40px;
  display: flex;
  flex-direction: column;
  gap: 40px;
  height: 100%;
  overflow: hidden;
}

.profile-scroll {
  display: flex;
  flex-direction: column;
  gap: 40px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

/* Detail View */
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
  background: none;
  color: var(--text-muted);
  font-size: 13px;
  padding: 8px 12px;
  border-radius: var(--radius-xs);
  transition: var(--transition);
}
.delete-note-btn:hover {
  background: rgba(248, 113, 113, 0.1);
  color: #f87171;
}

/* Profile Section */
.profile-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.profile-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.edit-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 14px;
  border-radius: var(--radius-pill);
  background: var(--bg-card);
  border: 1px solid var(--bg-glass-border);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  transition: var(--transition);
}
.edit-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
  border-color: var(--accent-blue);
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

.view-all-btn {
  background: none;
  color: var(--accent-blue);
  font-size: 13px;
  font-weight: 600;
  transition: var(--transition);
}
.view-all-btn:hover { opacity: 0.8; }

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

.note-card-image,
.note-card-placeholder {
  height: 100%;
}
.note-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow);
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

.note-card-content {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  overflow-wrap: anywhere;
  max-height: 42px;
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

.empty-btn {
  margin-top: 12px;
  padding: 8px 20px;
  border-radius: var(--radius-pill);
  background: var(--accent-gradient);
  color: white;
  font-size: 13px;
  font-weight: 600;
  transition: var(--transition);
}
.empty-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(56, 189, 248, 0.4);
}

/* Plan Cards */
.plan-detail-wrap {
  flex: 1;
  border-radius: var(--radius);
  padding: 24px;
  min-height: 0;
  overflow-y: auto;
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
  .profile-section {
    flex-direction: column;
  }
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
