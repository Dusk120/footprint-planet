<script setup>
import { ref, onMounted, watch } from 'vue'
import { adminApi } from '../../api'
import { useToast } from '../../composables/useToast'
import AdminPagination from '../../components/admin/AdminPagination.vue'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog.vue'
import { formatDateTime } from '../../utils/format'

const { show: toast } = useToast()

const keyword = ref('')        // 输入框内容
const appliedKeyword = ref('') // 已提交的搜索词，驱动请求
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)

const confirmOpen = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)

function locText(n) {
  const loc = n.location
  if (!loc) return '—'
  const name = loc.name || ''
  const prov = loc.province || ''
  if (name && prov) return `${name} · ${prov}`
  return name || prov || '—'
}

async function load() {
  loading.value = true
  try {
    const r = await adminApi.getNotes({ keyword: appliedKeyword.value, page: page.value, pageSize })
    list.value = r.list || []
    total.value = r.total || 0
  } catch (e) {
    toast(e.message || '笔记列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  appliedKeyword.value = keyword.value.trim()
  page.value = 1
  load()
}

// 输入框被清空时自动恢复全量列表，省掉「清空后再点一次搜索」
watch(keyword, (v) => {
  if (v.trim() || !appliedKeyword.value) return
  appliedKeyword.value = ''
  page.value = 1
  load()
})

function onPage(p) {
  page.value = p
  load()
}

function askDelete(n) {
  deleteTarget.value = n
  confirmOpen.value = true
}

function refreshAfterDelete() {
  // 若当前页被删空且不是第一页，则回退一页重新加载
  if (list.value.length === 0 && page.value > 1) {
    page.value -= 1
    load()
    return
  }
  const idx = list.value.findIndex(n => n.id === deleteTarget.value?.id)
  if (idx >= 0) {
    list.value.splice(idx, 1)
    total.value -= 1
  }
}

async function confirmDelete() {
  const n = deleteTarget.value
  if (!n) return
  deleting.value = true
  try {
    await adminApi.removeNote(n.id)
    toast(`已删除笔记「${n.title}」`, 'success')
    confirmOpen.value = false
    deleteTarget.value = null
    refreshAfterDelete()
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
      <h1 class="admin-page-title">笔记管理</h1>
      <p class="admin-page-sub">审核与处理社区笔记内容</p>
    </div>

    <div class="admin-toolbar">
      <input
        v-model="keyword"
        class="text-input"
        placeholder="搜索标题 / 内容 / 作者昵称"
        @keyup.enter="onSearch"
      />
      <button class="admin-search-btn" @click="onSearch">搜索</button>
    </div>

    <div class="admin-table-card glass-panel">
      <div v-if="loading" class="admin-loading">加载中…</div>

      <table v-else-if="list.length" class="admin-table">
        <thead>
          <tr>
            <th style="width: 60px">ID</th>
            <th>笔记内容</th>
            <th>作者</th>
            <th>点赞</th>
            <th>关联地点</th>
            <th>发布时间</th>
            <th style="text-align: right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="n in list" :key="n.id">
            <td>{{ n.id }}</td>
            <td>
              <div class="cell-main">
                <span class="note-title" :title="n.title">{{ n.title }}</span>
                <span class="cell-sub note-content" :title="n.content">{{ n.content || '（无正文）' }}</span>
              </div>
            </td>
            <td>
              <span class="cell-ellipsis" :title="`ID ${n.authorId}`">{{ n.author || '—' }}</span>
            </td>
            <td>{{ (n.likes ?? 0).toLocaleString() }}</td>
            <td><span class="cell-ellipsis" style="max-width: 160px">{{ locText(n) }}</span></td>
            <td>{{ formatDateTime(n.createdAt) }}</td>
            <td>
              <div class="col-actions">
                <button class="btn-admin danger" :disabled="deleting" @click="askDelete(n)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="admin-empty">暂无笔记数据</div>
    </div>

    <AdminPagination :page="page" :total="total" :page-size="pageSize" @change="onPage" />

    <AdminConfirmDialog
      :visible="confirmOpen"
      title="删除笔记"
      :message="`确认删除笔记「${deleteTarget?.title || ''}」吗？\n删除后不可恢复，作者在社区将无法再看到该内容。`"
      confirm-text="确认删除"
      danger
      :busy="deleting"
      @cancel="confirmOpen = false; deleteTarget = null"
      @confirm="confirmDelete"
    />
  </div>
</template>

<style scoped>
.note-title {
  font-size: 15px;
  font-weight: 600;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 340px;
}
.note-content {
  max-width: 340px;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
