<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Client } from '@stomp/stompjs'
import draggable from 'vuedraggable'
import { planApi, uploadApi } from '../api'
import { getToken } from '../api/http'
import { useToast } from '../composables/useToast'
import { useAppStore } from '../stores/app'
import AmapMap from '../components/AmapMap.vue'

const route = useRoute()
const router = useRouter()
const { show: toast } = useToast()
const store = useAppStore()

const planId = route.params.id
const plan = ref(null)      // PlanDetailVO
const loading = ref(true)

// ---------- 加载详情 ----------
async function loadDetail() {
  try {
    plan.value = await planApi.detail(planId)
    // 校验当前选中的天是否还存在
    if (!days.value.some(d => d.dayNo === activeDay.value)) {
      activeDay.value = days.value[0]?.dayNo ?? 1
    }
  } catch (e) {
    toast(e.message || '加载规划失败', 'error')
    router.replace('/checklist')
  } finally {
    loading.value = false
  }
}
onMounted(async () => {
  await loadDetail()
  await loadInvites()  // 页面加载即拉邀请列表，避免「申请新的之前看不到」
  connectWs()
})

// ---------- 权限 ----------
const canEdit = computed(() => ['owner', 'editor'].includes(plan.value?.myRole))
const isOwner = computed(() => plan.value?.myRole === 'owner')
const isLocked = computed(() => {
  // owner 不受锁定/截止时间限制（后端 requireEditor 只对 editor 校验锁定）
  if (plan.value?.myRole === 'owner') return false
  return plan.value?.collabLocked ||
    (plan.value?.collabDeadline && new Date(plan.value.collabDeadline).getTime() < Date.now())
})

// 编辑操作统一入口：editor 在锁定时提示「已锁定」，owner 不受限
function ensureEditable() {
  if (!canEdit.value) return false
  if (isLocked.value) {
    toast('计划已锁定或已到截止时间，暂不可编辑', 'error')
    return false
  }
  return true
}

// ---------- 封面上传（仅 owner） ----------
const coverInput = ref(null)
const uploadingCover = ref(false)
function pickCover() {
  if (!isOwner.value || uploadingCover.value) return
  coverInput.value?.click()
}
async function onCoverChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 10 * 1024 * 1024) {
    toast('封面不能超过 10MB', 'error')
    e.target.value = ''
    return
  }
  uploadingCover.value = true
  try {
    const url = await uploadApi.uploadImage(file)
    await planApi.setCover(planId, url)
    plan.value.image = url
    toast('封面已更新')
  } catch (err) {
    toast(err.message || '封面上传失败', 'error')
  } finally {
    uploadingCover.value = false
    e.target.value = ''
  }
}

// ---------- 按天分组 ----------
const days = computed(() => {
  const items = plan.value?.items || []
  const map = {}
  items.forEach(it => {
    const d = it.dayNo ?? 0
    ;(map[d] = map[d] || []).push(it)
  })
  // 确保 activeDay 指向的天始终显示（即使还没有地点，让用户能切换到新添加的空天）
  if (activeDay.value != null && !map[activeDay.value]) {
    map[activeDay.value] = []
  }
  return Object.keys(map)
    .map(Number)
    .sort((a, b) => a - b)
    .map(d => ({ dayNo: d, items: (map[d] || []).sort((a, b) => a.sortNo - b.sortNo) }))
})

// ---------- 选中的天（地图搜索/选点添加到这一天） ----------
const activeDay = ref(1)
function selectDay(d) {
  activeDay.value = d
}
// 添加新的一天（activeDay 指向最大天 + 1 的空天）
function addDay() {
  if (!ensureEditable()) return
  const maxDay = days.value.length ? Math.max(...days.value.map(d => d.dayNo)) : 0
  activeDay.value = maxDay + 1
}

// ---------- 每日条目折叠（收起详细条目，只看「第 N 天 + 地点数」）；按计划本地记忆 ----------
const COLLAPSED_KEY = `footprint-planet:collapsed-days:${planId}`
function loadCollapsedDays() {
  try {
    return new Set(JSON.parse(localStorage.getItem(COLLAPSED_KEY) || '[]'))
  } catch (e) {
    return new Set()
  }
}
const collapsedDays = ref(loadCollapsedDays())
function toggleCollapse(dayNo) {
  const s = new Set(collapsedDays.value)
  if (s.has(dayNo)) s.delete(dayNo)
  else s.add(dayNo)
  collapsedDays.value = s
  localStorage.setItem(COLLAPSED_KEY, JSON.stringify([...s]))
}

// ---------- 交通段（相邻地点之间的出行方案） ----------
// 后端返回 plan.legs：[{ fromItemId, toItemId, mode, duration, distance, summary }]
// 建 fromItemId → leg 映射，方便在行程面板里「地点 → 下一地点」之间展示
const legMap = computed(() => {
  const map = {}
  ;(plan.value?.legs || []).forEach(l => { map[l.fromItemId] = l })
  return map
})
function modeEmoji(mode) {
  return { walking: '🚶', driving: '🚗', transit: '🚌' }[mode] || '➡️'
}

// ---------- 地图搜索 + 添加 ----------
const amapRef = ref(null)
const searchKw = ref('')
const searchResults = ref([])

async function doSearch() {
  if (!searchKw.value.trim() || !amapRef.value) return
  searchResults.value = await amapRef.value.search(searchKw.value.trim())
}

// 清空搜索框时同步清空结果列表
watch(searchKw, (kw) => {
  if (!kw.trim()) searchResults.value = []
})

// 点击搜索结果：只在地图上定位显示，不加入计划
function focusSearchResult(r) {
  amapRef.value?.focusOn({ lng: r.lng, lat: r.lat, name: r.name })
}

