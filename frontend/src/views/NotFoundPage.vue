<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="nf-code gradient-text">404</div>
    <p class="nf-text">这颗星球还没有被发现</p>
    <button class="nf-btn gradient-btn" @click="router.push('/community')">
      返回社区
    </button>
  </div>
</template>

<style scoped>
.not-found {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.nf-code {
  font-size: 96px;
  font-weight: 900;
  letter-spacing: 4px;
}

.nf-text {
  font-size: 16px;
  color: var(--text-muted);
  margin-bottom: 16px;
}
</style>
