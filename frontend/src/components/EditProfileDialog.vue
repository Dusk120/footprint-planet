<script setup>
import { ref } from 'vue'
import { useAppStore } from '../stores/app'
import { uploadApi } from '../api'
import { useToast } from '../composables/useToast'

const emit = defineEmits(['cancel'])
const store = useAppStore()
const { show: toast } = useToast()

const nickname = ref(store.user.nickname)
const motto = ref(store.user.motto)
const avatar = ref(store.user.avatar)          // 预览用
const newAvatar = ref('')                      // 本次上传成功的新头像，未上传则不提交该字段
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const errorMsg = ref('')

const saving = ref(false)
const uploadingAvatar = ref(false)
const MAX_AVATAR_SIZE = 5 * 1024 * 1024

async function onAvatarChange(e) {
  const file = (e.target.files || [])[0]
  e.target.value = ''
  if (!file || uploadingAvatar.value) return
  if (!file.type.startsWith('image/')) {
    toast('请选择图片文件', 'error')
    return
  }
  if (file.size > MAX_AVATAR_SIZE) {
    toast('头像不能超过 5MB', 'error')
    return
  }
  uploadingAvatar.value = true
  try {
    const url = await uploadApi.uploadImage(file)
    avatar.value = url
    newAvatar.value = url
  } catch (err) {
    toast(err.message || '头像上传失败', 'error')
  } finally {
    uploadingAvatar.value = false
  }
}

async function handleConfirm() {
  errorMsg.value = ''

  if (!nickname.value.trim()) {
    errorMsg.value = '昵称不能为空'
    return
  }

  if (newPassword.value) {
    if (newPassword.value.length < 6) {
      errorMsg.value = '新密码至少6位'
      return
    }
    if (newPassword.value !== confirmPassword.value) {
      errorMsg.value = '两次密码不一致'
      return
    }
  }

  const payload = {
    nickname: nickname.value.trim(),
    motto: motto.value.trim(),
    oldPassword: oldPassword.value,
    newPassword: newPassword.value
  }
  if (newAvatar.value) payload.avatar = newAvatar.value

  saving.value = true
  try {
    await store.updateProfile(payload)
    toast('资料已更新')
    emit('cancel')
  } catch (e) {
    errorMsg.value = e.message || '保存失败，请重试'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="dialog-overlay" @click.self="emit('cancel')">
      <div class="dialog glass-panel">
        <div class="dialog-header">
          <span class="dialog-emoji">👤</span>
          <div class="dialog-title-wrap">
            <h2 class="dialog-title">编辑资料</h2>
            <span class="dialog-sub">修改你的账号信息</span>
          </div>
        </div>

        <div class="dialog-body">
          <div class="field">
            <label class="field-label">头像</label>
            <div class="avatar-row">
              <img class="avatar-preview" :src="avatar" alt="头像预览" />
              <label class="avatar-upload-btn">
                <input type="file" accept="image/*" style="display: none" @change="onAvatarChange" />
                {{ uploadingAvatar ? '上传中…' : '更换头像' }}
              </label>
            </div>
          </div>

          <div class="field">
            <label class="field-label">昵称</label>
            <input v-model="nickname" class="text-input" placeholder="你的昵称" maxlength="20" />
          </div>

          <div class="field">
            <label class="field-label">个性签名</label>
            <input v-model="motto" class="text-input" placeholder="一句话介绍自己" maxlength="50" />
          </div>

          <div class="section-divider">
            <span class="divider-label">修改密码</span>
          </div>
          <p class="hint">如不需要修改密码，可留空</p>

          <div class="field">
            <label class="field-label">原密码</label>
            <input v-model="oldPassword" type="password" class="text-input" placeholder="输入当前密码" />
          </div>

          <div class="field">
            <label class="field-label">新密码</label>
            <input v-model="newPassword" type="password" class="text-input" placeholder="至少6位" />
          </div>

          <div class="field">
            <label class="field-label">确认新密码</label>
            <input v-model="confirmPassword" type="password" class="text-input" placeholder="再次输入新密码" />
          </div>

          <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
        </div>

        <div class="dialog-footer">
          <button class="btn-cancel" @click="emit('cancel')">取消</button>
          <button class="btn-confirm" @click="handleConfirm">保存修改</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog { width: 440px; }

.hint { font-size: 12px; color: var(--text-muted); margin-top: -8px; }

.avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-preview {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(var(--overlay-rgb), 0.15);
  flex-shrink: 0;
}

.avatar-upload-btn {
  padding: 8px 16px;
  border-radius: var(--radius-pill);
  background: rgba(var(--overlay-rgb), 0.06);
  border: 1px solid var(--bg-glass-border);
  font-size: 13px;
  cursor: pointer;
  transition: var(--transition);
}
.avatar-upload-btn:hover {
  background: rgba(var(--overlay-rgb), 0.12);
}

.error-msg {
  padding: 10px 14px;
  border-radius: var(--radius-xs);
  background: rgba(248, 113, 113, 0.12);
  border: 1px solid rgba(248, 113, 113, 0.3);
  color: #f87171;
  font-size: 13px;
}
</style>