async function addToActiveDay({ name, lng, lat, poiId }) {
  if (!ensureEditable()) return
  if (!name) return
  try {
    await planApi.addItem(planId, { dayNo: activeDay.value, name, lng, lat, poiId: poiId || '' })
    searchResults.value = []
    searchKw.value = ''
    await loadDetail()
  } catch (e) {
    toast(e.message || '添加失败', 'error')
  }
}

// ---------- 出行方案选择 ----------
const routeDialog = ref(false)
const routeLoading = ref(false)
const routeOptions = ref([])
// 出行方案上下文：{ type: 'add', target, fromItem } 或 { type: 'leg', fromItem, toItem }
const routeContext = ref(null)

// 取某天最后一个地点（作为出行方案的起点）
function getLastItemOfDay(dayNo) {
  const day = days.value.find(d => d.dayNo === dayNo)
  if (!day || !day.items.length) return null
  return day.items[day.items.length - 1]
}

// 取某地点在当天内的下一个地点
function getNextItemOf(item) {
  const day = days.value.find(d => d.dayNo === item.dayNo)
  if (!day) return null
  const idx = day.items.findIndex(i => i.id === item.id)
  return day.items[idx + 1] || null
}

function addSearchResult(r) {
  openRouteSelect({ name: r.name, lng: r.lng, lat: r.lat, poiId: r.poiId })
}

function handleMapSelect({ lng, lat, name, poiId }) {
  openRouteSelect({ name: name || '未命名地点', lng, lat, poiId })
}

// 通用：计算 from → to 的出行方案并打开弹窗（调后端 Web 服务算路）
async function openRouteDialog(from, to, context) {
  routeContext.value = context
  routeDialog.value = true
  routeLoading.value = true
  routeOptions.value = []
  try {
    routeOptions.value = await planApi.routeSearch(from, to)
  } catch (e) {
    toast(e.message || '路线规划失败', 'error')
  } finally {
    routeLoading.value = false
  }
}

// 添加新地点：当天有最后地点则算「最后地点 → 新地点」方案，否则直接添加
async function openRouteSelect(target) {
  if (!ensureEditable()) return
  if (!target.name) return
  const lastItem = getLastItemOfDay(activeDay.value)
  if (!lastItem || lastItem.lng == null || lastItem.lat == null) {
    await addToActiveDay(target)
    return
  }
  await openRouteDialog(
    { lng: lastItem.lng, lat: lastItem.lat },
    { lng: target.lng, lat: target.lat },
    { type: 'add', target, fromItem: lastItem }
  )
}

// 补充已有地点之间的方案（点击「请选择出行方案」）
async function openLegSelect(fromItem) {
  if (!ensureEditable()) return
  const toItem = getNextItemOf(fromItem)
  if (!toItem || toItem.lng == null || toItem.lat == null) return
  await openRouteDialog(
    { lng: fromItem.lng, lat: fromItem.lat },
    { lng: toItem.lng, lat: toItem.lat },
    { type: 'leg', fromItem, toItem }
  )
}

// 选择某出行方案：添加地点（add 场景）或补充 leg（leg 场景）
async function chooseRoute(option) {
  const ctx = routeContext.value
  if (!ctx || !option) return
  try {
    if (ctx.type === 'add') {
      const newItem = await planApi.addItem(planId, { dayNo: activeDay.value, name: ctx.target.name, lng: ctx.target.lng, lat: ctx.target.lat, poiId: ctx.target.poiId || '' })
      await planApi.saveLeg(planId, {
        fromItemId: ctx.fromItem.id,
        toItemId: newItem.id,
        mode: option.mode,
        duration: Math.round(option.duration * 60),
        distance: Math.round(option.distance * 1000),
        summary: option.summary,
        polyline: option.polyline || ''
      })
      searchResults.value = []
      searchKw.value = ''
    } else if (ctx.type === 'leg') {
      await planApi.saveLeg(planId, {
        fromItemId: ctx.fromItem.id,
        toItemId: ctx.toItem.id,
        mode: option.mode,
        duration: Math.round(option.duration * 60),
        distance: Math.round(option.distance * 1000),
        summary: option.summary,
        polyline: option.polyline || ''
      })
    }
    routeDialog.value = false
    routeContext.value = null
    routeOptions.value = []
    await loadDetail()
  } catch (e) {
    toast(e.message || '保存失败', 'error')
  }
}

// 直接添加（不选路线）——仅「添加新地点」场景
async function chooseDirectAdd() {
  const ctx = routeContext.value
  if (!ctx || ctx.type !== 'add') { routeDialog.value = false; routeContext.value = null; return }
  const target = ctx.target
  routeDialog.value = false
  routeContext.value = null
  await addToActiveDay(target)
}

// ---------- 编辑条目（改名称/备注） ----------
const editDialog = ref(false)
const editForm = ref({ id: null, name: '', note: '' })
function openEdit(item) {
  if (!ensureEditable()) return
  editForm.value = { id: item.id, name: item.name, note: item.note || '' }
  editDialog.value = true
}
async function saveEdit() {
  const { id, name, note } = editForm.value
  if (!name.trim()) return
  try {
    await planApi.updateItem(planId, id, { name: name.trim(), note: note.trim() })
    editDialog.value = false
    await loadDetail()
  } catch (e) {
    toast(e.message || '保存失败', 'error')
  }
}

async function deleteItem(item) {
  if (!ensureEditable()) return
  try {
    await planApi.deleteItem(planId, item.id)
    await loadDetail()
  } catch (e) {
    toast(e.message || '删除失败', 'error')
  }
}

