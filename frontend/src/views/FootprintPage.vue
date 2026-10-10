<script setup>
import { computed, ref } from 'vue'
import { useAppStore } from '../stores/app'
import EChartsChinaMap from '../components/EChartsChinaMap.vue'
import CityLightDialog from '../components/CityLightDialog.vue'
import AdminConfirmDialog from '../components/admin/AdminConfirmDialog.vue'
import { useToast } from '../composables/useToast'
import { cityColorOf } from '../utils/cityColor'

const store = useAppStore()
const { show: toast } = useToast()

const RING_CIRCUMFERENCE = 2 * Math.PI * 42
const CITY_TARGET = 200

const ringDasharray = computed(() => {
  const ratio = Math.min(store.user.visitedCities / CITY_TARGET, 1)
  return `${ratio * RING_CIRCUMFERENCE} ${RING_CIRCUMFERENCE}`
})

const ringPercent = computed(() => {
  return Math.min(Math.round(store.user.visitedCities / CITY_TARGET * 100), 100)
})

// 点亮/修改对话框状态
const dialogState = ref(null)
const showAllCities = ref(false)
const mapRef = ref(null)
const removeTarget = ref(null) // 待确认取消点亮的目标城市

function handleCityClick(city) {
  const existing = store.visitedCityList.find(c =>
    (city.adcode && c.adcode === city.adcode) || c.name === city.name) || null
  dialogState.value = { city, existing }
}

function handleDialogCancel() {
  dialogState.value = null
}

function openLightDialog() {
  dialogState.value = { city: null, existing: null }
}

async function handleDialogConfirm(payload) {
  const { city, existing, records, color } = payload
  try {
    if (existing) {
      await store.updateVisitedCity({ ...existing, records, color })
      toast(`已更新 ${city.name} 的足迹`)
    } else {
      await store.addVisitedCity({ ...city, records, color })
      toast(`已点亮 ${city.name}`)
    }
    dialogState.value = null
  } catch (e) {
    toast(e.message || '操作失败，请重试', 'error')
  }
}

function askRemoveCity(city) {
  removeTarget.value = city
}

async function confirmRemove() {
  const city = removeTarget.value
  if (!city) return
  try {
    await store.removeVisitedCity(city)
    toast(`已取消点亮 ${city.name}`)
  } catch (e) {
    toast(e.message || '操作失败，请重试', 'error')
  } finally {
    removeTarget.value = null
  }
}
</script>

