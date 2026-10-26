<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAppStore } from '../stores/app'
import { authApi } from '../api/auth'

const router = useRouter()
const route = useRoute()
const store = useAppStore()

const isLogin = ref(true)
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const errorMsg = ref('')
const successMsg = ref('')
const captchaUuid = ref('')
const captchaCode = ref('')
const captchaImg = ref('')

const submitting = ref(false)

async function loadCaptcha() {
  try {
    const { uuid, img } = await authApi.getCaptcha()
    captchaUuid.value = uuid
    captchaImg.value = img
    captchaCode.value = ''
  } catch (e) {
    captchaImg.value = ''
    console.warn('验证码加载失败：', e && e.message)
  }
}

async function handleSubmit() {
  errorMsg.value = ''
  successMsg.value = ''

  if (!username.value.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!password.value.trim()) {
    errorMsg.value = '请输入密码'
    return
  }
  if (password.value.length < 6) {
    errorMsg.value = '密码至少6位'
    return
  }

  if (!isLogin.value && password.value !== confirmPassword.value) {
    errorMsg.value = '两次密码不一致'
    return
  }

  if (!captchaCode.value.trim()) {
    errorMsg.value = '请输入验证码'
    return
  }

  submitting.value = true
  try {
    const payload = {
      username: username.value,
      password: password.value,
      uuid: captchaUuid.value,
      code: captchaCode.value.trim()
    }
    if (isLogin.value) {
      await store.login(payload)
    } else {
      await store.register(payload)
    }
    const redirect = route.query.redirect || '/my'
    router.push(redirect)
  } catch (e) {
    errorMsg.value = e.message || '操作失败，请重试'
  } finally {
    submitting.value = false
    loadCaptcha() // 验证码一次性，无论成败都换新图（失败时旧码已被后端作废）
  }
}

function toggleMode() {
  isLogin.value = !isLogin.value
  errorMsg.value = ''
  successMsg.value = ''
}

onMounted(loadCaptcha)
</script>

<template>
  <div class="login-page">
    <!-- 地球装饰 -->
    <div class="bg-globe"></div>

    <div class="login-card glass-panel">
      <h1 class="login-title">
        <span class="logo-icon">🌍</span>
        <span class="gradient-text">足迹星球</span>
      </h1>

      <p class="login-subtitle">{{ isLogin ? '欢迎回来' : '加入我们' }}</p>

      <form class="login-form" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label>用户名</label>
          <input
            v-model="username"
            type="text"
            placeholder="请输入用户名"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label>密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            class="form-input"
          />
        </div>

        <div v-if="!isLogin" class="form-group">
          <label>确认密码</label>
          <input
            v-model="confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label>验证码</label>
          <div class="captcha-row">
            <input
              v-model="captchaCode"
              type="text"
              maxlength="6"
              autocomplete="off"
              placeholder="请输入图中字符"
              class="form-input captcha-input"
            />
            <img
              v-if="captchaImg"
              :src="captchaImg"
              alt="验证码"
              title="看不清？点击刷新"
              class="captcha-img"
              @click="loadCaptcha"
            />
            <span v-else class="captcha-refresh" @click="loadCaptcha">加载验证码</span>
          </div>
        </div>

        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
        <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>

        <button type="submit" class="submit-btn gradient-btn" :disabled="submitting">
          {{ submitting ? '请稍候…' : isLogin ? '登录' : '注册' }}
        </button>
      </form>

      <div class="toggle-mode">
        <span>{{ isLogin ? '还没有账号？' : '已有账号？' }}</span>
        <button class="toggle-btn" @click="toggleMode">
          {{ isLogin ? '立即注册' : '返回登录' }}
        </button>
      </div>

      <div class="divider">
        <span>或者</span>
      </div>

      <button class="guest-btn" @click="router.push('/community')">
        先逛逛看看
      </button>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-deep);
  overflow: hidden;
}

.bg-globe {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(var(--accent-rgb), 0.08), rgba(14, 165, 233, 0.04), transparent 70%);
  z-index: 0;
  pointer-events: none;
}

/* 登录卡片 */
.login-card {
  position: relative;
  z-index: 10;
  width: 400px;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.login-title {
  font-size: 28px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.logo-icon {
  font-size: 28px;
}

.login-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 24px;
}

/* 表单 */
.login-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(var(--overlay-rgb), 0.1);
  border-radius: var(--radius-sm);
  background: rgba(var(--overlay-rgb), 0.04);
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

/* 验证码 */
.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.captcha-input {
  flex: 1;
  min-width: 0;
}

.captcha-img {
  width: 130px;
  height: 44px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(var(--overlay-rgb), 0.1);
  cursor: pointer;
  flex-shrink: 0;
  display: block;
}

.captcha-img:hover {
  border-color: var(--accent-primary);
}

.captcha-refresh {
  width: 130px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: rgba(var(--overlay-rgb), 0.04);
  border: 1px solid rgba(var(--overlay-rgb), 0.1);
  color: var(--accent-primary);
  font-size: 13px;
  cursor: pointer;
  flex-shrink: 0;
}

.error-msg {
  font-size: 13px;
  color: var(--danger);
  text-align: center;
}

.success-msg {
  font-size: 13px;
  color: var(--success);
  text-align: center;
}

.submit-btn {
  width: 100%;
  padding: 13px;
  margin-top: 8px;
  font-size: 15px;
}

/* 切换登录/注册 */
.toggle-mode {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-muted);
}

.toggle-btn {
  background: none;
  color: var(--accent-primary);
  font-size: 13px;
  font-weight: 600;
  transition: var(--transition);
}
.toggle-btn:hover {
  color: var(--accent-secondary);
}

/* 分割线 */
.divider {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 16px 0 8px;
  color: var(--text-muted);
  font-size: 12px;
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(var(--overlay-rgb), 0.08);
}

.guest-btn {
  width: 100%;
  padding: 12px;
  border-radius: var(--radius-pill);
  background: rgba(var(--overlay-rgb), 0.06);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  border: 1px solid rgba(var(--overlay-rgb), 0.08);
  transition: var(--transition);
}
.guest-btn:hover {
  background: rgba(var(--overlay-rgb), 0.10);
  color: var(--text-primary);
}
</style>