// ---------- 拖拽排序（天内排序 + 跨天移动） ----------
// vuedraggable 跨组拖拽时：源组触发 change.removed，目标组触发 change.added；天内排序触发 change.moved。
// added 时更新条目归属的天，moved/added 都触发重排提交；removed 由目标组 added 兜底，不单独提交。
function onDayChange(evt, targetDayNo) {
  if (evt.added) {
    evt.added.element.dayNo = targetDayNo
    if (activeDay.value !== targetDayNo) activeDay.value = targetDayNo
  }
  if (evt.added || evt.moved) {
    submitReorder()
  }
}
async function submitReorder() {
  const hadLegs = (plan.value?.legs || []).length > 0
  const ds = days.value.map(d => ({ dayNo: d.dayNo, itemIds: d.items.map(i => i.id) }))
  try {
    await planApi.reorderItems(planId, ds, plan.value.version)
    await loadDetail()
    if (hadLegs) {
      toast('排序已改变，出行方案已清空，请重新选择')
    }
  } catch (e) {
    toast(e.message || '排序保存失败（可能已被他人修改，已刷新）', 'error')
    await loadDetail()
  }
}

// ---------- 协作控制 ----------
async function toggleLock() {
  try {
    await planApi.setLock(planId, !plan.value.collabLocked)
    await loadDetail()
  } catch (e) {
    toast(e.message || '操作失败', 'error')
  }
}

const deadlineDialog = ref(false)
const deadlineValue = ref('')
function openDeadline() {
  deadlineValue.value = plan.value?.collabDeadline?.slice(0, 16) || ''
  deadlineDialog.value = true
}
async function saveDeadline() {
  try {
    await planApi.setDeadline(planId, deadlineValue.value ? deadlineValue.value + ':00' : null)
    deadlineDialog.value = false
    await loadDetail()
  } catch (e) {
    toast(e.message || '设置失败', 'error')
  }
}

// ---------- 邀请 ----------
const invites = ref([])
// 详情弹窗展示：有效邀请排前、已撤销沉底（保留历史）
const sortedInvites = computed(() =>
  [...invites.value].sort((a, b) => (a.revoked === b.revoked ? 0 : a.revoked ? 1 : -1))
)
const inviteDialog = ref(false)
const inviteForm = ref({ role: 'editor', maxUses: 0, expiresAt: '' })

async function loadInvites() {
  if (!isOwner.value) return
  try {
    invites.value = await planApi.listInvites(planId)
  } catch (e) { /* 忽略 */ }
}

function openInvite() {
  inviteForm.value = { role: 'editor', maxUses: 0, expiresAt: '' }
  inviteDialog.value = true
}
async function createInvite() {
  try {
    const vo = await planApi.createInvite(planId, {
      role: inviteForm.value.role,
      maxUses: inviteForm.value.maxUses || 0,
      expiresAt: inviteForm.value.expiresAt ? inviteForm.value.expiresAt + ':00' : null
    })
    inviteDialog.value = false
    await loadInvites()
    try { await navigator.clipboard.writeText(vo.link) } catch (e) { /* 忽略 */ }
    toast(`邀请链接已生成并复制：${vo.link}`, 'success')
  } catch (e) {
    toast(e.message || '签发失败', 'error')
  }
}
async function revokeInvite(inviteId) {
  try {
    await planApi.revokeInvite(planId, inviteId)
    await loadInvites()
  } catch (e) {
    toast(e.message || '撤销失败', 'error')
  }
}

// ---------- 邀请详情（弹窗展示链接，避免不小心又要重新申请） ----------
const inviteDetailDialog = ref(false)
function openInviteDetail() {
  inviteDetailDialog.value = true
}
function isInviteExpired(iv) {
  return iv.expiresAt && new Date(iv.expiresAt).getTime() < Date.now()
}
function fmtInviteTime(iso) {
  if (!iso) return ''
  return iso.slice(0, 16).replace('T', ' ')
}
async function copyInviteLink(iv) {
  try {
    await navigator.clipboard.writeText(iv.link)
    toast('邀请链接已复制', 'success')
  } catch (e) {
    toast('复制失败，请手动复制', 'error')
  }
}

// ---------- 编辑邀请（详情框里改角色/次数/过期时间，token 不变、链接不变） ----------
const inviteEditDialog = ref(false)
const inviteEditForm = ref({ id: null, role: 'editor', maxUses: 0, expiresAt: '', usedCount: 0 })
function openInviteEdit(iv) {
  inviteEditForm.value = {
    id: iv.id,
    role: iv.role,
    maxUses: iv.maxUses || 0,
    expiresAt: iv.expiresAt ? iv.expiresAt.slice(0, 16) : '',
    usedCount: iv.usedCount || 0
  }
  inviteEditDialog.value = true
}
async function saveInviteEdit() {
  const { id, role, maxUses, expiresAt } = inviteEditForm.value
  try {
    await planApi.updateInvite(planId, id, {
      role,
      maxUses: maxUses || 0,
      expiresAt: expiresAt ? expiresAt + ':00' : null
    })
    inviteEditDialog.value = false
    await loadInvites()
    toast('邀请已更新', 'success')
  } catch (e) {
    toast(e.message || '更新失败', 'error')
  }
}
async function removeMember(userId) {
  try {
    await planApi.removeCollaborator(planId, userId)
    await loadDetail()
  } catch (e) {
    toast(e.message || '移除失败', 'error')
  }
}

// ---------- 实时动态流（纯实时版：只保留本次打开页面期间收到的事件，刷新即清空） ----------
const activities = ref([])
const MAX_ACTIVITIES = 50

function fmtTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

// 把事件映射成「谁 + 做了什么」的中文描述
function activityText(e) {
  const who = e.actorName || '有人'
  switch (e.type) {
    case 'item_add': return { who, text: `添加了景点「${e.itemName || '未命名地点'}」` }
    case 'item_update': return { who, text: `修改了景点「${e.itemName || '地点'}」` }
    case 'item_delete': return { who, text: `删除了景点「${e.itemName || '地点'}」` }
    case 'reorder': return { who, text: '调整了行程顺序' }
    case 'leg_save': return { who, text: '设置了出行方案' }
    case 'lock': return { who, text: e.locked ? '锁定了计划' : '解锁了计划' }
    case 'deadline': return { who, text: '设置了创作截止时间' }
    case 'collaborator_remove': return { who, text: '移除了一位成员' }
    case 'member_join': return { who, text: `加入了规划（${e.role === 'viewer' ? '查看者' : '编辑者'}）` }
    case 'plan_update': return { who, text: '更新了计划信息' }
    case 'plan_delete': return { who, text: '删除了计划' }
    default: return { who, text: '更新了计划' }
  }
}

