<script setup>
import { ref, computed } from 'vue'
import { useAppStore } from '../stores/app'
import { uploadApi } from '../api'
import { useToast } from '../composables/useToast'

const emit = defineEmits(['confirm', 'cancel'])
const store = useAppStore()

const title = ref('')
const content = ref('')
const photos = ref([])          // OSS URL 数组，第一张作为封面
const location = ref(null)      // { name, province, adcode, lng, lat }
const uploading = ref(false)
const { show } = useToast()
const MAX_SIZE = 10 * 1024 * 1024

// 关联地点：候选只取本人已点亮的城市（后端也按此规则校验）
const locationQuery = ref('')
const locationOpen = ref(false)
const locationResults = computed(() => {
  const q = locationQuery.value.trim()
  const src = q ? store.visitedCityList.filter(c => c.name.includes(q)) : store.visitedCityList
  return src.slice(0, 60)
})

function openLocationSearch() {
  locationOpen.value = !locationOpen.value
}

function pickLocation(c) {
  location.value = { name: c.name, province: c.province, adcode: c.adcode, lng: c.lng, lat: c.lat }
  locationOpen.value = false
  locationQuery.value = ''
}

async function onFileChange(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  const images = files.filter(f => f.type.startsWith('image/'))
  const oversized = images.filter(f => f.size > MAX_SIZE)
  if (oversized.length) show(`有 ${oversized.length} 张图片超过 10MB，已跳过`, 'warning')
  const valid = images.filter(f => f.size <= MAX_SIZE)
  if (!valid.length) return

  uploading.value = true
  try {
    for (const file of valid) {
      const url = await uploadApi.uploadImage(file)
      photos.value.push(url)
    }
  } catch (err) {
    show(err.message || '图片上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

function removePhoto(i) {
  photos.value.splice(i, 1)
}

function handleCancel() {
  emit('cancel')
}

function handlePublish() {
  if (!title.value.trim()) return
  emit('confirm', {
    title: title.value.trim(),
    content: content.value.trim(),
    image: photos.value[0] || '',
    photos: [...photos.value],
    location: location.value ? { ...location.value } : null
  })
}
</script>

<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="handleCancel">
      <div class="dialog glass-panel">
        <div class="dialog-header">
          <span class="dialog-emoji">✍️</span>
          <div class="dialog-title-wrap">
            <h2 class="dialog-title">发布笔记</h2>
            <span class="dialog-sub">分享你的旅行故事</span>
          </div>
          <span class="dialog-mode mode-add">旅行社区</span>
        </div>

        <div class="dialog-body">
          <div class="field">
            <label class="field-label">笔记主题 <span class="req">*</span></label>
            <input v-model="title" class="text-input" placeholder="一句话概括你的笔记，如「大理三日慢生活」" maxlength="50" />
          </div>

          <div class="field">
            <label class="field-label">笔记内容</label>
            <textarea v-model="content" class="text-input textarea" rows="5" placeholder="记录旅行中的见闻、攻略与心情…"></textarea>
          </div>

          <div class="field">
            <label class="field-label">图片</label>
            <div class="photos">
              <div v-for="(p, i) in photos" :key="i" class="photo">
                <img :src="p" alt="" />
                <button class="photo-del" @click="removePhoto(i)">×</button>
              </div>
              <label class="photo-add">
                <input type="file" accept="image/*" multiple style="display: none" @change="onFileChange" />
                <span>＋</span>
              </label>
            </div>
          </div>

          <div class="field">
            <label class="field-label">关联地点</label>
            <div v-if="location" class="location-tag">
              <span class="loc-name">📍 {{ location.name }}</span>
              <span class="loc-prov">{{ location.province }}</span>
              <button class="loc-remove" @click="location = null; locationOpen = false">×</button>
            </div>
            <button v-else class="loc-pick" @click="openLocationSearch">
              ＋ 选择关联地点
            </button>

            <div v-if="locationOpen" class="loc-search">
              <div class="loc-search-box">
                <svg class="loc-search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
                <input v-model="locationQuery" class="text-input loc-search-input" placeholder="搜索已点亮的城市，如「成都」「大理」" />
              </div>
              <div class="loc-list">
                <button v-for="c in locationResults" :key="c.id" class="loc-option" @click="pickLocation(c)">
                  <span class="loc-opt-name">{{ c.name }}</span>
                  <span class="loc-opt-prov">{{ c.province }}</span>
                </button>
              </div>
              <div v-if="!locationResults.length" class="loc-empty">
                {{ store.visitedCityList.length ? '未找到相关城市' : '还没有点亮的城市，先去「足迹点亮」添加吧' }}
              </div>
            </div>
          </div>
        </div>

        <div class="dialog-footer">
          <button class="btn-cancel" @click="handleCancel">取消</button>
          <button class="btn-confirm" :disabled="!title.trim() || uploading" @click="handlePublish">{{ uploading ? '上传中…' : '发布' }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog { width: 520px; }

/* 关联地点 */
.location-tag {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: var(--radius-pill);
  background: rgba(var(--accent-rgb), 0.18);
  border: 1px solid rgba(var(--accent-rgb), 0.4);
  font-size: 14px;
  font-weight: 500;
}
.loc-name {
  color: var(--accent-soft);
}
.loc-prov {
  font-size: 12px;
  color: var(--text-muted);
}
.loc-remove {
  margin-left: auto;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.loc-remove:hover {
  background: rgba(239, 68, 68, 0.8);
}

.loc-pick {
  align-self: flex-start;
  padding: 9px 18px;
  border-radius: var(--radius-pill);
  background: rgba(var(--accent-rgb), 0.12);
  border: 1px dashed rgba(var(--accent-rgb), 0.45);
  color: var(--accent-soft);
  font-size: 14px;
  font-weight: 600;
  transition: var(--transition);
}
.loc-pick:hover {
  background: rgba(var(--accent-rgb), 0.24);
}

.loc-search {
  border: 1px solid var(--dialog-divider);
  border-radius: var(--radius-sm);
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--dialog-day-bg);
}

.loc-search-box {
  position: relative;
}
.loc-search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  pointer-events: none;
}
.loc-search-input {
  width: 100%;
  padding-left: 34px;
}

.loc-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 200px;
  overflow-y: auto;
}

.loc-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: var(--radius-xs);
  background: var(--dialog-item-bg);
  color: var(--text-primary);
  font-size: 14px;
  text-align: left;
  transition: var(--transition);
}
.loc-option:hover {
  background: rgba(var(--accent-rgb), 0.18);
}

.loc-opt-prov {
  font-size: 12px;
  color: var(--text-muted);
}

.loc-empty {
  text-align: center;
  color: var(--text-muted);
  padding: 16px 0;
  font-size: 13px;
}
</style>
