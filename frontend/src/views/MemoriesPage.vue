<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useAppStore } from '../stores/app'
import AdminConfirmDialog from '../components/admin/AdminConfirmDialog.vue'
import { useToast } from '../composables/useToast'

const store = useAppStore()
const { show: toast } = useToast()

// 全量展平的游玩记录（后端已按游玩时间倒序），每条 = 一次游玩
const memories = ref([])
const loading = ref(false)
const selectedMemory = ref(null)
const removeTarget = ref(null)

// 卡片基准尺寸
const BASE_W = 196
const BASE_H = 252

// ---------- 时间隧道参数 ----------
const FOCAL = 2.0        // 透视焦距（越小远近差越大）
const FAR = 5.0          // 前方最远可见深度（此处透明度归零）
const BEHIND = 0.6       // 掠过身后淡出的深度
const SPREAD = 300       // 最近卡片的螺旋横向偏移（px）
const GOLDEN = 2.39996   // 黄金角，卡片沿隧道螺旋错落
const SCROLL_SPEED = 0.012  // 滚轮每像素前进的深度
const AUTO_SPEED = 0.9   // 自动播放每秒前进的卡数

const canvasRef = ref(null)
const canvasW = ref(0)
const canvasH = ref(0)

// 相机在隧道中的深度（0 = 最早的足迹，向前增大走到最新）
const camZ = ref(0)
const autoPlay = ref(false)

const activeYear = ref('all')
const keyword = ref('')          // 搜索框里的内容（回车/点按钮才提交）
const appliedKeyword = ref('')   // 已提交、真正驱动请求的搜索词

// 记录唯一键：所属城市 + 其在 records 数组中的下标
function memKey(m) {
  return `${m.cityId}-${m.recordIndex}`
}

function decorate(m) {
  return {
    ...m,
    key: memKey(m),
    cover: m.photos && m.photos.length ? m.photos[0] : '',
    year: (m.date || '').slice(0, 4)
  }
}

// 全量年份：搜索结果只含命中子集，所以只在「无关键词」那次请求后刷新，
// 否则搜索时年份下拉的选项会跟着缩水
const years = ref([])

const visibleMemories = computed(() => {
  if (activeYear.value === 'all') return memories.value
  return memories.value.filter(m => m.year === activeYear.value)
})

const searchKeyword = computed(() => appliedKeyword.value)

// 提交搜索：回车或点「搜索」按钮
function doSearch() {
  const kw = keyword.value.trim()
  if (kw === appliedKeyword.value) return   // 同一个词重复提交不重复请求
  appliedKeyword.value = kw
  loadMemories()
}

// 清空搜索：× 按钮或 Esc
function clearSearch() {
  keyword.value = ''
  if (!appliedKeyword.value) return
  appliedKeyword.value = ''
  loadMemories()
}

function clearFilters() {
  keyword.value = ''
  activeYear.value = 'all'
  if (appliedKeyword.value) {
    appliedKeyword.value = ''
    loadMemories()
  }
}

const cityCount = computed(() => new Set(memories.value.map(m => m.cityId)).size)

const visibleCityCount = computed(() => new Set(visibleMemories.value.map(m => m.cityId)).size)

// 是否处于筛选状态（年份或关键词）
const filtered = computed(() => activeYear.value !== 'all' || !!searchKeyword.value)

const removeIsLast = computed(() => {
  const m = removeTarget.value
  if (!m) return false
  return !memories.value.some(x => x.cityId === m.cityId && x.recordIndex !== m.recordIndex)
})

// ---------- 时间隧道排布 ----------
// 卡片沿时间正序（最早在前）排成一条螺旋隧道，相机沿 Z 轴前进。
// 滚轮驱动 camZ：卡片由远（小、透明）→ 近（大、清晰）→ 擦身而过（淡出消失），
// 往回滚即可重现——模拟「从第一个足迹走到最后一个足迹」的时间流逝。

