<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '操作确认' },
  message: { type: String, default: '' },
  confirmText: { type: String, default: '确认' },
  danger: { type: Boolean, default: false },
  busy: { type: Boolean, default: false }
})
const emit = defineEmits(['cancel', 'confirm'])
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="dialog-overlay" @click.self="emit('cancel')">
      <div class="dialog admin-confirm-dialog">
        <div class="dialog-header">
          <span class="dialog-emoji">{{ danger ? '⚠️' : '❓' }}</span>
          <div class="dialog-title-wrap">
            <h2 class="dialog-title">{{ title }}</h2>
            <span class="dialog-sub">该操作需要你再次确认</span>
          </div>
        </div>
        <div class="dialog-body">
          <p class="confirm-message">{{ message }}</p>
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" :disabled="busy" @click="emit('cancel')">取消</button>
          <button
            class="btn-confirm"
            :class="{ danger: danger }"
            :disabled="busy"
            @click="emit('confirm')"
          >{{ busy ? '处理中…' : confirmText }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.admin-confirm-dialog { width: 420px; }

.confirm-message {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary);
  white-space: pre-line;
  word-break: break-word;
}

.btn-confirm.danger {
  background: linear-gradient(135deg, var(--danger), var(--danger-deep));
  box-shadow: 0 4px 20px rgba(239, 68, 68, 0.4);
}
</style>
