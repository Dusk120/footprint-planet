<script setup>
import Sidebar from './Sidebar.vue'
</script>

<template>
  <div class="app-layout">
    <Sidebar />
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </router-view>
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.app-main {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--bg-deep);
}
</style>