// 时间正序的卡片（后端按游玩时间倒序返回，这里反过来让最早的在最前）
const tunnelItems = computed(() => visibleMemories.value.slice().reverse())

const nodes = computed(() => {
  const items = tunnelItems.value
  if (!items.length) return []

  return items.map((m, i) => {
    const dz = i - camZ.value                      // 相对相机深度：>0 前方，<0 已掠过
    const s = FOCAL / (FOCAL + Math.max(dz, 0))    // 透视缩放，最近为 1
    const op = dz >= 0
      ? Math.max(0, Math.min(1, 1 - dz / FAR))     // 前方越远越透明
      : Math.max(0, Math.min(1, 1 + dz / BEHIND))  // 身后淡出直到消失
    const ang = i * GOLDEN                         // 螺旋错落
    return {
      m, i, dz, s, op,
      offx: Math.cos(ang) * SPREAD * s,            // 远处卡片向中心聚拢（隧道收敛）
      offy: Math.sin(ang) * SPREAD * s,
      behind: dz < -BEHIND,                        // 掠过身后太远就不渲染
      zIndex: Math.max(10, Math.round(1000 - dz * 10))  // 近处的在上层
    }
  })
})

function clampCamZ(v) {
  const n = tunnelItems.value.length
  if (!n) return 0
  return Math.max(-1, Math.min(n - 0.2, v))
}

// 正在经过（离相机最近）的那条足迹，用于 HUD 提示
const focusMemory = computed(() => {
  let best = null
  let bestD = Infinity
  for (const nd of nodes.value) {
    const d = Math.abs(nd.dz)
    if (d < bestD) { bestD = d; best = nd.m }
  }
  return best
})

// 时间进度 0→1（最早→最新）
const progress = computed(() => {
  const n = tunnelItems.value.length
  if (!n) return 0
  return Math.max(0, Math.min(1, camZ.value / (n - 1)))
})

function cardStyle(nd) {
  return {
    width: `${BASE_W}px`,
    height: `${BASE_H}px`,
    transform: `translate(-50%, -50%) translate(${nd.offx.toFixed(1)}px, ${nd.offy.toFixed(1)}px) scale(${nd.s.toFixed(3)})`,
    opacity: +nd.op.toFixed(3),
    zIndex: nd.zIndex
  }
}

// ---------- 自动播放（缓缓前进） ----------
let autoRaf = null
let autoLast = 0

function autoTick(ts) {
  if (!autoPlay.value) {
    autoRaf = null
    return
  }
  if (autoLast) {
    camZ.value = clampCamZ(camZ.value + ((ts - autoLast) / 1000) * AUTO_SPEED)
  }
  autoLast = ts
  autoRaf = requestAnimationFrame(autoTick)
}

function toggleAuto() {
  autoPlay.value = !autoPlay.value
  if (autoPlay.value) {
    autoLast = 0
    autoRaf = requestAnimationFrame(autoTick)
  } else if (autoRaf) {
    cancelAnimationFrame(autoRaf)
    autoRaf = null
  }
}

// 切换年份 / 提交新搜索词时回到隧道起点（打字过程中不重置）
watch([activeYear, appliedKeyword], () => {
  autoPlay.value = false
  camZ.value = 0
})

