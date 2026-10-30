<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'
import { useToast } from '../composables/useToast'
import AddChecklistDialog from '../components/AddChecklistDialog.vue'
import AdminConfirmDialog from '../components/admin/AdminConfirmDialog.vue'

const store = useAppStore()
const router = useRouter()
const { show: toast } = useToast()

const showDialog = ref(false)
const editTarget = ref(null)
const deleteTarget = ref(null)
const plans = ref([])
const loading = ref(false)
// 封面图加载失败（无效 URL 等）时记录，兜底到渐变占位
const imgError = reactive(new Set())
function onImgError(id) {
  imgError.add(id)
}

// ---------- 分页（3 列 × 2 行 = 6 个一页） ----------
const PAGE_SIZE = 6
const page = ref(1)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))

async function loadPlans() {
  loading.value = true
  try {
    const res = await store.fetchPlans({ page: page.value, pageSize: PAGE_SIZE })
    plans.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    toast(e.message || '加载旅游规划失败', 'error')
  } finally {
    loading.value = false
  }
}

function goPage(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return
  page.value = p
  loadPlans()
}

onMounted(loadPlans)

function handleAdd() {
  editTarget.value = null
  showDialog.value = true
}

function handleEdit(p) {
  editTarget.value = { ...p }
  showDialog.value = true
}

async function handleDialogConfirm(data) {
  try {
    if (editTarget.value) {
      await store.updatePlan(editTarget.value.id, data)
      toast('规划已更新')
    } else {
      await store.addPlan(data)
      toast('规划已创建')
    }
    showDialog.value = false
    editTarget.value = null
    await loadPlans()
  } catch (e) {
    toast(e.message || '保存失败，请重试', 'error')
  }
}

function openEditor(id) {
  router.push(`/checklist/${id}`)
}

function handleDelete(p) {
  deleteTarget.value = p
}