<template>
  <div class="footprint-page">
    <div class="page-header">
      <h1 class="page-title">足迹点亮</h1>
      <div class="header-actions">
        <button class="all-btn" @click="showAllCities = true">已点亮城市</button>
        <button class="light-btn" @click="openLightDialog">＋ 点亮城市（或图中放大选点）</button>
      </div>
    </div>

    <div class="footprint-content">
      <!-- 左侧地图 -->
      <div class="map-section">
        <div class="map-container glass-panel">
          <EChartsChinaMap ref="mapRef" :cities="store.visitedCityList" @cityClick="handleCityClick" />
        </div>
        <!-- 缩放控件 -->
        <div class="map-controls">
          <button class="map-ctrl-btn" title="放大" @click="mapRef?.handleZoomIn()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14"/><path d="M5 12h14"/></svg>
          </button>
          <button class="map-ctrl-btn" title="缩小" @click="mapRef?.handleZoomOut()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14"/></svg>
          </button>
          <div class="scroll-hint">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="7" y="2" width="10" height="20" rx="5"/><path d="M12 6v2"/></svg>
            滚轮缩放
          </div>
        </div>
      </div>

      <!-- 右侧信息面板 -->
      <div class="info-panel">
        <!-- 统计 -->
        <div class="stats-card glass-panel">
          <div class="ring-stat">
            <div class="ring">
              <svg viewBox="0 0 100 100">
                <!-- 底轨用内联 style 而不是 stroke 属性：SVG 的 presentation attribute 不认 var()，
                     写死成白色的话浅色主题下这一圈就彻底看不见了 -->
                <circle cx="50" cy="50" r="42" fill="none" stroke-width="8" style="stroke: var(--border-strong)"/>
                <circle cx="50" cy="50" r="42" fill="none" stroke="url(#ringGrad)" stroke-width="8" stroke-linecap="round"
                  :stroke-dasharray="ringDasharray"
                  transform="rotate(-90 50 50)"/>
                <defs>
                  <linearGradient id="ringGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                    <stop offset="0%" stop-color="#38bdf8"/>
                    <stop offset="100%" stop-color="#0ea5e9"/>
                  </linearGradient>
                </defs>
              </svg>
              <div class="ring-center">
                <span class="ring-percent">{{ ringPercent }}%</span>
              </div>
            </div>
            <div class="ring-label">已点亮城市</div>
            <div class="ring-number">{{ store.user.visitedCities }}</div>
          </div>

          <div class="stat-row">
            <div class="stat-inline">
              <span class="stat-num">{{ store.user.visitedProvinces }}</span>
              <span class="stat-lbl">累计省份</span>
            </div>
            <div class="stat-inline">
              <span class="stat-num">{{ store.user.totalDays }}</span>
              <span class="stat-lbl">旅行天数</span>
            </div>
          </div>
        </div>

        <!-- 最近点亮 -->
        <div class="timeline-card glass-panel">
          <h3 class="card-title">最近点亮</h3>
          <div v-if="store.visitedCityList.length" class="timeline-list">
            <div v-for="(city, i) in store.visitedCityList.slice(0, 5)" :key="i" class="timeline-item">
              <span class="tl-dot" :style="{ background: cityColorOf(city) }"></span>
              <div class="timeline-info">
                <span class="tl-city">{{ city.name }}</span>
                <span class="tl-country">{{ city.province }}</span>
              </div>
              <span v-if="city.lightDate" class="tl-date">{{ city.lightDate }}</span>
            </div>
          </div>
          <div v-else class="empty-tip">还没有点亮任何城市</div>
        </div>

        <!-- 所有点亮弹层 -->
        <div v-if="showAllCities" class="all-cities-overlay" @click.self="showAllCities = false">
          <div class="all-cities-panel glass-panel">
            <div class="all-cities-header">
              <h3>所有点亮的城市</h3>
              <button class="close-btn" @click="showAllCities = false">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18"/><path d="M6 6l12 12"/></svg>
              </button>
            </div>
            <div v-if="store.visitedCityList.length" class="all-cities-list">
              <div v-for="(city, i) in store.visitedCityList" :key="i" class="all-city-item">
                <div class="all-city-left">
                  <span class="all-city-emoji" :style="{ color: cityColorOf(city) }">{{ city.name.charAt(0) }}</span>
                  <div class="all-city-info">
                    <span class="all-city-name">{{ city.name }}</span>
                    <span class="all-city-province">{{ city.province }}</span>
                  </div>
                </div>
                <button class="remove-btn" @click="askRemoveCity(city)">取消点亮</button>
              </div>
            </div>
            <div v-else class="all-cities-empty">暂无点亮记录</div>
          </div>
        </div>
      </div>
    </div>

    <CityLightDialog
      v-if="dialogState"
      :city="dialogState.city"
      :existing="dialogState.existing"
      @confirm="handleDialogConfirm"
      @cancel="handleDialogCancel"
    />

    <AdminConfirmDialog
      :visible="!!removeTarget"
      danger
      title="取消点亮"
      confirm-text="确认取消点亮"
      :message="removeTarget ? `确认取消点亮「${removeTarget.name}」吗？\n该城市的点亮记录与游玩回忆将被删除，且不可恢复。` : ''"
      @cancel="removeTarget = null"
      @confirm="confirmRemove"
    />
  </div>
</template>

<style scoped>
.footprint-page {
  padding: 32px 40px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
  position: relative;
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.footprint-content {
  display: flex;
  gap: 24px;
  flex: 1;
  min-height: 0;
}

.map-section {
  flex: 1;
  min-height: 0;
  position: relative;
}

.map-controls {
  position: absolute;
  bottom: 16px;
  right: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  z-index: 10;
}

.map-ctrl-btn {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.1);
  color: #334155;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}
.map-ctrl-btn:hover {
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.15);
}
.map-ctrl-btn:active {
  transform: scale(0.95);
}

