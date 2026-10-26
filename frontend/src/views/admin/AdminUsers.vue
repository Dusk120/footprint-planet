<script setup>
import { ref, onMounted, watch } from 'vue'
import { adminApi } from '../../api'
import { useAppStore } from '../../stores/app'
import { useToast } from '../../composables/useToast'
import AdminPagination from '../../components/admin/AdminPagination.vue'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog.vue'
import AdminUserDrawer from '../../components/admin/AdminUserDrawer.vue'
import { formatDateTime } from '../../utils/format'

const { show: toast } = useToast()
const store = useAppStore()

const keyword = ref('')        // 输入框内容
const appliedKeyword = ref('') // 已提交的搜索词，驱动请求
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)

// 行内操作 busy 标记（记录正在操作的用户 id）
const runningId = ref(null)

// 正在查看详情的用户 id（null 表示抽屉关闭）
const detailUserId = ref(null)

// 禁用二次确认
const confirmOpen = ref(false)
const confirmUser = ref(null)

// 重置密码弹窗
const resetOpen = ref(false)
const resetTarget = ref(null)
const resetPwd = ref('')
const resetBusy = ref(false)

function displayName(u) {
  return u ? (u.nickname || u.username || '') : ''
}

async function load() {
  loading.value = true
  try {
    const r = await adminApi.getUsers({ keyword: appliedKeyword.value, page: page.value, pageSize })
    list.value = r.list || []
    total.value = r.total || 0
  } catch (e) {
    toast(e.message || '用户列表加载失败', 'error')
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

function isSelf(u) {
  return u.id === store.currentUser?.id
}

// 点击「禁用」进入确认；点击「启用」直接生效
function askDisable(u) {
  confirmUser.value = u
  confirmOpen.value = true
}

async function confirmDisable() {
  const u = confirmUser.value
  if (!u) return
  runningId.value = u.id
  try {
    await adminApi.setUserStatus(u.id, 1)
    u.status = 1
    toast(`已禁用用户「${displayName(u)}」`, 'success')
    confirmOpen.value = false
    confirmUser.value = null
  } catch (e) {
    toast(e.message || '操作失败', 'error')
  } finally {
    runningId.value = null
  }
}

async function enableUser(u) {
  runningId.value = u.id
  try {
    await adminApi.setUserStatus(u.id, 0)
    u.status = 0
    toast(`已启用用户「${displayName(u)}」`, 'success')
  } catch (e) {
    toast(e.message || '操作失败', 'error')
  } finally {
    runningId.value = null
  }
}

function openReset(u) {
  resetTarget.value = u
  resetPwd.value = ''
  resetOpen.value = true
}

function closeReset() {
  if (resetBusy.value) return
  resetOpen.value = false
  resetTarget.value = null
}

async function submitReset() {
  const p = resetPwd.value.trim()
  if (p.length < 6) {
    toast('新密码至少 6 位', 'warning')
    return
  }
  const u = resetTarget.value
  resetBusy.value = true
  try {
    await adminApi.resetUserPassword(u.id, p)
    toast(`已重置「${displayName(u)}」的密码`, 'success')
    resetOpen.value = false
    resetTarget.value = null
  } catch (e) {
    toast(e.message || '重置失败', 'error')
  } finally {
    resetBusy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <div>
      <h1 class="admin-page-title">用户管理</h1>
      <p class="admin-page-sub">查看与维护平台注册用户</p>
    </div>

    <div class="admin-toolbar">
      <input
        v-model="keyword"
        class="text-input"
        placeholder="搜索用户名 / 昵称"
        @keyup.enter="onSearch"
      />
      <button class="admin-search-btn" @click="onSearch">搜索</button>
    </div>

    <div class="admin-table-card glass-panel">
      <div v-if="loading" class="admin-loading">加载中…</div>

      <table v-else-if="list.length" class="admin-table">
        <thead>
          <tr>
            <th>用户</th>
            <th>角色</th>
            <th>状态</th>
            <th>签名</th>
            <th>点亮</th>
            <th>笔记</th>
            <th>注册时间</th>
            <th style="text-align: right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in list" :key="u.id">
            <td>
              <div class="cell-main">
                <span class="cell-sub">ID {{ u.id }} · @{{ u.username }}</span>
                <span class="cell-name">{{ displayName(u) }}<span v-if="isSelf(u)" class="self-tag">我</span></span>
              </div>
            </td>
            <td><span class="admin-badge" :class="u.role === 'ADMIN' ? 'admin-badge-admin' : 'admin-badge-user'">{{ u.role }}</span></td>
            <td>
              <span class="admin-badge" :class="u.status === 0 ? 'admin-badge-ok' : 'admin-badge-warn'">
                {{ u.status === 0 ? '正常' : '禁用' }}
              </span>
            </td>
            <td><span class="cell-ellipsis" :title="u.motto || ''">{{ u.motto || '—' }}</span></td>
            <td>{{ u.visitedCount ?? 0 }}</td>
            <td>{{ u.noteCount ?? 0 }}</td>
            <td>{{ formatDateTime(u.createdAt) }}</td>
            <td>
              <div class="col-actions">
                <button class="btn-admin" @click="detailUserId = u.id">详情</button>
                <button class="btn-admin" :disabled="runningId === u.id" @click="openReset(u)">重置密码</button>
                <button
                  v-if="u.status === 0"
                  class="btn-admin danger"
                  :disabled="runningId === u.id || isSelf(u)"
                  :title="isSelf(u) ? '不能禁用自己' : ''"
                  @click="askDisable(u)"
                >禁用</button>
                <button v-else class="btn-admin" :disabled="runningId === u.id" @click="enableUser(u)">启用</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="admin-empty">暂无用户数据</div>
    </div>

    <AdminPagination :page="page" :total="total" :page-size="pageSize" @change="onPage" />

    <!-- 用户详情抽屉 -->
    <AdminUserDrawer :user-id="detailUserId" @close="detailUserId = null" />

    <!-- 禁用确认 -->
    <AdminConfirmDialog
      :visible="confirmOpen"
      title="禁用用户"
      :message="`确认禁用用户「${displayName(confirmUser || {})}」吗？\n禁用后该账号将无法登录，社区内容仍会保留。`"
      confirm-text="确认禁用"
      danger
      :busy="!!confirmUser && runningId === confirmUser.id"
      @cancel="confirmOpen = false; confirmUser = null"
      @confirm="confirmDisable"
    />

    <!-- 重置密码 -->
    <Teleport to="body">
      <div v-if="resetOpen" class="dialog-overlay" @click.self="closeReset">
        <div class="dialog admin-reset-dialog">
          <div class="dialog-header">
            <span class="dialog-emoji">🔑</span>
            <div class="dialog-title-wrap">
              <h2 class="dialog-title">重置密码</h2>
              <span class="dialog-sub">为「{{ displayName(resetTarget || {}) }}」设置新密码</span>
            </div>
            <span class="dialog-mode mode-edit">管理员操作</span>
          </div>
          <div class="dialog-body">
            <div class="field">
              <label class="field-label">新密码</label>
              <input v-model="resetPwd" type="password" class="text-input" placeholder="至少 6 位" maxlength="50" @keyup.enter="submitReset" />
            </div>
            <p class="reset-hint">该用户下次登录需使用新密码，原密码立即失效。</p>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" :disabled="resetBusy" @click="closeReset">取消</button>
            <button class="btn-confirm" :disabled="!resetPwd.trim() || resetBusy" @click="submitReset">
              {{ resetBusy ? '提交中…' : '确认重置' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.cell-name {
  font-size: 15px;
  font-weight: 600;
}
.self-tag {
  margin-left: 6px;
  font-size: 11px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: var(--radius-pill);
  background: rgba(var(--accent-rgb), 0.18);
  color: var(--accent-soft);
  vertical-align: 1px;
}
.admin-reset-dialog { width: 440px; }
.reset-hint {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.6;
}
</style>