async function confirmDelete() {
  const p = deleteTarget.value
  if (!p) return
  try {
    await store.removePlan(p.id)
    deleteTarget.value = null
    toast('规划已删除')
    // 当前页删空则回退一页
    if (plans.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadPlans()
  } catch (e) {
    deleteTarget.value = null
    toast(e.message || '删除失败，请重试', 'error')
  }
}

function formatDate(d) {
  return d || '未设定'
}
</script>

<template>
  <div class="plan-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">旅游规划</h1>
        <p class="page-sub">地图驱动行程，多人协同创作</p>
      </div>
      <button class="add-btn gradient-btn" @click="handleAdd">＋ 新建规划</button>
    </div>

    <div v-if="loading" class="loading">加载中…</div>

    <div v-else-if="!plans.length" class="empty-state">
      <div class="empty-icon">🗺️</div>
      <p class="empty-text">还没有旅游规划</p>
      <p class="empty-hint">点击「新建规划」，去地图上规划你的下一段旅程</p>
    </div>

    <div v-else class="plan-grid">
      <div
        v-for="p in plans"
        :key="p.id"
        class="plan-card glass-panel"
        @click="openEditor(p.id)"
      >
        <div class="card-cover">
          <img v-if="p.image && !imgError.has(p.id)" :src="p.image" class="cover-img" alt="" @error="onImgError(p.id)" />
          <div v-else class="cover-fallback">
            <span class="cover-emoji">🗺️</span>
            <span class="cover-letter">{{ (p.destination || p.name || '旅').charAt(0) }}</span>
          </div>
          <div class="cover-tags">
            <span v-if="p.collabLocked" class="tag lock">🔒 已锁定</span>
            <span class="tag" :class="p.isPublic ? 'public' : 'private'">
              {{ p.isPublic ? '🌐 公开' : '🔒 私有' }}
            </span>
            <span v-if="p.collaborated" class="tag collab">👥 协作</span>
          </div>
        </div>

        <div class="card-body">
          <h3 class="card-name">{{ p.name }}</h3>
          <div class="card-dest">📍 {{ p.destination }}</div>
          <div class="card-meta">
            <span v-if="p.location" class="meta-item">{{ p.location }}</span>
            <span class="meta-item">📅 {{ formatDate(p.startDate) }}</span>
          </div>
        </div>

        <div v-if="!p.collaborated" class="card-actions" @click.stop>
          <button class="mini-btn" title="编辑信息" @click="handleEdit(p)">编辑</button>
          <button class="mini-btn danger" title="删除" @click="handleDelete(p)">删除</button>
        </div>
      </div>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="page <= 1" @click="goPage(page - 1)">‹ 上一页</button>
      <span class="page-info">{{ page }} / {{ totalPages }}</span>
      <button class="page-btn" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页 ›</button>
    </div>

    <AddChecklistDialog
      v-if="showDialog"
      :edit-data="editTarget"
      @confirm="handleDialogConfirm"
      @cancel="showDialog = false; editTarget = null"
    />

    <AdminConfirmDialog
      :visible="!!deleteTarget"
      title="删除旅游规划"
      :message="`确定要删除「${deleteTarget?.name || '这份规划'}」吗？删除后不可恢复。`"
      confirm-text="删除"
      danger
      @confirm="confirmDelete"
      @cancel="deleteTarget = null"
    />
  </div>
</template>

<style scoped>
.plan-page {
  padding: 32px 40px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
  overflow-y: auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title { font-size: 28px; font-weight: 800; }
.page-sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

.add-btn { padding: 10px 24px; font-size: 14px; }

.loading { padding: 60px 0; text-align: center; color: var(--text-muted); }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}
.empty-icon { font-size: 56px; margin-bottom: 16px; }
.empty-text { font-size: 16px; font-weight: 600; margin-bottom: 6px; }
.empty-hint { font-size: 13px; color: var(--text-muted); }

.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 8px 0;
}
.page-btn {
  padding: 8px 18px;
  border-radius: var(--radius-pill);
  background: rgba(var(--overlay-rgb), 0.06);
  color: var(--text-secondary);
  font-size: 13px;
  transition: var(--transition);
}
.page-btn:hover:not(:disabled) { background: rgba(var(--overlay-rgb), 0.12); color: var(--text-primary); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 13px; color: var(--text-muted); font-variant-numeric: tabular-nums; }

.plan-card {
  display: flex;
  flex-direction: column;
  cursor: pointer;
  overflow: hidden;
  transition: var(--transition);
  border: 2px solid transparent;
}
.plan-card:hover {
  transform: translateY(-3px);
  border-color: var(--accent-primary);
  box-shadow: 0 8px 28px rgba(var(--accent-rgb), 0.18);
}

.card-cover {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: rgba(var(--overlay-rgb), 0.05);
}
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-fallback {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(var(--accent-rgb), 0.35), rgba(var(--accent-rgb), 0.12));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.cover-emoji { font-size: 30px; opacity: 0.9; }
.cover-letter { font-size: 12px; color: var(--text-secondary); font-weight: 600; }

.cover-tags {
  position: absolute;
  top: 10px;
  left: 10px;
  right: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.tag {
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 600;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  backdrop-filter: blur(4px);
}
.tag.lock { background: rgba(220, 38, 38, 0.7); }
.tag.collab { background: rgba(99, 102, 241, 0.78); }

.card-body {
  padding: 14px 16px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}
.card-name {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-dest { font-size: 13px; color: var(--text-secondary); }
.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 2px;
}
.meta-item { font-size: 12px; color: var(--text-muted); }

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 12px 12px;
}
.mini-btn {
  padding: 5px 12px;
  border-radius: var(--radius-xs);
  background: rgba(var(--overlay-rgb), 0.06);
  color: var(--text-secondary);
  font-size: 12px;
  transition: var(--transition);
}
.mini-btn:hover { background: rgba(var(--overlay-rgb), 0.12); color: var(--text-primary); }
.mini-btn.danger:hover { color: var(--danger); background: rgba(248, 113, 113, 0.12); }

@media (max-width: 900px) {
  .plan-page { padding: 24px; }
}
</style>
