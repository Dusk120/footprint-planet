<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { planApi } from '../api'
import { useToast } from '../composables/useToast'

const route = useRoute()
const router = useRouter()
const { show: toast } = useToast()

const status = ref('loading')   // loading | error
const message = ref('')

onMounted(async () => {
  const token = route.params.token
  if (!token) {
    status.value = 'error'
    message.value = '邀请链接无效'
    return
  }
  try {
    const res = await planApi.acceptInvite(token)
    toast('已加入计划协作', 'success')
    router.replace(`/checklist/${res.planId}`)
  } catch (e) {
    status.value = 'error'
    message.value = e.message || '邀请链接无效或已失效'
  }
})
</script>

<template>
  <div class="invite-page">
    <div class="invite-card glass-panel">
      <template v-if="status === 'loading'">
        <div class="spinner"></div>
        <p class="invite-text">正在接受邀请…</p>
      </template>
      <template v-else>
        <div class="invite-icon">😕</div>
        <p class="invite-text">{{ message }}</p>
        <button class="btn-confirm" @click="router.push('/checklist')">回到我的规划</button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.invite-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.invite-card {
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  min-width: 320px;
}
.invite-icon { font-size: 48px; }
.invite-text { font-size: 15px; color: var(--text-secondary); }

.spinner {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 3px solid rgba(var(--accent-rgb), 0.2);
  border-top-color: var(--accent-primary);
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
