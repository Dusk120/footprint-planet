<script setup>
import { ref } from 'vue'
import { useToast } from '../composables/useToast'
import { feedbackApi, uploadApi } from '../api'

const { show: toast } = useToast()

const activeTab = ref('功能建议')
const feedbackContent = ref('')
const contact = ref('')
const images = ref([]) // OSS URL 数组
const uploading = ref(false)
const submitted = ref(false)
const submitting = ref(false)
const MAX_SIZE = 10 * 1024 * 1024

const tabs = ['功能建议', '问题反馈', '体验吐槽', '其他']

// 选图后立刻上传到 OSS，拿到 URL 存起来；提交时只传 URL 数组
async function onFileChange(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = '' // 清空以便重复选同一个文件
  const onlyImages = files.filter(f => f.type.startsWith('image/'))
  const oversized = onlyImages.filter(f => f.size > MAX_SIZE)
  if (oversized.length) toast(`有 ${oversized.length} 张图片超过 10MB，已跳过`, 'warning')
  const valid = onlyImages.filter(f => f.size <= MAX_SIZE)
  if (!valid.length) return

  uploading.value = true
  try {
    for (const file of valid) {
      const url = await uploadApi.uploadImage(file)
      images.value.push(url)
    }
  } catch (err) {
    toast(err.message || '图片上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

function removeImage(i) {
  images.value.splice(i, 1)
}

async function submitFeedback() {
  if (!feedbackContent.value.trim()) {
    toast('请先填写反馈内容', 'warning')
    return
  }
  submitting.value = true
  try {
    await feedbackApi.submit({
      content: `【${activeTab.value}】${feedbackContent.value.trim()}`,
      contact: contact.value.trim(),
      images: [...images.value]
    })
    submitted.value = true
    toast('反馈已提交，感谢你的建议', 'success')
    setTimeout(() => {
      submitted.value = false
      feedbackContent.value = ''
      contact.value = ''
      images.value = []
    }, 2000)
  } catch (e) {
    toast(e.message || '提交失败，请重试', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="feedback-form glass-panel">
    <div class="form-header">
      <h2 class="form-title">建议与反馈</h2>
      <p class="form-subtitle">帮助我们把足迹星球做得更好</p>
    </div>

    <div class="form-tabs">
      <button
        v-for="tab in tabs"
        :key="tab"
        class="tab-btn"
        :class="{ active: activeTab === tab }"
        @click="activeTab = tab"
      >
        {{ tab }}
      </button>
    </div>

    <div class="form-body">
      <label class="form-label">反馈内容</label>
      <textarea
        v-model="feedbackContent"
        class="form-textarea glass-panel"
        placeholder="请描述你遇到的问题或想增加的功能"
        rows="4"
      ></textarea>

      <label class="form-label">联系方式（选填）</label>
      <input
        v-model="contact"
        class="form-input glass-panel"
        type="text"
        maxlength="100"
        placeholder="邮箱 / 微信 / QQ，方便我们回复你"
      />

      <div v-if="images.length" class="thumb-grid">
        <div v-for="(img, i) in images" :key="img" class="upload-thumb">
          <img :src="img" alt="反馈图片" />
          <button class="thumb-del" type="button" @click="removeImage(i)">×</button>
        </div>
      </div>

      <label class="upload-area glass-panel">
        <input
          type="file"
          accept="image/*"
          multiple
          style="display: none"
          @change="onFileChange"
        />
        <div class="upload-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke-width="1.5" style="stroke: var(--text-muted)">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
        </div>
        <p class="upload-text">{{ uploading ? '上传中…' : '上传截图或照片（可多选，单张≤10MB）' }}</p>
      </label>

      <button class="submit-btn gradient-btn" :disabled="submitting || uploading" @click="submitFeedback">
        {{ submitting ? '提交中…' : submitted ? '已提交 ✓' : '提交反馈' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.feedback-form {
  padding: 32px;
  max-width: 600px;
}

.form-header {
  margin-bottom: 24px;
}

.form-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 4px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

.form-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.tab-btn {
  padding: 8px 20px;
  border-radius: var(--radius-pill);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: var(--transition);
}
.tab-btn:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}
.tab-btn.active {
  background: var(--accent-gradient);
  color: white;
  box-shadow: 0 2px 12px rgba(var(--accent-rgb), 0.4);
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.form-textarea {
  width: 100%;
  padding: 14px;
  border: 1px solid var(--bg-glass-border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  transition: var(--transition);
}
.form-textarea:focus {
  outline: none;
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(var(--accent-rgb), 0.15);
}
.form-textarea::placeholder {
  color: var(--text-muted);
}

.form-input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--bg-glass-border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  transition: var(--transition);
}
.form-input:focus {
  outline: none;
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(var(--accent-rgb), 0.15);
}
.form-input::placeholder {
  color: var(--text-muted);
}

.thumb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(84px, 1fr));
  gap: 10px;
}

.upload-thumb {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--bg-glass-border);
}
.upload-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.thumb-del {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 14px;
  line-height: 1;
  transition: var(--transition);
}
.thumb-del:hover {
  background: rgba(248, 113, 113, 0.9);
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
  border: 2px dashed var(--border-strong);
  border-radius: var(--radius-sm);
  background: transparent;
  cursor: pointer;
  transition: var(--transition);
}
.upload-area:hover {
  border-color: var(--accent-primary);
  background: var(--bg-card-hover);
}

.upload-text {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 8px;
}

.submit-btn {
  align-self: flex-end;
  padding: 12px 36px;
  font-size: 15px;
}
</style>
