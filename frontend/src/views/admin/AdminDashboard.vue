<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '../../api'
import { useToast } from '../../composables/useToast'
import StatCard from '../../components/StatCard.vue'
import { formatDateTime } from '../../utils/format'

const { show: toast } = useToast()
const overview = ref(null)
const loading = ref(true)

const d = computed(() => overview.value || {})
const trend = computed(() => (d.value.trend && Array.isArray(d.value.trend)) ? d.value.trend : [])
const hotCities = computed(() => (d.value.hotCities && Array.isArray(d.value.hotCities)) ? d.value.hotCities : [])
const hotNotes = computed(() => (d.value.hotNotes && Array.isArray(d.value.hotNotes)) ? d.value.hotNotes : [])

const maxTrend = computed(() => Math.max(1, ...trend.value.map(t => Number(t.noteCount) || 0)))
const maxCity = computed(() => Math.max(1, ...hotCities.value.map(c => Number(c.count) || 0)))

function mdLabel(dateStr) {
  if (!dateStr) return ''
  const s = String(dateStr)
  return s.length >= 10 ? s.slice(5, 10).replace('-', '/') : s
}

function barHeight(n) {
  return `${Math.max(Number(n) > 0 ? 8 : 3, (Number(n) / maxTrend.value) * 100)}%`
}

function hbarWidth(n) {
  return `${Math.max(6, (Number(n) / maxCity.value) * 100)}%`
}

async function load() {
  loading.value = true
  try {
    overview.value = await adminApi.getOverview()
  } catch (e) {
    toast(e.message || '概览数据加载失败', 'error')
    overview.value = null
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<template>
  <div class="admin-page">
    <div>
      <h1 class="admin-page-title">数据概览</h1>
      <p class="admin-page-sub">平台整体运营情况一览</p>
    </div>

    <div v-if="loading" class="admin-panel glass-panel">
      <div class="admin-loading">加载中…</div>
    </div>

    <template v-else-if="overview">
      <div class="dash-kpis">
        <StatCard label="注册用户" :value="d.userCount ?? 0" unit="人" />
        <StatCard label="社区笔记" :value="d.noteCount ?? 0" unit="篇" />
        <StatCard label="建议反馈" :value="d.feedbackCount ?? 0" unit="条" />
        <StatCard label="待处理反馈" :value="d.pendingFeedbackCount ?? 0" unit="条" />
      </div>

      <div class="dash-grid">
        <section class="admin-panel glass-panel">
          <h2 class="panel-title">近 7 天新增笔记</h2>
          <div v-if="trend.length" class="trend-bars">
            <div v-for="(t, i) in trend" :key="i" class="trend-col" :title="`${t.date} 新增 ${t.noteCount ?? 0} 篇`">
              <span class="trend-val">{{ t.noteCount ?? 0 }}</span>
              <div class="trend-track">
                <div class="trend-fill" :style="{ height: barHeight(t.noteCount) }"></div>
              </div>
              <span class="trend-label">{{ mdLabel(t.date) }}</span>
            </div>
          </div>
          <div v-else class="admin-empty">暂无趋势数据</div>
        </section>

        <section class="admin-panel glass-panel">
          <h2 class="panel-title">热门城市 Top</h2>
          <div v-if="hotCities.length" class="hbar-list">
            <div v-for="(c, i) in hotCities" :key="i" class="hbar-row">
              <span class="hbar-name" :title="c.name">{{ c.name }}</span>
              <div class="hbar-track">
                <div class="hbar-fill" :style="{ width: hbarWidth(c.count) }"></div>
              </div>
              <span class="hbar-count">{{ c.count }}</span>
            </div>
          </div>
          <div v-else class="admin-empty">暂无点亮记录</div>
        </section>
      </div>

      <section class="admin-panel glass-panel dash-full">
        <h2 class="panel-title">社区点赞榜 Top 5</h2>
        <div v-if="hotNotes.length" class="rank-list">
          <div v-for="(n, i) in hotNotes" :key="n.id" class="rank-item">
            <span class="rank-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
            <div class="rank-main">
              <span class="rank-title" :title="n.title">{{ n.title }}</span>
              <span class="rank-sub">{{ n.author }} · {{ formatDateTime(n.createdAt) }}</span>
            </div>
            <span class="rank-like">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
              {{ (n.likes ?? 0).toLocaleString() }}
            </span>
          </div>
        </div>
        <div v-else class="admin-empty">暂无笔记</div>
      </section>
    </template>
  </div>
</template>
