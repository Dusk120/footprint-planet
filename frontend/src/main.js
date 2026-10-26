import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAppStore } from './stores/app'
// 全局字体：IBM Plex Sans（仅 latin 子集，中文回退系统字体）
import '@fontsource/ibm-plex-sans/latin-400.css'
import '@fontsource/ibm-plex-sans/latin-500.css'
import '@fontsource/ibm-plex-sans/latin-600.css'
import '@fontsource/ibm-plex-sans/latin-700.css'
import './styles/global.css'
import './styles/admin.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)

// 应用启动时恢复登录态并拉取用户数据（异步，不阻塞渲染）
const store = useAppStore(pinia)
store.ensureInit()

// 监听 401 事件，自动跳转到登录页
window.addEventListener('unauthorized', () => {
  router.push('/login')
})

app.mount('#app')
