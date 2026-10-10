<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import LaunchScreen from './components/LaunchScreen.vue'
import ToastContainer from './components/ToastContainer.vue'

const router = useRouter()
const LAUNCHED_KEY = 'footprint-planet:launched'
// 启动屏只在每次会话首次打开出现；同会话刷新直接留在当前页
const launched = ref(sessionStorage.getItem(LAUNCHED_KEY) === '1')

function handleEnter() {
  sessionStorage.setItem(LAUNCHED_KEY, '1')
  launched.value = true
}
</script>

<template>
  <LaunchScreen v-if="!launched" @enter="handleEnter" />
  <router-view v-else v-slot="{ Component }">
    <Transition name="page" mode="out-in">
      <component :is="Component" />
    </Transition>
  </router-view>
  <ToastContainer />
</template>

<style>
#app {
  width: 100%;
  height: 100%;
}
</style>
