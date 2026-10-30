<script setup>
import { ref, computed } from 'vue'
import { uploadApi } from '../api'
import { useToast } from '../composables/useToast'

const emit = defineEmits(['confirm', 'cancel'])
const { show: toast } = useToast()

const props = defineProps({
  editData: { type: Object, default: null }
})

const name = ref(props.editData?.name || '')
const destination = ref(props.editData?.destination || '')
const location = ref(props.editData?.location || '')
const startDate = ref(props.editData?.startDate || '')
const image = ref(props.editData?.image || '')
// 是否公开：新建默认公开（后端缺省也是 true）；编辑回显原状态
const isPublic = ref(props.editData?.isPublic ?? true)

const isValid = computed(() => name.value.trim() && destination.value.trim())

// ---------- 封面上传 ----------
const coverInput = ref(null)
const uploading = ref(false)
const MAX_COVER_SIZE = 10 * 1024 * 1024

function pickCover() {
  if (uploading.value) return
  coverInput.value?.click()
}

async function onCoverChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file || uploading.value) return
  if (!file.type.startsWith('image/')) {
    toast('请选择图片文件', 'error')
    return
  }
  if (file.size > MAX_COVER_SIZE) {
    toast('封面不能超过 10MB', 'error')
    return
  }
  uploading.value = true
  try {
    const url = await uploadApi.uploadImage(file)
    image.value = url
  } catch (err) {
    toast(err.message || '封面上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

function handleConfirm() {
  if (!isValid.value) return
  emit('confirm', {
    name: name.value.trim(),
    destination: destination.value.trim(),
    location: location.value.trim(),
    startDate: startDate.value,
    isPublic: isPublic.value,
    image: image.value
  })
}
</script>

<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="emit('cancel')">
      <div class="dialog glass-panel">
        <div class="dialog-header">
          <span class="dialog-emoji">🗺️</span>
          <div class="dialog-title-wrap">
            <h2 class="dialog-title">{{ editData ? '编辑规划' : '新建旅游规划' }}</h2>
            <span class="dialog-sub">先填好基本信息，再去地图上排行程</span>
          </div>
        </div>

        <div class="dialog-body">
          <div class="field">
            <label class="field-label">规划名称 <span class="req">*</span></label>
            <input v-model="name" class="text-input" placeholder="如「敦煌三日游」" maxlength="30" />
          </div>

          <div class="field-row">
            <div class="field flex-1">
              <label class="field-label">目的地 <span class="req">*</span></label>
              <input v-model="destination" class="text-input" placeholder="如「敦煌莫高窟」" />
            </div>
            <div class="field flex-1">
              <label class="field-label">所在省份/地区</label>
              <input v-model="location" class="text-input" placeholder="如「甘肃省」" />
            </div>
          </div>

          <div class="field">
            <label class="field-label">预计出发日期</label>
            <input v-model="startDate" type="date" class="text-input" />
          </div>

          <div class="field">
            <label class="field-label">封面</label>
            <div class="cover-row">
              <div class="cover-thumb">
                <img v-if="image" :src="image" alt="封面预览" />
                <span v-else class="cover-placeholder">🖼️</span>
              </div>
              <button type="button" class="cover-upload-btn" :disabled="uploading" @click="pickCover">
                {{ uploading ? '上传中…' : (image ? '更换封面' : '上传封面') }}
              </button>
              <input ref="coverInput" type="file" accept="image/*" hidden @change="onCoverChange" />
            </div>
          </div>

          <div class="field public-field">
            <div class="public-row">
              <div class="public-label">
                <span class="field-label">公开分享</span>
                <span class="field-hint">开启后，其他用户可在社区看到并复制这份规划</span>
              </div>
              <button
                type="button"
                class="switch"
                :class="{ on: isPublic }"
                :aria-pressed="isPublic"
                @click="isPublic = !isPublic"
              >
                <span class="switch-dot"></span>
              </button>
            </div>
          </div>
        </div>

        <div class="dialog-footer">
          <button class="btn-cancel" @click="emit('cancel')">取消</button>
          <button class="btn-confirm" :disabled="!isValid" @click="handleConfirm">
            {{ editData ? '保存修改' : '创建规划' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog { width: 520px; }

.field-row { display: flex; gap: 12px; }
.flex-1 { flex: 1; }

.cover-row {
  display: flex;
  align-items: center;
  gap: 14px;
}
.cover-thumb {
  width: 96px;
  height: 64px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid rgba(var(--overlay-rgb), 0.15);
  background: rgba(var(--overlay-rgb), 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.cover-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-placeholder {
  font-size: 26px;
  opacity: 0.8;
}
.cover-upload-btn {
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  background: rgba(var(--overlay-rgb), 0.06);
  border: 1px solid var(--bg-glass-border);
  font-size: 13px;
  cursor: pointer;
  transition: var(--transition);
}
.cover-upload-btn:hover { background: rgba(var(--overlay-rgb), 0.12); }
.cover-upload-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.public-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}
.public-label {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.field-hint {
  font-size: 12px;
  color: var(--text-muted);
}
.switch {
  position: relative;
  width: 46px;
  height: 26px;
  border-radius: var(--radius-pill);
  background: rgba(var(--overlay-rgb), 0.12);
  border: none;
  cursor: pointer;
  flex-shrink: 0;
  transition: var(--transition);
}
.switch.on { background: var(--accent-gradient); }
.switch-dot {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s;
}
.switch.on .switch-dot { transform: translateX(20px); }
</style>
