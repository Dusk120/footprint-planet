import { ref } from 'vue'

const toasts = ref([])
let seed = 0

export function useToast() {
  function show(message, type = 'info', duration = 2500) {
    const id = ++seed
    toasts.value.push({ id, message, type })
    setTimeout(() => {
      toasts.value = toasts.value.filter(t => t.id !== id)
    }, duration)
  }

  return { toasts, show }
}
