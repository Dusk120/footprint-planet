<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '../stores/app'
import { loadCityPoints } from '../data/cities'
import { uploadApi } from '../api'
import { useToast } from '../composables/useToast'
import { CITY_COLORS, pickCityColor } from '../utils/cityColor'

const { show } = useToast()
const uploading = ref(false)
const MAX_SIZE = 10 * 1024 * 1024

const props = defineProps({
  // 点击的目标城市 { name, lng, lat, adcode, province, visited }；为空表示进入“选择城市”模式
  city: { type: Object, default: null },
  // 已点亮城市的完整数据（含 records）；为空表示新点亮
  existing: { type: Object, default: null }
})
const emit = defineEmits(['confirm', 'cancel'])

const store = useAppStore()

const selectedCity = ref(props.city)
const currentExisting = ref(props.existing)
const isEdit = computed(() => !!currentExisting.value)
const isSelecting = computed(() => !selectedCity.value)

// 城市主题色（城市级，与某一次游玩无关）
const color = ref(props.existing?.color || pickCityColor(props.city?.name))

// ---------- 选择城市模式 ----------
const searchQuery = ref('')
const cityList = ref([])
const filteredCities = computed(() => {
  const q = searchQuery.value.trim()
  const src = q ? cityList.value.filter(c => c.name.includes(q)) : cityList.value
  return src.slice(0, 120)
})

onMounted(async () => {
  if (isSelecting.value) cityList.value = await loadCityPoints()
})

function pickCity(c) {
  selectedCity.value = c
  const ex = store.visitedCityList.find(city =>
    (c.adcode && city.adcode === c.adcode) || city.name === c.name) || null
  currentExisting.value = ex
  // 对话框是 v-if 挂载的，连点不同城市不会重新挂载，颜色必须手动重置
  color.value = ex?.color || pickCityColor(c.name)
  buildRecords()
}

// ---------- 游玩记录表单 ----------
const records = ref([])

function emptyRecord() {
  return { rating: 0, date: '', photos: [] }
}

function buildRecords() {
  const base = currentExisting.value?.records?.length
    ? currentExisting.value.records.map(r => ({
        rating: typeof r.rating === 'number' ? r.rating : 0,
        date: r.date || '',
        photos: [...(r.photos || [])]
      }))
    : [emptyRecord()]
  records.value = base
}

buildRecords()

function addRecord() {
  records.value.push(emptyRecord())
}

function removeRecord(i) {
  if (records.value.length > 1) records.value.splice(i, 1)
}