function pushActivity(event) {
  const a = activityText(event)
  activities.value.push({
    key: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    who: a.who,
    text: a.text,
    time: fmtTime(event.timestamp),
    self: store.currentUser && event.actorId === store.currentUser.id
  })
  if (activities.value.length > MAX_ACTIVITIES) activities.value.shift()
}

// ---------- STOMP 订阅实时同步 ----------
let client = null
function connectWs() {
  const token = getToken()
  if (!token) return
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  client = new Client({
    brokerURL: `${protocol}://${location.host}/ws`,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/plan/${planId}`, (msg) => {
        try {
          const event = JSON.parse(msg.body)
          const isSelf = store.currentUser && event.actorId === store.currentUser.id
          pushActivity(event)             // 自己 + 他人的操作都进动态流
          if (!isSelf) loadDetail()        // 他人操作才重新拉详情（自己操作本地已处理）
        } catch (e) { /* 忽略解析失败 */ }
      })
    }
  })
  client.activate()
}

onUnmounted(() => {
  client?.deactivate()
})
</script>

<template>
  <div class="editor-page">
    <div v-if="loading" class="loading">加载中…</div>

    <template v-else-if="plan">
      <!-- 顶栏 -->
      <div class="topbar">
        <button class="back-btn" @click="router.push('/checklist')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
          返回列表
        </button>
        <div class="cover-upload" :class="{ owner: isOwner }" :title="isOwner ? '上传封面' : ''" @click="pickCover">
          <img v-if="plan.image" :src="plan.image" class="cover-thumb" alt="" />
          <div v-else class="cover-placeholder">🖼️</div>
          <div v-if="isOwner" class="cover-hover">{{ uploadingCover ? '上传中…' : '上传封面' }}</div>
          <input ref="coverInput" type="file" accept="image/*" hidden @change="onCoverChange" />
        </div>
        <div class="title-wrap">
          <h1 class="title">{{ plan.name }}</h1>
          <span class="dest">📍 {{ plan.destination }}</span>
        </div>
        <div class="top-actions">
          <span v-if="isLocked" class="lock-badge">🔒 已锁定</span>
        </div>
      </div>

      <!-- 三栏 -->
      <div class="editor-body">
        <!-- 地图 -->
        <div class="map-panel glass-panel">
          <!-- 搜索工具栏：搜索地点加入选中的天 -->
          <div v-if="canEdit" class="search-toolbar">
            <div class="search-target">
              <span class="target-label">添加到</span>
              <span class="target-day">第 {{ activeDay }} 天</span>
            </div>
            <div class="search-box">
              <input v-model="searchKw" class="search-input" placeholder="搜索地点，如「西湖」" @keyup.enter="doSearch" />
              <button class="search-btn" @click="doSearch">搜索</button>
            </div>
            <div v-if="searchResults.length" class="search-results">
              <div v-for="r in searchResults" :key="r.poiId || r.name" class="search-item">
                <div class="sr-info" title="点击在地图上查看" @click="focusSearchResult(r)">
                  <span class="sr-name">{{ r.name }}</span>
                  <span class="sr-addr">{{ r.address }}</span>
                </div>
                <button class="sr-add" @click.stop="addSearchResult(r)">＋ 添加</button>
              </div>
            </div>
          </div>
          <AmapMap ref="amapRef" :items="plan.items" :legs="plan.legs" :editable="canEdit && !isLocked" :destination="plan.destination" @select="handleMapSelect" />
        </div>

        <!-- 行程面板 -->
        <div class="itinerary-panel glass-panel">
          <div class="panel-title-row">
            <div class="panel-title">行程安排</div>
            <button v-if="canEdit" class="add-day-btn" @click="addDay">＋ 添加天</button>
          </div>
          <div v-if="!days.length" class="empty">暂无行程，在地图上方搜索地点加入</div>
          <div
            v-for="day in days"
            :key="day.dayNo"
            class="day-block"
            :class="{ active: day.dayNo === activeDay }"
          >
            <div class="day-header" @click="selectDay(day.dayNo)">
              <span class="day-num">{{ day.dayNo }}</span>
              <span class="day-title">第 {{ day.dayNo }} 天</span>
              <span class="day-count">{{ day.items.length }} 个地点</span>
              <span v-if="day.dayNo === activeDay" class="day-active-tag">编辑中</span>
              <button
                class="day-collapse"
                :title="collapsedDays.has(day.dayNo) ? '展开条目' : '收起条目'"
                @click.stop="toggleCollapse(day.dayNo)"
              >{{ collapsedDays.has(day.dayNo) ? '▸' : '▾' }}</button>
            </div>

            <template v-if="!collapsedDays.has(day.dayNo)">
              <draggable
                v-if="day.items.length"
                :list="day.items"
                item-key="id"
                group="plan-days"
                :disabled="!canEdit || isLocked"
                @change="(evt) => onDayChange(evt, day.dayNo)"
              >
                <template #item="{ element: item, index }">
                  <div class="item">
                    <div class="item-main">
                      <span class="item-order">{{ index + 1 }}</span>
                      <div class="item-info" @dblclick="openEdit(item)">
                        <span class="item-name">{{ item.name }}</span>
                        <span v-if="item.note" class="item-note">{{ item.note }}</span>
                      </div>
                      <button v-if="canEdit" class="item-edit" title="编辑" @click.stop="openEdit(item)">✎</button>
                      <button v-if="canEdit" class="item-del" title="删除" @click.stop="deleteItem(item)">✕</button>
                    </div>
                    <div v-if="legMap[item.id]" class="item-leg item-leg-has">
                      <span class="leg-text">{{ modeEmoji(legMap[item.id].mode) }} {{ legMap[item.id].summary }}</span>
                      <span v-if="canEdit" class="leg-modify" @click.stop="openLegSelect(item)">修改</span>
                    </div>
                    <div
                      v-else-if="canEdit && getNextItemOf(item)"
                      class="item-leg item-leg-empty"
                      @click.stop="openLegSelect(item)"
                    >
                      ＋ 请选择出行方案
                    </div>
                  </div>
                </template>
              </draggable>
              <div v-else class="day-empty">暂无地点，在地图上方搜索或点地图加入</div>
            </template>
          </div>
        </div>

        <!-- 协作栏 -->
        <div class="collab-panel glass-panel">
          <div class="panel-title">协作</div>

          <div class="collab-section">
            <div class="section-label">成员（{{ plan.collaborators?.length || 0 }}）</div>
            <div v-for="c in plan.collaborators" :key="c.userId" class="member">
              <span class="member-avatar">{{ (c.nickname || '?').charAt(0) }}</span>
              <span class="member-name">{{ c.nickname }}</span>
              <span class="member-role" :class="c.role">{{ c.role }}</span>
              <button v-if="isOwner && c.role !== 'owner'" class="member-remove" title="移除" @click="removeMember(c.userId)">✕</button>
            </div>
          </div>

          <div v-if="isOwner" class="collab-section">
            <div class="section-label">协作控制</div>
            <button class="ctl-btn" @click="toggleLock">
              {{ plan.collabLocked ? '🔓 解锁' : '🔒 锁定' }}
            </button>
            <button class="ctl-btn" @click="openDeadline">
              ⏰ 截止时间{{ plan.collabDeadline ? '：' + plan.collabDeadline.slice(0, 16).replace('T', ' ') : '（未设）' }}
            </button>
          </div>

          <div v-if="isOwner" class="collab-section">
            <div class="section-label">邀请协作</div>
            <button class="ctl-btn primary" @click="openInvite">＋ 生成邀请链接</button>
            <button class="ctl-btn" @click="openInviteDetail">📋 邀请详情</button>
          </div>

          <!-- 实时动态（纯实时，刷新清空） -->
          <div class="collab-section">
            <div class="section-label">实时动态</div>
            <div class="activity-list">
              <div v-if="!activities.length" class="activity-empty">暂无动态，等待协作…</div>
              <div v-for="a in activities" :key="a.key" class="activity" :class="{ self: a.self }">
                <span class="activity-avatar">{{ a.who.charAt(0) }}</span>
                <div class="activity-body">
                  <span class="activity-text"><b>{{ a.who }}</b> {{ a.text }}</span>
                  <span class="activity-time">{{ a.time }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 编辑条目对话框 -->
    <Teleport to="body">
      <div v-if="editDialog" class="dialog-overlay" @click.self="editDialog = false">
        <div class="dialog glass-panel">
          <div class="dialog-header"><h2 class="dialog-title">编辑地点</h2></div>
          <div class="dialog-body">
            <div class="field">
              <label class="field-label">地点名称 <span class="req">*</span></label>
              <input v-model="editForm.name" class="text-input" placeholder="地点名称" />
            </div>
            <div class="field">
              <label class="field-label">备注（选填）</label>
              <input v-model="editForm.note" class="text-input" placeholder="如「提前预约门票」" />
            </div>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" @click="editDialog = false">取消</button>
            <button class="btn-confirm" @click="saveEdit">保存</button>
          </div>
        </div>
      </div>

      <!-- 出行方案选择对话框 -->
      <div v-if="routeDialog" class="dialog-overlay" @click.self="routeDialog = false">
        <div class="dialog glass-panel">
          <div class="dialog-header"><h2 class="dialog-title">选择出行方案</h2></div>
          <div class="dialog-body">
            <p class="route-from">
              从「<b>{{ routeContext?.fromItem?.name }}</b>」前往「<b>{{ routeContext?.type === 'add' ? routeContext?.target?.name : routeContext?.toItem?.name }}</b>」
            </p>
            <div v-if="routeLoading" class="route-loading">正在计算路线…</div>
            <div v-else-if="routeOptions.length" class="route-options">
              <button v-for="opt in routeOptions" :key="opt.mode" class="route-option" @click="chooseRoute(opt)">
                <span class="route-mode">{{ opt.label }}</span>
                <span class="route-summary">{{ opt.summary }}</span>
                <span class="route-arrow">›</span>
              </button>
            </div>
            <div v-else class="route-empty">未找到可用路线，可直接添加</div>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" @click="routeDialog = false">取消</button>
            <button class="btn-cancel" @click="chooseDirectAdd">直接添加</button>
          </div>
        </div>
      </div>

      <!-- 截止时间对话框 -->
      <div v-if="deadlineDialog" class="dialog-overlay" @click.self="deadlineDialog = false">
        <div class="dialog glass-panel">
          <div class="dialog-header"><h2 class="dialog-title">设置创作截止时间</h2></div>
          <div class="dialog-body">
            <input v-model="deadlineValue" type="datetime-local" class="text-input" />
            <p class="coord-hint">到点后，editor 将无法再编辑（owner 不受限）</p>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" @click="deadlineDialog = false">取消</button>
            <button class="btn-confirm" @click="saveDeadline">保存</button>
          </div>
        </div>
      </div>

      <!-- 邀请对话框 -->
      <div v-if="inviteDialog" class="dialog-overlay" @click.self="inviteDialog = false">
        <div class="dialog glass-panel">
          <div class="dialog-header"><h2 class="dialog-title">生成邀请链接</h2></div>
          <div class="dialog-body">
            <div class="field">
              <label class="field-label">加入角色</label>
              <select v-model="inviteForm.role" class="text-input">
                <option value="editor">编辑者（可编辑行程）</option>
                <option value="viewer">查看者（只读）</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label">次数上限（0 = 不限）</label>
              <input v-model.number="inviteForm.maxUses" type="number" min="0" class="text-input" />
            </div>
            <div class="field">
              <label class="field-label">过期时间（选填）</label>
              <input v-model="inviteForm.expiresAt" type="datetime-local" class="text-input" />
            </div>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" @click="inviteDialog = false">取消</button>
            <button class="btn-confirm" @click="createInvite">生成并复制链接</button>
          </div>
        </div>
      </div>

      <!-- 邀请详情对话框 -->
      <div v-if="inviteDetailDialog" class="dialog-overlay" @click.self="inviteDetailDialog = false">
        <div class="dialog glass-panel invite-detail-dialog">
          <div class="dialog-header"><h2 class="dialog-title">邀请详情</h2></div>
          <div class="dialog-body invite-detail-body">
            <div v-if="!invites.length" class="route-empty">暂无邀请链接</div>
            <div v-for="iv in sortedInvites" :key="iv.id" class="invite-detail-item">
              <div class="invite-detail-top">
                <span class="invite-role">{{ iv.role === 'editor' ? '编辑者' : '查看者' }}</span>
                <span class="invite-uses">{{ iv.usedCount }}/{{ iv.maxUses || '∞' }} 已用</span>
                <span v-if="iv.revoked" class="invite-state">已撤销</span>
                <span v-else-if="isInviteExpired(iv)" class="invite-state">已过期</span>
                <span v-else class="invite-active">有效</span>
              </div>
              <div class="invite-detail-link">
                <span class="invite-link-text" :title="iv.link">{{ iv.link }}</span>
                <button class="invite-copy" @click="copyInviteLink(iv)">复制</button>
              </div>
              <div class="invite-detail-meta">
                <span v-if="iv.expiresAt">过期时间：{{ fmtInviteTime(iv.expiresAt) }}</span>
                <span v-else>永不过期</span>
              </div>
              <div v-if="!iv.revoked" class="invite-detail-actions">
                <button class="invite-edit" @click="openInviteEdit(iv)">编辑</button>
                <button class="invite-revoke" @click="revokeInvite(iv.id)">撤销此邀请</button>
              </div>
            </div>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" @click="inviteDetailDialog = false">关闭</button>
          </div>
        </div>
      </div>

      <!-- 邀请编辑对话框 -->
      <div v-if="inviteEditDialog" class="dialog-overlay" @click.self="inviteEditDialog = false">
        <div class="dialog glass-panel">
          <div class="dialog-header"><h2 class="dialog-title">编辑邀请</h2></div>
          <div class="dialog-body">
            <div class="field">
              <label class="field-label">加入角色</label>
              <select v-model="inviteEditForm.role" class="text-input">
                <option value="editor">编辑者（可编辑行程）</option>
                <option value="viewer">查看者（只读）</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label">次数上限（0 = 不限）</label>
              <input v-model.number="inviteEditForm.maxUses" type="number" min="0" class="text-input" />
              <p class="coord-hint">当前已使用 {{ inviteEditForm.usedCount }} 次，上限不能小于该值</p>
            </div>
            <div class="field">
              <label class="field-label">过期时间（留空 = 不过期）</label>
              <input v-model="inviteEditForm.expiresAt" type="datetime-local" class="text-input" />
            </div>
          </div>
          <div class="dialog-footer">
            <button class="btn-cancel" @click="inviteEditDialog = false">取消</button>
            <button class="btn-confirm" @click="saveInviteEdit">保存</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.editor-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 28px;
  overflow: hidden;
}
.loading { padding: 60px 0; text-align: center; color: var(--text-muted); }

.topbar { display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.back-btn {
  display: flex; align-items: center; gap: 6px; background: none;
  color: var(--text-secondary); font-size: 14px; padding: 8px 12px;
  border-radius: var(--radius-xs); transition: var(--transition); flex-shrink: 0;
}
.back-btn:hover { background: var(--bg-card-hover); color: var(--text-primary); }

.cover-upload {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid rgba(var(--overlay-rgb), 0.12);
  background: rgba(var(--overlay-rgb), 0.04);
}
.cover-upload.owner { cursor: pointer; }
.cover-upload.owner:hover { border-color: var(--accent-primary); }
.cover-thumb { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px;
}
.cover-hover {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0, 0, 0, 0.55); color: #fff;
  font-size: 11px; font-weight: 600; text-align: center; padding: 0 2px;
  opacity: 0; transition: var(--transition);
}
.cover-upload.owner:hover .cover-hover { opacity: 1; }

.title-wrap { flex: 1; min-width: 0; }
.title { font-size: 22px; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dest { font-size: 13px; color: var(--text-muted); }
.top-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.lock-badge { padding: 5px 12px; border-radius: var(--radius-pill); background: rgba(220, 38, 38, 0.15); color: #f87171; font-size: 12px; font-weight: 600; }

.editor-body { flex: 1; min-height: 0; display: flex; gap: 16px; }
.map-panel { flex: 1.3; min-width: 0; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.itinerary-panel { flex: 1; min-width: 0; padding: 16px; overflow-y: auto; }
.collab-panel { width: 260px; flex-shrink: 0; padding: 16px; overflow-y: auto; }

.panel-title { font-size: 15px; font-weight: 700; margin-bottom: 12px; }

/* 行程面板标题行 + 添加天 */
.panel-title-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.panel-title-row .panel-title { margin-bottom: 0; }
.add-day-btn {
  padding: 5px 14px; border-radius: var(--radius-pill);
  background: rgba(var(--accent-rgb), 0.14); color: var(--accent-primary);
  font-size: 12px; font-weight: 600; transition: var(--transition);
}
.add-day-btn:hover { background: rgba(var(--accent-rgb), 0.22); }

.day-empty { padding: 14px 12px; font-size: 12px; color: var(--text-muted); text-align: center; }

/* 搜索工具栏 */
.search-toolbar {
  display: flex; flex-direction: column; gap: 8px;
  flex-shrink: 0;
}
.search-target { display: flex; align-items: center; gap: 6px; }
.target-label { font-size: 12px; color: var(--text-muted); }
.target-day {
  padding: 3px 12px; border-radius: var(--radius-pill);
  background: var(--accent-gradient); color: #fff; font-size: 12px; font-weight: 700;
}
.search-box { display: flex; gap: 8px; }
.search-input {
  flex: 1; padding: 8px 12px; border-radius: var(--radius-xs);
  background: var(--dialog-input-bg); border: 1px solid var(--dialog-input-border);
  color: var(--text-primary); font-size: 13px; outline: none;
}
.search-input:focus { border-color: var(--accent-primary); }
.search-btn {
  padding: 8px 16px; border-radius: var(--radius-xs);
  background: var(--accent-gradient); color: #fff; font-size: 13px; white-space: nowrap;
}
.search-results {
  max-height: 200px; overflow-y: auto;
  border: 1px solid rgba(var(--overlay-rgb), 0.1); border-radius: var(--radius-xs);
  background: var(--dialog-bg);
}
.search-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 12px;
  border-bottom: 1px solid rgba(var(--overlay-rgb), 0.05); transition: var(--transition);
}
.search-item:hover { background: rgba(var(--overlay-rgb), 0.06); }
.search-item:last-child { border-bottom: none; }
.sr-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; cursor: pointer; }
.sr-name { font-size: 13px; font-weight: 600; }
.sr-addr { font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sr-add {
  color: var(--accent-primary); font-size: 12px; font-weight: 600; flex-shrink: 0;
  padding: 4px 12px; border-radius: var(--radius-pill);
  background: rgba(var(--accent-rgb), 0.14); border: none; cursor: pointer;
  transition: var(--transition);
}
.sr-add:hover { background: rgba(var(--accent-rgb), 0.24); }

/* 行程面板 */
.day-block {
  border-radius: var(--radius-sm);
  border: 1px solid rgba(var(--overlay-rgb), 0.08);
  margin-bottom: 12px;
  overflow: hidden;
}
.day-block.active { border-color: var(--accent-primary); }
.day-header {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; background: rgba(var(--overlay-rgb), 0.04); cursor: pointer;
  transition: var(--transition);
}
.day-header:hover { background: rgba(var(--overlay-rgb), 0.08); }
.day-num {
  width: 22px; height: 22px; border-radius: 50%;
  background: var(--accent-gradient); color: #fff;
  font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.day-title { font-size: 13px; font-weight: 700; }
.day-count { font-size: 11px; color: var(--text-muted); flex: 1; }
.day-active-tag {
  font-size: 10px; padding: 2px 8px; border-radius: var(--radius-pill);
  background: rgba(var(--accent-rgb), 0.18); color: var(--accent-primary); font-weight: 600;
}
.day-collapse {
  flex-shrink: 0;
  width: 20px; height: 20px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  background: rgba(var(--overlay-rgb), 0.08);
  color: var(--text-muted);
  font-size: 12px; line-height: 1;
  cursor: pointer;
  transition: var(--transition);
}
.day-collapse:hover { color: var(--accent-primary); background: rgba(var(--accent-rgb), 0.14); }

.item {
  display: flex; flex-direction: column;
  padding: 0; border-top: 1px solid rgba(var(--overlay-rgb), 0.04);
  background: var(--bg-card);
}
.item-main {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px;
}
.item-leg {
  padding: 5px 12px 8px 40px;
  font-size: 11px; color: var(--accent-soft);
  border-top: 1px dashed rgba(var(--overlay-rgb), 0.06);
}
.item-leg-has { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.leg-text { flex: 1; min-width: 0; }
.leg-modify {
  color: var(--text-muted); font-size: 11px; cursor: pointer; flex-shrink: 0;
  padding: 2px 8px; border-radius: var(--radius-pill);
  background: rgba(var(--overlay-rgb), 0.08); transition: var(--transition);
}
.leg-modify:hover { color: var(--accent-primary); background: rgba(var(--accent-rgb), 0.14); }
.item-leg-empty {
  color: var(--text-muted);
  cursor: pointer;
  transition: var(--transition);
}
.item-leg-empty:hover { color: var(--accent-primary); }
.item-order {
  width: 18px; height: 18px; border-radius: 50%;
  background: rgba(var(--overlay-rgb), 0.08); color: var(--text-secondary);
  font-size: 11px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.item-info { flex: 1; min-width: 0; cursor: grab; display: flex; flex-direction: column; gap: 2px; }
.item-name { font-size: 13px; font-weight: 600; }
.item-note { font-size: 11px; color: var(--text-muted); }
.item-edit { font-size: 13px; color: var(--text-muted); background: none; padding: 2px 4px; flex-shrink: 0; }
.item-edit:hover { color: var(--accent-primary); }
.item-del { font-size: 13px; color: var(--text-muted); background: none; padding: 2px 4px; flex-shrink: 0; }
.item-del:hover { color: var(--danger); }

/* 协作栏 */
.collab-section { margin-bottom: 20px; }
.section-label { font-size: 12px; font-weight: 700; color: var(--text-muted); margin-bottom: 10px; }

/* 实时动态流 */
.activity-list {
  max-height: 220px; overflow-y: auto;
  display: flex; flex-direction: column; gap: 2px;
}
.activity-empty { font-size: 12px; color: var(--text-muted); padding: 8px 0 12px; }
.activity {
  display: flex; align-items: flex-start; gap: 8px; padding: 7px 6px;
  border-radius: var(--radius-xs); animation: activity-in 0.3s ease;
}
.activity:hover { background: rgba(var(--overlay-rgb), 0.05); }
.activity.self { opacity: 0.72; }
.activity-avatar {
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--accent-gradient); color: #fff;
  font-size: 11px; font-weight: 700; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.activity-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.activity-text { font-size: 12px; line-height: 1.45; color: var(--text-secondary); word-break: break-word; }
.activity-text b { color: var(--text-primary); font-weight: 700; }
.activity-time { font-size: 10px; color: var(--text-muted); }
@keyframes activity-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
.member { display: flex; align-items: center; gap: 8px; padding: 8px 0; }
.member-avatar {
  width: 28px; height: 28px; border-radius: 50%;
  background: var(--accent-gradient); color: #fff;
  font-size: 13px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.member-name { flex: 1; min-width: 0; font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.member-role { font-size: 10px; padding: 2px 8px; border-radius: var(--radius-pill); background: rgba(var(--overlay-rgb), 0.08); color: var(--text-secondary); }
.member-role.owner { background: rgba(var(--accent-rgb), 0.18); color: var(--accent-primary); }
.member-remove { font-size: 12px; color: var(--text-muted); background: none; padding: 2px 4px; }
.member-remove:hover { color: var(--danger); }

.ctl-btn {
  display: block; width: 100%; text-align: left; padding: 9px 12px; margin-bottom: 8px;
  border-radius: var(--radius-xs); background: rgba(var(--overlay-rgb), 0.06);
  color: var(--text-secondary); font-size: 13px; transition: var(--transition);
}
.ctl-btn:hover { background: rgba(var(--overlay-rgb), 0.12); color: var(--text-primary); }
.ctl-btn.primary { background: var(--accent-gradient); color: #fff; }
.ctl-btn.primary:hover { opacity: 0.9; color: #fff; }

.invite-role { padding: 2px 8px; border-radius: var(--radius-pill); background: rgba(var(--accent-rgb), 0.15); color: var(--accent-primary); }
.invite-uses { color: var(--text-muted); }
.invite-state { color: var(--danger); font-size: 11px; }

/* 邀请详情弹窗 */
.invite-detail-body { max-height: 420px; overflow-y: auto; }
.invite-detail-item {
  border: 1px solid rgba(var(--overlay-rgb), 0.1);
  border-radius: var(--radius-sm);
  padding: 12px 14px; margin-bottom: 12px;
  background: rgba(var(--overlay-rgb), 0.04);
}
.invite-detail-item:last-child { margin-bottom: 0; }
.invite-detail-top { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.invite-active { color: #34d399; font-size: 11px; }
.invite-detail-link { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.invite-link-text {
  flex: 1; min-width: 0; font-size: 12px; color: var(--text-secondary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  padding: 7px 10px; border-radius: var(--radius-xs);
  background: rgba(var(--overlay-rgb), 0.06);
}
.invite-copy {
  flex-shrink: 0; padding: 6px 12px; border-radius: var(--radius-xs);
  background: rgba(var(--accent-rgb), 0.16); color: var(--accent-primary);
  font-size: 12px; font-weight: 600; border: none; cursor: pointer; transition: var(--transition);
}
.invite-copy:hover { background: rgba(var(--accent-rgb), 0.26); }
.invite-detail-meta { font-size: 11px; color: var(--text-muted); margin-bottom: 10px; }
.invite-revoke {
  padding: 5px 12px; border-radius: var(--radius-xs); font-size: 12px;
  color: var(--danger); background: rgba(220, 38, 38, 0.1); border: none; cursor: pointer;
  transition: var(--transition);
}
.invite-revoke:hover { background: rgba(220, 38, 38, 0.18); }
.invite-detail-actions { display: flex; gap: 8px; }
.invite-edit {
  padding: 5px 14px; border-radius: var(--radius-xs); font-size: 12px;
  color: var(--text-secondary); background: rgba(var(--overlay-rgb), 0.08);
  border: none; cursor: pointer; transition: var(--transition);
}
.invite-edit:hover { color: var(--accent-primary); background: rgba(var(--accent-rgb), 0.14); }

.dialog { width: 460px; }
.invite-detail-dialog { width: 640px; }
.coord-hint { font-size: 12px; color: var(--text-muted); margin-top: 12px; }

/* 出行方案弹窗 */
.route-from { font-size: 13px; color: var(--text-secondary); margin-bottom: 14px; }
.route-from b { color: var(--text-primary); font-weight: 700; }
.route-loading, .route-empty { font-size: 13px; color: var(--text-muted); padding: 24px 0; text-align: center; }
.route-options { display: flex; flex-direction: column; gap: 8px; }
.route-option {
  display: flex; align-items: center; gap: 12px; padding: 12px 14px;
  border-radius: var(--radius-sm); border: 1px solid rgba(var(--overlay-rgb), 0.1);
  background: rgba(var(--overlay-rgb), 0.04); transition: var(--transition);
  text-align: left;
}
.route-option:hover { border-color: var(--accent-primary); background: rgba(var(--accent-rgb), 0.08); }
.route-mode {
  font-size: 14px; font-weight: 700; color: var(--text-primary); flex-shrink: 0;
  min-width: 40px;
}
.route-summary { flex: 1; font-size: 13px; color: var(--text-secondary); }
.route-arrow { color: var(--text-muted); font-size: 16px; flex-shrink: 0; }

select.text-input option { background: var(--dialog-bg); color: var(--text-primary); }
.field { margin-bottom: 14px; }
.field-label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 6px; }
.req { color: var(--danger); }
.empty { color: var(--text-muted); font-size: 13px; padding: 20px 0; text-align: center; }
</style>
