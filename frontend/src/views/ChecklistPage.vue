<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '../stores/app'
import { useToast } from '../composables/useToast'
import AddChecklistDialog from '../components/AddChecklistDialog.vue'
import ChecklistDetail from '../components/ChecklistDetail.vue'
import AdminConfirmDialog from '../components/admin/AdminConfirmDialog.vue'

const store = useAppStore()
const { show: toast } = useToast()

const showDialog = ref(false)
const editTarget = ref(null)
const selectedId = ref(null)
const deleteConfirm = ref(false)
const PAGE_SIZE = 4
const currentPage = ref(1)

// 后端真分页：页内只存当前这一页
const plans = ref([])
const planTotal = ref(0)

const totalPages = computed(() => Math.max(1, Math.ceil(planTotal.value / PAGE_SIZE)))

const selectedItem = computed(() =>
  plans.value.find(w => w.id === selectedId.value) || null
)

async function loadPlans() {
  try {
    const res = await store.fetchPlans({ page: currentPage.value, pageSize: PAGE_SIZE })
    plans.value = res.list
    planTotal.value = res.total
  } catch (e) {
    toast(e.message || '加载旅游规划失败', 'error')
  }
}

onMounted(loadPlans)

function handleAdd() {
  editTarget.value = null
  showDialog.value = true
}

function handleEdit() {
  if (!selectedItem.value) return
  editTarget.value = { ...selectedItem.value }
  showDialog.value = true
}

async function handleDialogConfirm(data) {
  try {
    if (editTarget.value) {
      await store.updatePlan(editTarget.value.id, data)
      toast('清单已更新')
      await loadPlans()
    } else {
      const entry = await store.addPlan(data)
      // 新清单按创建时间倒序排在首页，回到第 1 页并选中它
      currentPage.value = 1
      await loadPlans()
      selectedId.value = entry.id
      toast('清单已创建')
    }
    showDialog.value = false
    editTarget.value = null
  } catch (e) {
    toast(e.message || '保存失败，请重试', 'error')
  }
}

async function handleDelete() {
  if (!selectedItem.value) return
  deleteConfirm.value = true
}

async function confirmDelete() {
  const item = selectedItem.value
  if (!item) return
  try {
    await store.removePlan(item.id)
    selectedId.value = null
    deleteConfirm.value = false
    // 删掉本页最后一条且不在首页时回退一页
    if (plans.value.length === 1 && currentPage.value > 1) currentPage.value--
    await loadPlans()
    toast('清单已删除')
  } catch (e) {
    deleteConfirm.value = false
    toast(e.message || '删除失败，请重试', 'error')
  }
}

function handleSelect(id) {
  selectedId.value = selectedId.value === id ? null : id
}

function formatDate(d) {
  if (!d) return '未设定'
  return d
}

function totalItems(item) {
  return (item.days || []).reduce((sum, d) => sum + (d.items?.length || 0), 0)
}

function goToPage(p) {
  if (p < 1 || p > totalPages.value) return
  currentPage.value = p
  loadPlans()
}
</script>