// ---------- 星空背景（不随缩放变化，固定铺满画布） ----------
function mulberry32(a) {
  return function () {
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

const starfield = computed(() => {
  const w = canvasW.value
  const h = canvasH.value
  if (!w || !h) return { stars: [], web: [], nodes: [] }

  const rnd = mulberry32(20260911)
  const stars = []
  const count = Math.min(220, Math.round((w * h) / 6500))
  for (let i = 0; i < count; i++) {
    stars.push({
      x: +(rnd() * w).toFixed(1),
      y: +(rnd() * h).toFixed(1),
      r: +(0.6 + rnd() * 1.4).toFixed(2),
      o: +(0.28 + rnd() * 0.72).toFixed(2)
    })
  }

  // 星座网：左右两侧各一簇锚点，邻近的连细线
  const nodes = []
  const clusters = [
    { cx: 0.09, cy: 0.30, spread: 0.10, n: 8 },
    { cx: 0.92, cy: 0.58, spread: 0.11, n: 8 }
  ]
  for (const cl of clusters) {
    for (let i = 0; i < cl.n; i++) {
      nodes.push({
        x: +((cl.cx + (rnd() - 0.5) * 2 * cl.spread) * w).toFixed(1),
        y: +((cl.cy + (rnd() - 0.5) * 2 * cl.spread) * h).toFixed(1),
        r: +(1.1 + rnd() * 1.3).toFixed(2)
      })
    }
  }

  const web = []
  const maxD = Math.min(w, h) * 0.26
  for (let i = 0; i < nodes.length; i++) {
    for (let j = i + 1; j < nodes.length; j++) {
      const dx = nodes[i].x - nodes[j].x
      const dy = nodes[i].y - nodes[j].y
      const d = Math.hypot(dx, dy)
      if (d > 26 && d < maxD) {
        web.push({
          x1: nodes[i].x, y1: nodes[i].y, x2: nodes[j].x, y2: nodes[j].y,
          o: +(0.28 * (1 - d / maxD)).toFixed(3)
        })
      }
    }
  }
  return { stars, web, nodes }
})

let ro = null
function setupObserver(el) {
  if (ro) {
    ro.disconnect()
    ro = null
  }
  if (!el) return
  ro = new ResizeObserver(entries => {
    const r = entries[0].contentRect
    canvasW.value = r.width
    canvasH.value = r.height
  })
  ro.observe(el)
  canvasW.value = el.clientWidth
  canvasH.value = el.clientHeight
}
watch(canvasRef, setupObserver)
onBeforeUnmount(() => {
  ro?.disconnect()
  if (autoRaf) cancelAnimationFrame(autoRaf)
})

let loadSeq = 0

async function loadMemories() {
  const seq = ++loadSeq
  loading.value = true
  try {
    const list = await store.fetchMemories({ keyword: appliedKeyword.value })
    if (seq !== loadSeq) return                  // 丢弃过期响应
    memories.value = (list || []).map(decorate)
    if (!appliedKeyword.value) {                 // 只有全量那次才重算年份列表
      const set = new Set()
      memories.value.forEach(m => { if (m.year) set.add(m.year) })
      years.value = [...set].sort((a, b) => b.localeCompare(a))
    }
    if (activeYear.value !== 'all' && !years.value.includes(activeYear.value)) {
      activeYear.value = 'all'
    }
  } catch (e) {
    if (seq === loadSeq) toast(e.message || '加载旅行回忆失败', 'error')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

onMounted(loadMemories)

// ---------- 滚轮驱动时间前进 / 后退 ----------
// 向上滚（deltaY < 0）= 时间前进，向下滚 = 回看
function onWheel(e) {
  if (autoPlay.value) autoPlay.value = false   // 手动接管时暂停自动播放
  camZ.value = clampCamZ(camZ.value - e.deltaY * SCROLL_SPEED)
}

function resetTunnel() {
  autoPlay.value = false
  camZ.value = 0
}

function openDetail(m) {
  selectedMemory.value = m
}

// ---------- 删除单条游玩回忆 ----------
function askRemove(m) {
  removeTarget.value = m
}

async function confirmRemove() {
  const m = removeTarget.value
  if (!m) return
  try {
    const siblings = memories.value
      .filter(x => x.cityId === m.cityId)
      .sort((a, b) => a.recordIndex - b.recordIndex)
    const rest = siblings.filter(x => x.recordIndex !== m.recordIndex)

    if (!rest.length) {
      await store.removeVisitedCity({ id: m.cityId, name: m.cityName })
      toast(`已取消点亮 ${m.cityName}`)
    } else {
      await store.updateVisitedCity({
        id: m.cityId,
        name: m.cityName,
        province: m.province,
        adcode: m.adcode,
        lat: m.lat,
        lng: m.lng,
        color: m.color,
        lightDate: m.lightDate,
        records: rest.map(r => ({ rating: r.rating, date: r.date, photos: r.photos || [] }))
      })
      toast('已删除这条游玩回忆')
    }
    selectedMemory.value = null
    await loadMemories()
  } catch (e) {
    toast(e.message || '操作失败，请重试', 'error')
  } finally {
    removeTarget.value = null
  }
}

// ---------- 详情照片轮播 ----------
const currentPhoto = ref(0)
const memoryPhotoList = computed(() => selectedMemory.value?.photos || [])
watch(memoryPhotoList, () => { currentPhoto.value = 0 })

function prevPhoto() {
  if (!memoryPhotoList.value.length) return
  currentPhoto.value = (currentPhoto.value - 1 + memoryPhotoList.value.length) % memoryPhotoList.value.length
}
function nextPhoto() {
  if (!memoryPhotoList.value.length) return
  currentPhoto.value = (currentPhoto.value + 1) % memoryPhotoList.value.length
}
</script>

<template>
  <div class="memories-page">
    <!-- 时间隧道视图 -->
    <template v-if="!selectedMemory">
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">旅行回忆</h1>
          <span v-if="filtered" class="page-sub">
            找到 {{ visibleMemories.length }} 条游玩记录 · {{ visibleCityCount }} 个目的地
          </span>
          <span v-else class="page-sub">共 {{ memories.length }} 条游玩记录 · {{ cityCount }} 个目的地</span>
        </div>

        <div class="header-controls">
          <div class="mem-search">
            <div class="mem-search-field">
              <svg class="mem-search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                <circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/>
              </svg>
              <input
                v-model="keyword"
                class="mem-search-input"
                type="text"
                placeholder="搜索城市或省份，回车确认…"
                @keydown.enter="doSearch"
                @keydown.esc="clearSearch"
              />
              <button
                v-if="keyword.trim()"
                class="mem-search-clear"
                title="清除"
                @click="clearSearch"
              >×</button>
            </div>
            <button class="mem-search-btn" @click="doSearch">搜索</button>
          </div>

          <div v-if="years.length" class="year-select-wrap">
            <select v-model="activeYear" class="year-select">
              <option value="all">全部年份</option>
              <option v-for="y in years" :key="y" :value="y">{{ y }} 年</option>
            </select>
            <span class="year-select-arrow">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M6 9l6 6 6-6"/></svg>
            </span>
          </div>

          <div class="zoom-box">
            <button class="auto-toggle" :class="{ active: autoPlay }" @click="toggleAuto">
              {{ autoPlay ? '⏸ 暂停' : '▶ 自动播放' }}
            </button>
            <button class="zoom-reset" @click="resetTunnel">回到起点</button>
          </div>
        </div>
      </div>

      <div
        ref="canvasRef"
        class="memories-canvas"
        @wheel.prevent="onWheel"
        @dragstart.prevent
      >
        <svg class="starfield" :width="canvasW" :height="canvasH" aria-hidden="true">
          <line
            v-for="(l, i) in starfield.web"
            :key="'w' + i"
            :x1="l.x1" :y1="l.y1" :x2="l.x2" :y2="l.y2"
            :stroke-opacity="l.o"
            class="web-line"
          />
          <circle
            v-for="(n, i) in starfield.nodes"
            :key="'n' + i"
            :cx="n.x" :cy="n.y" :r="n.r"
            class="web-node"
          />
          <circle
            v-for="(s, i) in starfield.stars"
            :key="'s' + i"
            :cx="s.x" :cy="s.y" :r="s.r"
            :fill-opacity="s.o"
            class="star-dot"
            :class="{ 'star-bright': s.r > 1.6 }"
          />
        </svg>

        <article
          v-for="nd in nodes"
          v-show="!nd.behind"
          :key="nd.m.key"
          class="memory-card"
          :style="cardStyle(nd)"
          @click="openDetail(nd.m)"
        >
          <img v-if="nd.m.cover" class="card-img" :src="nd.m.cover" alt="" />
          <div v-else class="card-img card-img-fallback">
            <span>{{ nd.m.cityName.charAt(0) }}</span>
          </div>

          <button class="card-del" title="删除这条回忆" @click.stop="askRemove(nd.m)">×</button>
          <span v-if="nd.m.photos && nd.m.photos.length" class="photo-badge">📷 {{ nd.m.photos.length }}</span>

          <div class="card-overlay">
            <h3 class="card-name">{{ nd.m.cityName }}</h3>
            <p class="card-sub">📍 {{ nd.m.province }}</p>
            <div class="card-foot">
              <span class="card-date">{{ nd.m.date }}</span>
              <span v-if="nd.m.rating > 0" class="card-stars">
                <span v-for="s in 5" :key="s" :class="{ off: s > nd.m.rating }">★</span>
              </span>
            </div>
          </div>
        </article>

        <div v-if="focusMemory" class="tunnel-hud">
          <span class="hud-year">{{ focusMemory.year }} 年</span>
          <span class="hud-city">{{ focusMemory.cityName }}</span>
          <span class="hud-date">{{ focusMemory.date }}</span>
          <span class="hud-progress">{{ Math.round(progress * 100) }}%</span>
        </div>

        <div v-if="loading && !nodes.length" class="empty-state">
          <span class="empty-icon">⏳</span>
          <p>加载中…</p>
        </div>

        <div v-else-if="!nodes.length && searchKeyword" class="empty-state">
          <span class="empty-icon">🔍</span>
          <p v-if="activeYear !== 'all'">没有找到「{{ searchKeyword }}」在 {{ activeYear }} 年的游玩记录</p>
          <p v-else>没有找到与「{{ searchKeyword }}」相关的地点</p>
          <button class="link-btn" @click="clearFilters">清除筛选</button>
        </div>

        <div v-else-if="!nodes.length && activeYear !== 'all'" class="empty-state">
          <span class="empty-icon">🔍</span>
          <p>{{ activeYear }} 年还没有游玩记录</p>
          <button class="link-btn" @click="clearFilters">查看全部年份</button>
        </div>

        <div v-else-if="!nodes.length" class="empty-state">
          <span class="empty-icon">🗺️</span>
          <p>还没有点亮任何城市</p>
          <p class="empty-hint">去「足迹点亮」开启你的旅行回忆吧</p>
        </div>
      </div>
    </template>

    <!-- 单条回忆详情 -->
    <template v-else>
      <div class="detail-topbar">
        <button class="back-btn" @click="selectedMemory = null">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
          返回隧道
        </button>
      </div>

      <div class="detail-layout">
        <div class="detail-photos glass-panel">
          <div v-if="memoryPhotoList.length" class="detail-cover">
            <img :src="memoryPhotoList[currentPhoto]" alt="" />
            <button v-if="memoryPhotoList.length > 1" class="photo-arrow prev" @click="prevPhoto">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
            </button>
            <button v-if="memoryPhotoList.length > 1" class="photo-arrow next" @click="nextPhoto">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
            </button>
            <span v-if="memoryPhotoList.length > 1" class="photo-counter">{{ currentPhoto + 1 }} / {{ memoryPhotoList.length }}</span>
          </div>
          <div v-else class="detail-cover-placeholder">
            <span>{{ selectedMemory.cityName.charAt(0) }}</span>
          </div>
          <div v-if="memoryPhotoList.length > 1" class="detail-thumbs">
            <button
              v-for="(photo, pi) in memoryPhotoList"
              :key="pi"
              class="detail-thumb"
              :class="{ active: pi === currentPhoto }"
              @click="currentPhoto = pi"
            >
              <img :src="photo" alt="" />
            </button>
          </div>
        </div>

        <div class="detail-info">
          <div class="detail-header glass-panel">
            <span class="detail-emoji">{{ selectedMemory.cityName.charAt(0) }}</span>
            <div class="detail-title-wrap">
              <h1 class="detail-name">{{ selectedMemory.cityName }}</h1>
              <p class="detail-province">📍 {{ selectedMemory.province }}</p>
            </div>
          </div>

          <div class="detail-stats glass-panel">
            <div class="stat-item">
              <span class="stat-label">游玩时间</span>
              <span class="stat-value">{{ selectedMemory.date }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">点亮日期</span>
              <span class="stat-value">{{ selectedMemory.lightDate || '未知' }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">照片数量</span>
              <span class="stat-value">{{ memoryPhotoList.length }}</span>
            </div>
          </div>

          <div class="detail-rating glass-panel">
            <h3 class="section-title">推荐指数</h3>
            <div v-if="selectedMemory.rating > 0" class="rating-display">
              <span class="rating-number">{{ selectedMemory.rating }}.0</span>
              <div class="rating-stars">
                <span v-for="s in 5" :key="s" class="star" :class="{ filled: s <= selectedMemory.rating }">★</span>
              </div>
            </div>
            <p v-else class="no-rating">暂无评分</p>
          </div>

          <button class="remove-city-btn" @click="askRemove(selectedMemory)">删除这条回忆</button>
        </div>
      </div>
    </template>

    <AdminConfirmDialog
      :visible="!!removeTarget"
      danger
      title="删除回忆"
      :confirm-text="removeIsLast ? '确认取消点亮' : '确认删除'"
      :message="removeTarget
        ? (removeIsLast
            ? `这是「${removeTarget.cityName}」的最后一条游玩记录，删除后该城市将取消点亮，记录不可恢复。`
            : `确认删除「${removeTarget.cityName}」${removeTarget.date} 的这条游玩回忆吗？此操作不可恢复。`)
        : ''"
      @cancel="removeTarget = null"
      @confirm="confirmRemove"
    />
  </div>
</template>

<style scoped>
.memories-page {
  padding: 28px 36px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
}

.page-sub {
  font-size: 14px;
  color: var(--text-muted);
}

/* Header controls */
.header-controls {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

/* 地点搜索框 */
.mem-search {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.mem-search-field {
  position: relative;
  display: flex;
  align-items: center;
}

.mem-search-btn {
  padding: 7px 16px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(125, 211, 252, 0.28);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  transition: var(--transition);
}
.mem-search-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(125, 211, 252, 0.55);
}

.mem-search-icon {
  position: absolute;
  left: 12px;
  color: var(--text-muted);
  pointer-events: none;
}

.mem-search-input {
  width: 190px;
  padding: 7px 30px 7px 34px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(125, 211, 252, 0.28);
  color: var(--text-primary);
  font-size: 13px;
  transition: var(--transition);
}
.mem-search-input::placeholder {
  color: var(--text-muted);
}
.mem-search-input:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(125, 211, 252, 0.55);
}
.mem-search-input:focus {
  outline: none;
  border-color: var(--accent-blue);
  box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.18);
}

.mem-search-clear {
  position: absolute;
  right: 8px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.10);
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1;
  transition: var(--transition);
}
.mem-search-clear:hover {
  background: rgba(255, 255, 255, 0.20);
  color: var(--text-primary);
}

/* 自动播放 + 回到起点 */
.zoom-box {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.auto-toggle {
  padding: 7px 16px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(125, 211, 252, 0.28);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  transition: var(--transition);
}
.auto-toggle:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(125, 211, 252, 0.55);
}
.auto-toggle.active {
  background: rgba(52, 211, 153, 0.16);
  border-color: rgba(52, 211, 153, 0.55);
  color: var(--success);
}

.zoom-reset {
  font-size: 12px;
  color: var(--text-muted);
  background: none;
  padding: 6px 10px;
  border-radius: var(--radius-xs);
  transition: var(--transition);
}
.zoom-reset:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-primary);
}

/* 年份下拉框 */
.year-select-wrap {
  position: relative;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.year-select {
  appearance: none;
  -webkit-appearance: none;
  padding: 7px 34px 7px 14px;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(125, 211, 252, 0.28);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}
.year-select:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(125, 211, 252, 0.55);
}
.year-select:focus {
  outline: none;
  border-color: var(--accent-blue);
  box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.18);
}
.year-select option {
  background: #0b1220;
  color: var(--text-primary);
}

.year-select-arrow {
  position: absolute;
  right: 12px;
  display: flex;
  align-items: center;
  color: var(--accent-blue);
  pointer-events: none;
}

/* 时间隧道画布 */
.memories-canvas {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border-radius: var(--radius);
  cursor: default;
  user-select: none;
  background:
    radial-gradient(ellipse 62% 72% at 46% 44%, rgba(56, 189, 248, 0.10), transparent 70%),
    radial-gradient(ellipse 40% 50% at 88% 66%, rgba(167, 139, 250, 0.10), transparent 70%),
    radial-gradient(ellipse 34% 44% at 10% 26%, rgba(56, 189, 248, 0.08), transparent 70%),
    radial-gradient(ellipse 90% 90% at 50% 50%, #0a1020 30%, #060a14 100%);
}

.starfield {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 0;
  pointer-events: none;
}

.star-dot {
  fill: #dbeafe;
}

.star-bright {
  fill: #f0f9ff;
  filter: drop-shadow(0 0 3px rgba(186, 230, 253, 0.95));
}

.web-line {
  stroke: #7dd3fc;
  stroke-width: 0.7;
}

.web-node {
  fill: #bae6fd;
  filter: drop-shadow(0 0 3px rgba(125, 211, 252, 0.9));
}

/* 卡片绝对定位到画布中心，位置/尺寸/透明度/层级全部由 inline style（投影）驱动 */
.memory-card {
  position: absolute;
  left: 50%;
  top: 50%;
  overflow: hidden;
  cursor: pointer;
  background: #0b1220;
  border: 1px solid rgba(56, 189, 248, 0.35);
  border-radius: 14px;
  box-shadow: 0 0 18px rgba(56, 189, 248, 0.16), 0 8px 20px rgba(0, 0, 0, 0.45);
  font-size: 15px;
  transition: box-shadow 0.22s ease, border-color 0.22s ease;
}
.memory-card:hover {
  translate: 0 -4px;
  border-color: rgba(125, 211, 252, 0.75);
  box-shadow: 0 0 26px rgba(56, 189, 248, 0.38), 0 12px 26px rgba(0, 0, 0, 0.5);
}

.card-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-img-fallback {
  background: linear-gradient(135deg, #10b981, #14b8a6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3.73em;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.9);
}

.card-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 26px 12px 11px;
  background: linear-gradient(to top, rgba(4, 8, 18, 0.94) 40%, rgba(4, 8, 18, 0.6) 70%, transparent);
}

.card-name {
  font-size: 1em;
  font-weight: 800;
  color: #fff;
  margin-bottom: 2px;
}

.card-sub {
  font-size: 0.733em;
  color: rgba(226, 232, 240, 0.72);
}

.card-foot {
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.card-date {
  font-size: 0.7em;
  color: #7dd3fc;
  font-variant-numeric: tabular-nums;
}

.card-stars {
  font-size: 0.667em;
  color: #fbbf24;
  letter-spacing: 0.5px;
}
.card-stars .off {
  color: rgba(255, 255, 255, 0.22);
}

.card-del {
  position: absolute;
  right: 7px;
  top: 7px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  color: white;
  font-size: 16px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: var(--transition);
  z-index: 3;
}
.memory-card:hover .card-del {
  opacity: 1;
}
.card-del:hover {
  background: rgba(248, 113, 113, 0.85);
}

.photo-badge {
  position: absolute;
  left: 8px;
  top: 8px;
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  color: white;
  font-size: 10px;
  font-variant-numeric: tabular-nums;
  z-index: 3;
}

/* 隧道进度 HUD */
.tunnel-hud {
  position: absolute;
  left: 50%;
  bottom: 22px;
  transform: translateX(-50%);
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 18px;
  border-radius: var(--radius-pill);
  background: rgba(6, 10, 20, 0.72);
  border: 1px solid rgba(125, 211, 252, 0.28);
  backdrop-filter: blur(10px);
  pointer-events: none;
}
.hud-year {
  font-size: 13px;
  font-weight: 700;
  color: #7dd3fc;
}
.hud-city {
  font-size: 15px;
  font-weight: 800;
  color: #fff;
}
.hud-date {
  font-size: 13px;
  color: rgba(226, 232, 240, 0.8);
  font-variant-numeric: tabular-nums;
}
.hud-progress {
  font-size: 12px;
  font-weight: 600;
  color: var(--success);
  font-variant-numeric: tabular-nums;
}

/* Empty */
.empty-state {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-muted);
}

.empty-icon {
  font-size: 48px;
}

.empty-hint {
  font-size: 13px;
  color: var(--text-muted);
}

.link-btn {
  background: none;
  color: var(--accent-blue);
  font-size: 13px;
  text-decoration: underline;
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

.detail-layout {
  flex: 1;
  display: flex;
  gap: 20px;
  min-height: 0;
}

.detail-photos {
  flex: 1;
  border-radius: var(--radius);
  overflow-y: auto;
  min-width: 0;
}

.detail-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
}
.detail-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.detail-cover-placeholder {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: linear-gradient(135deg, #10b981, #14b8a6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.9);
}

.photo-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(8px);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
  cursor: pointer;
}
.photo-arrow.prev { left: 12px; }
.photo-arrow.next { right: 12px; }
.photo-arrow:hover { background: rgba(0, 0, 0, 0.65); }

.photo-counter {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  color: white;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.detail-thumbs {
  display: flex;
  gap: 8px;
  padding: 12px;
  overflow-x: auto;
}

.detail-thumb {
  flex-shrink: 0;
  width: 72px;
  height: 54px;
  border-radius: var(--radius-xs);
  overflow: hidden;
  padding: 0;
  border: 2px solid transparent;
  background: none;
  cursor: pointer;
  opacity: 0.6;
  transition: var(--transition);
}
.detail-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.detail-thumb.active {
  border-color: var(--accent-blue);
  opacity: 1;
}

.detail-info {
  width: 340px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  border-radius: var(--radius);
}

.detail-emoji {
  font-size: 36px;
}

.detail-title-wrap {
  flex: 1;
}

.detail-name {
  font-size: 22px;
  font-weight: 800;
}

.detail-province {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

.detail-stats {
  padding: 16px 20px;
  border-radius: var(--radius);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
}

.detail-rating {
  padding: 18px 20px;
  border-radius: var(--radius);
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 14px;
}

.rating-display {
  display: flex;
  align-items: center;
  gap: 14px;
}

.rating-number {
  font-size: 36px;
  font-weight: 800;
  color: #fbbf24;
}

.rating-stars {
  display: flex;
  gap: 3px;
}

.star {
  font-size: 20px;
  color: var(--text-muted);
}
.star.filled {
  color: #fbbf24;
}

.no-rating {
  color: var(--text-muted);
  font-size: 13px;
}

.remove-city-btn {
  padding: 10px 0;
  border-radius: var(--radius-pill);
  background: rgba(248, 113, 113, 0.1);
  border: 1px solid rgba(248, 113, 113, 0.25);
  color: var(--danger);
  font-size: 13px;
  font-weight: 600;
  transition: var(--transition);
}
.remove-city-btn:hover {
  background: rgba(248, 113, 113, 0.2);
}

@media (max-width: 1100px) {
  .detail-layout {
    flex-direction: column;
  }
  .detail-info {
    width: 100%;
  }
}

@media (max-width: 700px) {
  .memories-page {
    padding: 20px;
  }
}
</style>