.scroll-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  padding: 4px 8px;
  border-radius: 4px;
  white-space: nowrap;
  pointer-events: none;
}

.map-container {
  width: 100%;
  height: 100%;
  border-radius: var(--radius);
  overflow: hidden;
  padding: 0;
}

.info-panel {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
}

.stats-card {
  padding: 24px;
}

.ring-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.ring {
  width: 120px;
  height: 120px;
  position: relative;
}

.ring svg {
  width: 100%;
  height: 100%;
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ring-percent {
  font-size: 22px;
  font-weight: 800;
  background: var(--accent-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.ring-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 8px;
}

.ring-number {
  font-size: 28px;
  font-weight: 800;
}

.stat-row {
  display: flex;
  justify-content: space-around;
  padding-top: 16px;
  border-top: 1px solid rgba(var(--overlay-rgb), 0.08);
}

.stat-inline {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-num {
  font-size: 20px;
  font-weight: 700;
}

.stat-lbl {
  font-size: 12px;
  color: var(--text-muted);
}

/* Timeline */
.timeline-card {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 14px;
}

.all-btn {
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  background: rgba(var(--overlay-rgb), 0.06);
  border: 1px solid rgba(125, 211, 252, 0.28);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  transition: var(--transition);
}
.all-btn:hover {
  background: rgba(var(--overlay-rgb), 0.12);
  border-color: rgba(125, 211, 252, 0.55);
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-tip {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
  font-size: 13px;
}

.light-btn {
  padding: 8px 18px;
  border-radius: var(--radius-pill);
  background: var(--accent-gradient);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  transition: var(--transition);
  box-shadow: 0 4px 16px rgba(56, 189, 248, 0.35);
}
.light-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(56, 189, 248, 0.5);
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.timeline-info {
  display: flex;
  flex-direction: column;
}

.tl-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tl-city {
  font-size: 14px;
  font-weight: 600;
}

.tl-country {
  font-size: 12px;
  color: var(--text-muted);
}

.tl-date {
  font-size: 11px;
  color: var(--text-muted);
  margin-left: auto;
  flex-shrink: 0;
}

/* All Cities Overlay */
.all-cities-overlay {
  position: fixed;
  inset: 0;
  background: rgba(5, 8, 30, 0.6);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 24px;
}

.all-cities-panel {
  width: 420px;
  max-width: 100%;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  background: var(--dialog-bg);
  border: 1px solid var(--dialog-border);
  box-shadow: var(--dialog-shadow);
  border-radius: var(--radius);
  overflow: hidden;
}

.all-cities-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid var(--dialog-divider);
}

.all-cities-header h3 {
  font-size: 17px;
  font-weight: 700;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--dialog-cancel-bg);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: var(--transition);
}
.close-btn:hover { background: var(--dialog-cancel-hover); color: var(--text-primary); }

.all-cities-list {
  overflow-y: auto;
  flex: 1;
  padding: 8px 0;
}

.all-city-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 22px;
  transition: var(--transition);
}
.all-city-item:hover {
  background: rgba(var(--overlay-rgb), 0.04);
}

.all-city-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.all-city-emoji {
  font-size: 24px;
}

.all-city-info {
  display: flex;
  flex-direction: column;
}

.all-city-name {
  font-size: 14px;
  font-weight: 600;
}

.all-city-province {
  font-size: 12px;
  color: var(--text-muted);
}

.remove-btn {
  padding: 5px 14px;
  border-radius: var(--radius-pill);
  background: rgba(248, 113, 113, 0.1);
  border: 1px solid rgba(248, 113, 113, 0.25);
  color: #f87171;
  font-size: 12px;
  font-weight: 600;
  transition: var(--transition);
  flex-shrink: 0;
}
.remove-btn:hover {
  background: rgba(248, 113, 113, 0.2);
}

.all-cities-empty {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
  font-size: 14px;
}

@media (max-width: 900px) {
  .footprint-page {
    padding: 24px;
  }
  .info-panel {
    width: 260px;
  }
}

</style>
