<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../../api'
import { useToast } from '../../composables/useToast'
import AdminPagination from '../../components/admin/AdminPagination.vue'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog.vue'
import { formatDateTime } from '../../utils/format'

const { show: toast } = useToast()

const tabs = [
  { label: '全部', value: null },
  { label: '待处理', value: 0 },
  { label: '已处理', value: 1 }
]

// handledFilter 为数字 0/1（筛选参数），null 表示全部
const handledFilter = ref(null)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)

const runningId = ref(null)
const confirmOpen = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize }
    if (handledFilter.value !== null) params.handled = handledFilter.value
    const r = await adminApi.getFeedbacks(params)
    list.value = r.list || []
    total.value = r.total || 0
  } catch (e) {
    toast(e.message || '反馈列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function switchTab(v) {
  handledFilter.value = v
  page.value = 1
  load()
}

function onPage(p) {
  page.value = p
  load()
}

async function toggleHandled(f) {
  const next = !f.handled
  runningId.value = f.id
  try {
    await adminApi.setFeedbackHandled(f.id, next)
    toast(next ? '已标记为已处理' : '已改回待处理', 'success')
    // 在「全部」下直接翻转保持视图；在具体筛选 tab 下重新加载以移除不再符合的行
    if (handledFilter.value === null) {
      f.handled = next
    } else {
      load()
    }
  } catch (e) {
    toast(e.message || '操作失败', 'error')
  } finally {
    runningId.value = null
  }
}

function askDelete(f) {
  deleteTarget.value = f
  confirmOpen.value = true
}

async function confirmDelete() {
  const f = deleteTarget.value
  if (!f) return
  deleting.value = true
  try {
    await adminApi.removeFeedback(f.id)
    toast('反馈已删除', 'success')
    confirmOpen.value = false
    deleteTarget.value = null
    const idx = list.value.findIndex(x => x.id === f.id)
    if (idx >= 0) {
      list.value.splice(idx, 1)
      total.value -= 1
    }
    if (list.value.length === 0 && page.value > 1) {
      page.value -= 1
      load()
    }
  } catch (e) {
    toast(e.message || '删除失败', 'error')
  } finally {
    deleting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <div>
      <h1 class="admin-page-title">反馈管理</h1>
      <p class="admin-page-sub">查看并处理用户建议与问题反馈</p>
    </div>

    <div class="admin-tabs">
      <button
        v-for="t in tabs"
        :key="String(t.value)"
        class="admin-tab"
        :class="{ active: handledFilter === t.value }"
        @click="switchTab(t.value)"
      >{{ t.label }}</button>
    </div>

    <div class="admin-table-card glass-panel">
      <div v-if="loading" class="admin-loading">加载中…</div>

      <table v-else-if="list.length" class="admin-table">
        <thead>
          <tr>
            <th style="width: 60px">ID</th>
            <th>反馈内容</th>
            <th>联系方式</th>
            <th>截图</th>
            <th>提交时间</th>
            <th>状态</th>
            <th style="text-align: right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="f in list" :key="f.id">
            <td>{{ f.id }}</td>
            <td><span class="cell-ellipsis feedback-content" :title="f.content">{{ f.content }}</span></td>
            <td><span class="cell-ellipsis" style="max-width: 140px">{{ f.contact || '—' }}</span></td>
            <td>
              <div v-if="f.images && f.images.length" class="shot-list">
                <a
                  v-for="img in f.images.slice(0, 3)"
                  :key="img"
                  class="shot-thumb"
                  :href="img"
                  target="_blank"
                  rel="noopener"
                >
                  <img :src="img" alt="反馈截图" />
                </a>
                <span v-if="f.images.length > 3" class="shot-more">+{{ f.images.length - 3 }}</span>
              </div>
              <span v-else class="cell-sub">—</span>
            </td>
            <td>{{ formatDateTime(f.createdAt) }}</td>
            <td>
              <span class="admin-badge" :class="f.handled ? 'admin-badge-ok' : 'admin-badge-warn'">
                {{ f.handled ? '已处理' : '待处理' }}
              </span>
            </td>
            <td>
              <div class="col-actions">
                <button class="btn-admin" :disabled="runningId === f.id" @click="toggleHandled(f)">
                  {{ f.handled ? '改回待处理' : '标记已处理' }}
                </button>
                <button class="btn-admin danger" :disabled="deleting" @click="askDelete(f)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="admin-empty">
        {{ handledFilter === 1 ? '暂无已处理反馈' : (handledFilter === 0 ? '暂无待处理反馈' : '暂无反馈数据') }}
      </div>
    </div>

    <AdminPagination :page="page" :total="total" :page-size="pageSize" @change="onPage" />

    <AdminConfirmDialog
      :visible="confirmOpen"
      title="删除反馈"
      message="确认删除该条反馈吗？\n删除后不可恢复。"
      confirm-text="确认删除"
      danger
      :busy="deleting"
      @cancel="confirmOpen = false; deleteTarget = null"
      @confirm="confirmDelete"
    />
  </div>
</template>

<style scoped>
.feedback-content {
  max-width: 340px;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shot-list {
  display: flex;
  align-items: center;
  gap: 6px;
}

.shot-thumb {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: block;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid rgba(var(--overlay-rgb), 0.12);
  transition: var(--transition);
}
.shot-thumb:hover {
  border-color: var(--accent-primary);
}
.shot-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.shot-more {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
