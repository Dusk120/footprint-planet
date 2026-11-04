<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, required: true },
  total: { type: Number, default: 0 },
  pageSize: { type: Number, default: 10 }
})
const emit = defineEmits(['change'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur = props.page
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }
  const set = new Set([1, total, cur - 1, cur, cur + 1])
  const arr = [...set].filter(p => p >= 1 && p <= total).sort((a, b) => a - b)
  const out = []
  let prev = 0
  for (const p of arr) {
    if (p - prev > 1) out.push(null)
    out.push(p)
    prev = p
  }
  return out
})

function go(p) {
  if (p === props.page || p < 1 || p > totalPages.value) return
  emit('change', p)
}
</script>

<template>
  <div v-if="totalPages > 1" class="admin-pagination">
    <button class="page-btn" :disabled="page <= 1" aria-label="上一页" @click="go(page - 1)">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
    </button>
    <template v-for="(p, i) in pageNumbers" :key="p === null ? `gap-${i}` : p">
      <button v-if="p === null" class="page-num" disabled>…</button>
      <button v-else class="page-num" :class="{ active: p === page }" @click="go(p)">{{ p }}</button>
    </template>
    <button class="page-btn" :disabled="page >= totalPages" aria-label="下一页" @click="go(page + 1)">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
    </button>
  </div>
</template>