async function onFileChange(i, event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  const images = files.filter(f => f.type.startsWith('image/'))
  const oversized = images.filter(f => f.size > MAX_SIZE)
  if (oversized.length) show(`有 ${oversized.length} 张图片超过 10MB，已跳过`, 'warning')
  const valid = images.filter(f => f.size <= MAX_SIZE)
  if (!valid.length) return

  uploading.value = true
  try {
    for (const file of valid) {
      const url = await uploadApi.uploadImage(file)
      records.value[i].photos.push(url)
    }
  } catch (err) {
    show(err.message || '图片上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

function removePhoto(i, pIdx) {
  records.value[i].photos.splice(pIdx, 1)
}

function handleCancel() {
  emit('cancel')
}

function handleConfirm() {
  if (isSelecting.value) return
  emit('confirm', {
    city: selectedCity.value,
    existing: currentExisting.value,
    color: color.value,
    records: JSON.parse(JSON.stringify(records.value))
  })
}
</script>

<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="handleCancel">
      <div class="dialog glass-panel">
        <div class="dialog-header">
          <span class="dialog-emoji">📍</span>
          <div class="dialog-title-wrap">
            <h2 class="dialog-title">{{ isSelecting ? '点亮城市' : selectedCity.name }}</h2>
            <span class="dialog-sub">{{ isSelecting ? '选择一个城市记录你的足迹' : selectedCity.province }}</span>
          </div>
          <span class="dialog-mode" :class="isEdit ? 'mode-edit' : 'mode-add'">
            {{ isEdit ? '修改足迹' : (isSelecting ? '选择城市' : '点亮城市') }}
          </span>
        </div>

        <div class="dialog-body">
          <!-- 选择城市模式：模糊搜索 -->
          <template v-if="isSelecting">
            <div class="search-box">
              <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
              <input v-model="searchQuery" class="text-input search-input" placeholder="输入城市名搜索，如「成都」「大理」" />
            </div>
            <div class="city-list">
              <button v-for="c in filteredCities" :key="c.adcode || c.name" class="city-option" @click="pickCity(c)">
                <span class="city-name">{{ c.name }}</span>
                <span class="city-prov">{{ c.province }}</span>
              </button>
            </div>
            <div v-if="!filteredCities.length" class="search-empty">未找到相关城市</div>
          </template>

          <!-- 表单模式：主题色 / 星级 / 时间 / 照片 -->
          <template v-else>
            <div class="field color-field">
              <label class="field-label">主题色</label>
              <div class="color-palette">
                <button
                  v-for="c in CITY_COLORS"
                  :key="c"
                  class="color-swatch"
                  :class="{ on: c === color }"
                  :style="{ background: c }"
                  :title="c"
                  @click="color = c"
                ></button>
              </div>
              <p class="field-hint">用于地图上的城市标记与整市涂色</p>
            </div>

            <div v-for="(rec, i) in records" :key="i" class="record-card">
              <div class="record-head">
                <span class="record-title">第 {{ i + 1 }} 次游玩</span>
                <button v-if="records.length > 1" class="remove-record" @click="removeRecord(i)">
                  删除本次
                </button>
              </div>

              <div class="field">
                <label class="field-label">推荐指数</label>
                <div class="stars">
                  <span
                    v-for="n in 5"
                    :key="n"
                    class="star"
                    :class="{ on: n <= rec.rating }"
                    @click="rec.rating = n"
                  >★</span>
                  <span class="rating-val">{{ rec.rating }}.0</span>
                </div>
              </div>

              <div class="field">
                <label class="field-label">游玩时间</label>
                <input v-model="rec.date" type="date" class="text-input" />
                <p class="field-hint">不填则默认按点亮日期</p>
              </div>

              <div class="field">
                <label class="field-label">照片</label>
                <div class="photos">
                  <div v-for="(p, pIdx) in rec.photos" :key="pIdx" class="photo">
                    <img :src="p" alt="" />
                    <button class="photo-del" @click="removePhoto(i, pIdx)">×</button>
                  </div>
                  <label class="photo-add">
                    <input
                      type="file"
                      accept="image/*"
                      multiple
                      style="display: none"
                      @change="onFileChange(i, $event)"
                    />
                    <span>＋</span>
                  </label>
                </div>
              </div>
            </div>

            <button class="add-more" @click="addRecord">＋ 再加一次</button>
          </template>
        </div>

        <div class="dialog-footer">
          <button class="btn-cancel" @click="handleCancel">
            {{ isSelecting ? '取消' : (isEdit ? '取消修改' : '取消点亮') }}
          </button>
          <button v-if="!isSelecting" class="btn-confirm" :disabled="uploading" @click="handleConfirm">
            {{ uploading ? '上传中…' : (isEdit ? '确定修改' : '确定点亮') }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.field-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.color-palette {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.color-swatch {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  padding: 0;
  transition: var(--transition);
}
.color-swatch:hover {
  transform: scale(1.12);
}
.color-swatch.on {
  border-color: #fff;
  box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.6);
}

.record-card {
  background: var(--dialog-item-bg);
  border: 1px solid var(--dialog-divider);
  border-radius: var(--radius-sm);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.record-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.record-title {
  font-size: 14px;
  font-weight: 700;
  color: #c7d2fe;
}

.remove-record {
  font-size: 12px;
  color: var(--text-muted);
  background: none;
  padding: 4px 8px;
  border-radius: var(--radius-xs);
}
.remove-record:hover {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.12);
}

.stars {
  display: flex;
  align-items: center;
  gap: 4px;
}

.star {
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
  /* 未点亮的星星用主题色 + 降透明度：原来写死 rgba(var(--overlay-rgb), 0.18)，
     浅色主题下五颗空星整个消失，只剩右边那个黄色的 0.0 */
  color: var(--text-muted);
  opacity: 0.55;
  transition: var(--transition);
  user-select: none;
}
.star:hover {
  transform: scale(1.15);
}
.star.on {
  color: #fbbf24;
  text-shadow: 0 0 12px rgba(251, 191, 36, 0.45);
}

.rating-val {
  font-size: 14px;
  font-weight: 700;
  color: #fbbf24;
  margin-left: 6px;
}

.add-more {
  align-self: center;
  background: rgba(56, 189, 248, 0.15);
  border: 1px solid rgba(56, 189, 248, 0.4);
  color: #a5b4fc;
  font-size: 14px;
  font-weight: 600;
  padding: 10px 26px;
  border-radius: var(--radius-pill);
  transition: var(--transition);
}
.add-more:hover {
  background: rgba(56, 189, 248, 0.28);
}

/* 选择城市模式 */
.search-box {
  position: relative;
}

.search-icon {
  position: absolute;
  left: 13px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding-left: 38px;
}

.city-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  max-height: 340px;
}

.city-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-xs);
  background: var(--dialog-item-bg);
  border: 1px solid var(--dialog-divider);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  transition: var(--transition);
}
.city-option:hover {
  background: rgba(56, 189, 248, 0.18);
  border-color: rgba(56, 189, 248, 0.5);
}

.city-prov {
  font-size: 12px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.search-empty {
  text-align: center;
  color: var(--text-muted);
  padding: 26px 0;
  font-size: 13px;
}
</style>
