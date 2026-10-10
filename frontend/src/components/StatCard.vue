<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  unit: { type: String, default: '' }
})
</script>

<template>
  <div class="stat-card glass-panel">
    <div class="stat-label">{{ label }}</div>
    <div class="stat-value">
      <span class="stat-number gradient-text">{{ value }}</span>
      <span v-if="unit" class="stat-unit">{{ unit }}</span>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: var(--transition);
}
.stat-card:hover {
  background: var(--bg-card-hover);
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

.stat-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-number {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -1px;
}

.stat-unit {
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