<template>
  <div class="checklist-page">
    <div class="page-header">
      <h1 class="page-title">旅游规划</h1>
      <button class="add-btn gradient-btn" @click="handleAdd">
        ＋ 新建清单
      </button>
    </div>

    <div class="checklist-content">
      <!-- 左侧清单列表 -->
      <div class="list-section">
        <div v-if="!plans.length" class="empty-state">
          <div class="empty-icon">📋</div>
          <p class="empty-text">还没有旅游规划</p>
          <p class="empty-hint">点击上方按钮创建你的第一份旅行计划</p>
        </div>

        <div class="list-scroll">
          <div
            v-for="item in plans"
            :key="item.id"
            class="list-card glass-panel"
            :class="{ active: selectedId === item.id }"
            @click="handleSelect(item.id)"
          >
            <div class="card-top">
              <div class="card-emoji">✈️</div>
              <div class="card-info">
                <h3 class="card-name">{{ item.name }}</h3>
                <span class="card-dest">📍 {{ item.destination }}</span>
              </div>
            </div>
            <div class="card-meta">
              <span class="meta-item">📅 {{ formatDate(item.startDate) }}</span>
              <span class="meta-item">{{ item.days?.length || 0 }} 天</span>
              <span class="meta-item">{{ totalItems(item) }} 个行程</span>
            </div>
            <div v-if="item.waypoints?.length" class="card-waypoints">
              <span v-for="(wp, i) in item.waypoints.slice(0, 3)" :key="i" class="waypoint-pill">
                📍 {{ wp.name }}
              </span>
              <span v-if="item.waypoints.length > 3" class="waypoint-pill more">
                +{{ item.waypoints.length - 3 }}
              </span>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="totalPages > 1" class="pagination">
          <button class="page-btn" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
          <button
            v-for="p in totalPages"
            :key="p"
            class="page-num"
            :class="{ active: p === currentPage }"
            @click="goToPage(p)"
          >{{ p }}</button>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        </div>
      </div>

      <!-- 右侧详情 -->
      <div class="detail-section glass-panel">
        <ChecklistDetail
          v-if="selectedItem"
          :item="selectedItem"
          @edit="handleEdit"
          @delete="handleDelete"
          @back="selectedId = null"
        />
        <div v-else class="detail-empty">
          <div class="detail-empty-icon">🗺️</div>
          <p class="detail-empty-text">选择一份清单查看详情</p>
          <p class="detail-empty-hint">或点击「新建清单」开始规划旅行</p>
        </div>
      </div>
    </div>

    <AddChecklistDialog
      v-if="showDialog"
      :edit-data="editTarget"
      @confirm="handleDialogConfirm"
      @cancel="showDialog = false; editTarget = null"
    />

    <AdminConfirmDialog
      :visible="deleteConfirm"
      title="删除旅游规划"
      :message="`确定要删除「${selectedItem?.name || '这份规划'}」吗？删除后不可恢复。`"
      confirm-text="删除"
      danger
      @confirm="confirmDelete"
      @cancel="deleteConfirm = false"
    />
  </div>
</template>

<style scoped>
.checklist-page {
  padding: 32px 40px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title { font-size: 28px; font-weight: 800; }

.add-btn {
  padding: 10px 24px;
  font-size: 14px;
}

.checklist-content {
  display: flex;
  gap: 24px;
  flex: 1;
  min-height: 0;
}

.list-section {
  width: 360px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.list-scroll {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
  padding-right: 4px;
}

.detail-section {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  min-height: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-text { font-size: 16px; font-weight: 600; margin-bottom: 6px; }
.empty-hint { font-size: 13px; color: var(--text-muted); }

.list-card {
  padding: 18px;
  cursor: pointer;
  transition: var(--transition);
  border: 2px solid transparent;
}
.list-card:hover {
  background: var(--bg-card-hover);
  transform: translateY(-1px);
}
.list-card.active {
  border-color: var(--accent-primary);
  box-shadow: 0 0 16px rgba(var(--accent-rgb), 0.2);
}

.card-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.card-emoji {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, rgba(var(--accent-rgb), 0.2), rgba(14, 165, 233, 0.2));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.card-info { display: flex; flex-direction: column; min-width: 0; }
.card-name { font-size: 15px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card-dest { font-size: 12px; color: var(--text-muted); }

.card-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}
.meta-item { font-size: 12px; color: var(--text-secondary); }

.card-waypoints {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.waypoint-pill {
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  background: rgba(var(--accent-rgb), 0.08);
  font-size: 11px;
  color: var(--accent-primary);
}
.waypoint-pill.more {
  background: rgba(var(--overlay-rgb), 0.06);
  color: var(--text-muted);
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 0;
  flex-shrink: 0;
}

.page-btn, .page-num {
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

.page-btn {
  background: rgba(var(--overlay-rgb), 0.06);
  color: var(--text-secondary);
}
.page-btn:hover:not(:disabled) {
  background: rgba(var(--overlay-rgb), 0.12);
}
.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-num {
  background: rgba(var(--overlay-rgb), 0.04);
  color: var(--text-secondary);
}
.page-num:hover {
  background: rgba(var(--overlay-rgb), 0.08);
}
.page-num.active {
  background: var(--accent-gradient);
  color: white;
}

.detail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}
.detail-empty-icon { font-size: 56px; margin-bottom: 16px; }
.detail-empty-text { font-size: 16px; font-weight: 600; margin-bottom: 6px; }
.detail-empty-hint { font-size: 13px; color: var(--text-muted); }

@media (max-width: 900px) {
  .checklist-page { padding: 24px; }
  .checklist-content { flex-direction: column; }
  .list-section { width: 100%; }
}
</style>
