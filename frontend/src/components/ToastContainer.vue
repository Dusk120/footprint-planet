<script setup>
import { useToast } from '../composables/useToast'

const { toasts } = useToast()

const icons = {
  info: 'ℹ️',
  success: '✅',
  warning: '⚠️',
  error: '❌',
}
</script>

<template>
  <Teleport to="body">
    <div class="toast-container">
      <TransitionGroup name="toast">
        <div
          v-for="t in toasts"
          :key="t.id"
          class="toast-item glass-panel"
          :class="`toast-${t.type}`"
        >
          <span class="toast-icon">{{ icons[t.type] || icons.info }}</span>
          <span class="toast-msg">{{ t.message }}</span>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  pointer-events: none;
}

.toast-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  border-radius: var(--radius-pill);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  box-shadow: var(--shadow-card);
}

.toast-icon {
  font-size: 16px;
}

.toast-success { border-color: rgba(34, 197, 94, 0.4); }
.toast-warning { border-color: rgba(245, 158, 11, 0.4); }
.toast-error { border-color: rgba(239, 68, 68, 0.4); }

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(-16px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateY(-16px);
}
</style>
